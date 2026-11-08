package com.example.audio.download

import android.content.Context
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.example.audio.storage.AudioStorage
import com.example.data.prefs.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AudioDownloadManager(
    private val context: Context,
    private val storage: AudioStorage,
    private val settingsRepository: SettingsRepository,
    private val scope: CoroutineScope
) {
    private val workManager = WorkManager.getInstance(context)

    private val _states = MutableStateFlow<Map<Pair<String, Int>, DownloadState>>(emptyMap())
    val downloadStates: StateFlow<Map<Pair<String, Int>, DownloadState>> = _states.asStateFlow()

    init {
        // Clean any leftover .part files on startup
        scope.launch(Dispatchers.IO) {
            storage.cleanPartFiles()
        }
    }

    fun startDownload(reciterId: String, surahId: Int) {
        scope.launch {
            val settings = settingsRepository.settingsFlow.first()
            val requiredNetwork = if (settings.audioWifiOnly) {
                NetworkType.UNMETERED
            } else {
                NetworkType.CONNECTED
            }

            val constraints = Constraints.Builder()
                .setRequiredNetworkType(requiredNetwork)
                .setRequiresStorageNotLow(true)
                .build()

            val inputData = Data.Builder()
                .putString(DownloadWorker.KEY_RECITER_ID, reciterId)
                .putInt(DownloadWorker.KEY_SURAH_ID, surahId)
                .build()

            val request = OneTimeWorkRequestBuilder<DownloadWorker>()
                .setConstraints(constraints)
                .setInputData(inputData)
                .addTag("audio_download")
                .addTag("surah_$surahId")
                .addTag("reciter_$reciterId")
                .build()

            try {
                val workName = DownloadWorker.uniqueWorkName(reciterId, surahId)
                workManager.enqueueUniqueWork(workName, ExistingWorkPolicy.REPLACE, request)

                updateState(reciterId, surahId, DownloadState.Queued)
                observeWork(reciterId, surahId, workName)
            } catch (e: Throwable) {
                android.util.Log.e("AudioDownloadManager", "Failed to enqueue download work", e)
                updateState(reciterId, surahId, DownloadState.Failed(e.localizedMessage ?: "Failed to start download"))
            }
        }
    }

    fun cancelDownload(reciterId: String, surahId: Int) {
        val workName = DownloadWorker.uniqueWorkName(reciterId, surahId)
        workManager.cancelUniqueWork(workName)
        updateState(reciterId, surahId, DownloadState.Idle)
    }

    fun deleteDownload(reciterId: String, surahId: Int, ayahIds: List<Int>) {
        cancelDownload(reciterId, surahId)
        storage.deleteSurah(reciterId, surahId, ayahIds)
        updateState(reciterId, surahId, DownloadState.Idle)
    }

    fun observeSurahState(reciterId: String, surahId: Int, ayahIds: List<Int>, needsBismillah: Boolean) {
        val key = Pair(reciterId, surahId)
        val current = _states.value[key]
        if (current == null || current is DownloadState.Idle) {
            if (storage.isSurahDownloaded(reciterId, surahId, ayahIds, needsBismillah)) {
                updateState(reciterId, surahId, DownloadState.Done)
            } else {
                val workName = DownloadWorker.uniqueWorkName(reciterId, surahId)
                observeWork(reciterId, surahId, workName)
            }
        }
    }

    private fun observeWork(reciterId: String, surahId: Int, workName: String) {
        scope.launch {
            workManager.getWorkInfosForUniqueWorkFlow(workName).collect { workInfos ->
                val info = workInfos.firstOrNull() ?: return@collect
                when (info.state) {
                    WorkInfo.State.ENQUEUED -> {
                        updateState(reciterId, surahId, DownloadState.Queued)
                    }
                    WorkInfo.State.RUNNING -> {
                        val done = info.progress.getInt(DownloadWorker.KEY_DONE, 0)
                        val total = info.progress.getInt(DownloadWorker.KEY_TOTAL, 0)
                        val bytes = info.progress.getLong(DownloadWorker.KEY_BYTES, 0L)
                        updateState(reciterId, surahId, DownloadState.Running(done, total, bytes))
                    }
                    WorkInfo.State.SUCCEEDED -> {
                        updateState(reciterId, surahId, DownloadState.Done)
                    }
                    WorkInfo.State.FAILED -> {
                        val reason = info.outputData.getString(DownloadWorker.KEY_ERROR)
                            ?: "Download failed"
                        updateState(reciterId, surahId, DownloadState.Failed(reason))
                    }
                    WorkInfo.State.CANCELLED -> {
                        updateState(reciterId, surahId, DownloadState.Idle)
                    }
                    WorkInfo.State.BLOCKED -> {
                        updateState(reciterId, surahId, DownloadState.Queued)
                    }
                }
            }
        }
    }

    private fun updateState(reciterId: String, surahId: Int, state: DownloadState) {
        val current = _states.value.toMutableMap()
        current[Pair(reciterId, surahId)] = state
        _states.value = current
    }
}
