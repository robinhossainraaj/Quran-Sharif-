package com.example.data.translations

import android.util.Log
import androidx.room.withTransaction
import com.example.data.db.QuranDatabase
import com.example.data.db.TranslationInfoEntity
import com.example.data.db.TranslationTextEntity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext

sealed interface CatalogState {
    object Loading : CatalogState
    data class Loaded(val items: List<RemoteTranslation>) : CatalogState
    data class Error(val message: String) : CatalogState
}

sealed interface DownloadStatus {
    data class Running(val progress: Float) : DownloadStatus
    data class Failed(val message: String) : DownloadStatus
}

/**
 * Loads the QuranEnc catalogue and downloads translations into the local database.
 * Texts are saved in the phone's private storage (not inside the APK), so the app size stays the same.
 * Runs in the application scope, so a download keeps going when the person leaves the screen.
 */
class TranslationDownloadManager(
    private val database: QuranDatabase,
    private val scope: CoroutineScope
) {

    private val _catalog = MutableStateFlow<CatalogState>(CatalogState.Loading)
    val catalog: StateFlow<CatalogState> = _catalog.asStateFlow()

    private val _status = MutableStateFlow<Map<String, DownloadStatus>>(emptyMap())
    val status: StateFlow<Map<String, DownloadStatus>> = _status.asStateFlow()

    private val _completed = MutableSharedFlow<String>(extraBufferCapacity = 4)
    /** Emits the translation id when a download has finished and is saved. */
    val completed: SharedFlow<String> = _completed.asSharedFlow()

    private val jobs = HashMap<String, Job>()
    private var catalogJob: Job? = null

    fun loadCatalog(force: Boolean = false) {
        if (!force && _catalog.value is CatalogState.Loaded) return
        if (catalogJob?.isActive == true) return
        _catalog.value = CatalogState.Loading
        catalogJob = scope.launch(Dispatchers.IO) {
            try {
                val items = QuranEncApi.fetchCatalog()
                _catalog.value = if (items.isEmpty()) {
                    CatalogState.Error("Empty list")
                } else {
                    CatalogState.Loaded(items)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("TranslationDownload", "Catalogue failed", e)
                _catalog.value = CatalogState.Error(e.message ?: e.javaClass.simpleName)
            }
        }
    }

    fun download(item: RemoteTranslation) {
        val id = item.id
        if (jobs[id]?.isActive == true) return
        setStatus(id, DownloadStatus.Running(0f))
        jobs[id] = scope.launch(Dispatchers.IO) {
            try {
                fetchAndSave(item)
                _status.update { it - id }
                _completed.emit(id)
            } catch (e: CancellationException) {
                _status.update { it - id }
                throw e
            } catch (e: Exception) {
                Log.e("TranslationDownload", "Download failed for ${item.key}", e)
                setStatus(id, DownloadStatus.Failed(e.message ?: e.javaClass.simpleName))
            }
        }
    }

    fun cancel(id: String) {
        jobs[id]?.cancel()
        _status.update { it - id }
    }

    private fun setStatus(id: String, s: DownloadStatus) {
        _status.update { it + (id to s) }
    }

    private suspend fun fetchAndSave(item: RemoteTranslation) {
        val dao = database.quranDao()
        val idByKey = HashMap<Int, Int>(6400)
        for (k in dao.ayahKeys()) idByKey[k.surahId * 1000 + k.ayahNo] = k.id
        val expected = idByKey.size
        if (expected == 0) throw IllegalStateException("Quran data is not ready")

        val perSura = arrayOfNulls<List<SuraAyah>>(115)
        val doneCount = java.util.concurrent.atomic.AtomicInteger(0)
        val gate = Semaphore(4)

        coroutineScope {
            (1..114).map { sura ->
                async(Dispatchers.IO) {
                    gate.withPermit {
                        perSura[sura] = fetchWithRetry(item.key, sura)
                    }
                    val n = doneCount.incrementAndGet()
                    setStatus(item.id, DownloadStatus.Running(n / 114f))
                }
            }.awaitAll()
        }

        val rows = ArrayList<TranslationTextEntity>(expected)
        for (sura in 1..114) {
            for (a in perSura[sura].orEmpty()) {
                val ayahId = idByKey[sura * 1000 + a.aya] ?: continue
                if (a.text.isBlank()) continue
                rows.add(TranslationTextEntity(item.id, ayahId, a.text))
            }
        }
        if (rows.size < expected) {
            throw IllegalStateException("Incomplete translation: ${rows.size} of $expected ayahs")
        }

        val info = TranslationInfoEntity(
            id = item.id,
            nameEn = item.title,
            nameBn = item.title,
            translator = item.description.ifBlank { item.title },
            source = "${QuranEncApi.SOURCE_NAME} (${QuranEncApi.SOURCE_URL})",
            version = item.version,
            licenseNote = "Source: ${QuranEncApi.SOURCE_NAME}. ${item.description}".trim(),
            licenseText = "Downloaded from ${QuranEncApi.SOURCE_NAME} (translation key: ${item.key}). " +
                "The translation belongs to its translator and publisher.",
            ayahCount = rows.size,
            isDefault = false
        )
        withContext(Dispatchers.IO) {
            database.withTransaction {
                dao.clearTranslationTexts(item.id)
                dao.insertTranslationTexts(rows)
                dao.insertTranslationInfo(info)
            }
        }
    }

    private suspend fun fetchWithRetry(key: String, sura: Int): List<SuraAyah> {
        var last: Exception? = null
        repeat(3) { attempt ->
            try {
                val list = QuranEncApi.fetchSura(key, sura)
                if (list.isNotEmpty()) return list
                last = IllegalStateException("Surah $sura is empty")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                last = e
            }
            kotlinx.coroutines.delay(600L * (attempt + 1))
        }
        throw last ?: IllegalStateException("Surah $sura failed")
    }
}
