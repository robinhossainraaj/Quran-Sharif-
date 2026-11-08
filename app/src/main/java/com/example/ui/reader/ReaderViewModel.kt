package com.example.ui.reader

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.audio.catalog.Reciter
import com.example.audio.catalog.ReciterCatalogLoader
import com.example.audio.download.AudioDownloadManager
import com.example.audio.download.DownloadState
import com.example.audio.player.AudioRepeatMode
import com.example.audio.player.PlaybackController
import com.example.audio.player.PlaybackUiState
import com.example.audio.preview.AudioPreviewController
import com.example.audio.storage.AudioStorage
import com.example.data.QuranRepository
import com.example.data.db.AyahWithText
import com.example.data.db.SurahEntity
import com.example.data.db.TranslationInfoEntity
import com.example.data.prefs.SettingsRepository
import com.example.data.prefs.UserSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ReaderUiState(
    val surah: SurahEntity? = null,
    val ayahs: List<AyahWithText> = emptyList(),
    val bismillahArabic: String = "",
    val bismillahBangla: String = "",
    val translationInfo: TranslationInfoEntity? = null,
    val bookmarkedAyahIds: Set<Int> = emptySet(),
    val userSettings: UserSettings = UserSettings(),
    val isLoading: Boolean = true
)

@OptIn(ExperimentalCoroutinesApi::class)
class ReaderViewModel(
    val surahId: Int,
    private val quranRepository: QuranRepository,
    private val settingsRepository: SettingsRepository,
    private val audioStorage: AudioStorage,
    private val audioDownloadManager: AudioDownloadManager,
    private val playbackController: PlaybackController,
    val audioPreviewController: AudioPreviewController
) : ViewModel() {

    val playbackState: StateFlow<PlaybackUiState> = playbackController.state
    val downloadStates: StateFlow<Map<Pair<String, Int>, DownloadState>> = audioDownloadManager.downloadStates

    private val _bismillahData = MutableStateFlow(Pair("", ""))

    private val translationIdFlow = settingsRepository.settingsFlow
        .map { it.translationId }
        .distinctUntilChanged()

    private val ayahsFlow = translationIdFlow.flatMapLatest { transId ->
        quranRepository.ayahsOfSurahWithText(surahId, transId)
    }

    private val translationInfoFlow = translationIdFlow.flatMapLatest { transId ->
        quranRepository.translationInfoFlow(transId)
    }

    private data class FourTuple<A, B, C, D>(val surah: A, val ayahs: B, val bookmarks: C, val settings: D)

    val uiState: StateFlow<ReaderUiState> = combine(
        combine(
            quranRepository.surahFlow(surahId),
            ayahsFlow,
            quranRepository.bookmarks(),
            settingsRepository.settingsFlow
        ) { surah, ayahs, bookmarks, settings ->
            FourTuple(surah, ayahs, bookmarks.map { it.ayahId }.toSet(), settings)
        },
        combine(_bismillahData, translationInfoFlow) { bismillah, transInfo ->
            Pair(bismillah, transInfo)
        }
    ) { four, (bismillah, transInfo) ->
        ReaderUiState(
            surah = four.surah,
            ayahs = four.ayahs,
            bismillahArabic = bismillah.first,
            bismillahBangla = bismillah.second,
            translationInfo = transInfo,
            bookmarkedAyahIds = four.bookmarks,
            userSettings = four.settings,
            isLoading = four.ayahs.isEmpty()
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ReaderUiState()
    )

    init {
        viewModelScope.launch {
            translationIdFlow.collect { transId ->
                val ayah1 = quranRepository.ayahWithTextBySurahAndNo(1, 1, transId)
                if (ayah1 != null) {
                    _bismillahData.value = Pair(ayah1.arabicOriginal, ayah1.bangla)
                }
            }
        }

        // Observe download state for current surah
        viewModelScope.launch {
            val settings = settingsRepository.settingsFlow.first()
            val reciterId = settings.audioReciterId
            val ayahs = quranRepository.ayahsOfSurah(surahId).first()
            audioDownloadManager.observeSurahState(
                reciterId = reciterId,
                surahId = surahId,
                ayahIds = ayahs.map { it.id },
                needsBismillah = settings.audioBismillahPrefix && (surahId != 1 && surahId != 9)
            )
        }
    }

    fun toggleBookmark(ayahId: Int) {
        viewModelScope.launch {
            quranRepository.toggleBookmark(ayahId)
        }
    }

    fun bookmarkSurahStart() {
        val firstAyah = uiState.value.ayahs.firstOrNull() ?: return
        viewModelScope.launch {
            quranRepository.addBookmark(firstAyah.id)
        }
    }

    fun saveLastRead(ayahNo: Int) {
        if (ayahNo > 0) {
            viewModelScope.launch {
                settingsRepository.setLastRead(surahId, ayahNo)
            }
        }
    }

    // Audio methods
    suspend fun playSurah(startAyahNo: Int = 1, reciterId: String? = null): Boolean {
        return playbackController.playSurah(surahId, startAyahNo, reciterId)
    }

    fun playOrPause() {
        playbackController.playOrPause()
    }

    fun pauseAudio() {
        playbackController.pause()
    }

    fun stopAudio() {
        playbackController.stop()
    }

    fun skipToNext() {
        playbackController.skipToNext()
    }

    fun skipToPrevious() {
        playbackController.skipToPrevious()
    }

    fun seekToAyah(ayahNo: Int) {
        playbackController.seekToAyah(ayahNo)
    }

    fun setPlaybackSpeed(speed: Float) {
        playbackController.setSpeed(speed)
    }

    fun setRepeatMode(mode: AudioRepeatMode) {
        playbackController.setRepeatMode(mode)
    }

    fun startDownload(reciterId: String) {
        audioDownloadManager.startDownload(reciterId, surahId)
    }

    fun cancelDownload(reciterId: String) {
        audioDownloadManager.cancelDownload(reciterId, surahId)
    }

    fun deleteAudio(reciterId: String) {
        val ayahs = uiState.value.ayahs
        audioDownloadManager.deleteDownload(reciterId, surahId, ayahs.map { it.id })
    }

    fun isSurahDownloaded(reciterId: String): Boolean {
        val ayahs = uiState.value.ayahs
        if (ayahs.isEmpty()) return false
        val settings = uiState.value.userSettings
        val needsBismillah = settings.audioBismillahPrefix && (surahId != 1 && surahId != 9)
        return audioStorage.isSurahDownloaded(
            reciterId = reciterId,
            surahId = surahId,
            ayahIds = ayahs.map { it.id },
            needsBismillah = needsBismillah
        )
    }

    fun getSurahDiskSizeBytes(reciterId: String): Long {
        val ayahs = uiState.value.ayahs
        return audioStorage.surahSizeBytes(reciterId, ayahs.map { it.id })
    }

    fun setAudioReciterId(reciterId: String) {
        viewModelScope.launch {
            settingsRepository.setAudioReciterId(reciterId)
        }
    }

    fun setAudioFollow(follow: Boolean) {
        viewModelScope.launch {
            settingsRepository.setAudioFollow(follow)
        }
    }

    fun setMobileDataWarningShown() {
        viewModelScope.launch {
            settingsRepository.setMobileDataWarningShown(true)
        }
    }

    class Factory(
        private val surahId: Int,
        private val quranRepository: QuranRepository,
        private val settingsRepository: SettingsRepository,
        private val audioStorage: AudioStorage,
        private val audioDownloadManager: AudioDownloadManager,
        private val playbackController: PlaybackController,
        private val audioPreviewController: AudioPreviewController
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ReaderViewModel(
                surahId,
                quranRepository,
                settingsRepository,
                audioStorage,
                audioDownloadManager,
                playbackController,
                audioPreviewController
            ) as T
        }
    }
}
