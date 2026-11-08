package com.example.ui.translations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.QuranRepository
import com.example.data.db.TranslationInfoEntity
import com.example.data.prefs.SettingsRepository
import com.example.data.translations.CatalogState
import com.example.data.translations.DownloadStatus
import com.example.data.translations.RemoteTranslation
import com.example.data.translations.TranslationDownloadManager
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class TranslationsViewModel(
    private val manager: TranslationDownloadManager,
    quranRepository: QuranRepository,
    settingsRepository: SettingsRepository
) : ViewModel() {

    val catalog: StateFlow<CatalogState> = manager.catalog
    val status: StateFlow<Map<String, DownloadStatus>> = manager.status
    val completed: SharedFlow<String> = manager.completed

    val installed: StateFlow<List<TranslationInfoEntity>> = quranRepository.translations()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val selectedId: StateFlow<String> = settingsRepository.settingsFlow
        .map { it.translationId }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    private val settings = settingsRepository

    init {
        manager.loadCatalog()
    }

    fun reload() = manager.loadCatalog(force = true)
    fun download(item: RemoteTranslation) = manager.download(item)
    fun cancel(id: String) = manager.cancel(id)

    /** Saves the chosen translation. The caller restarts the app afterwards. */
    suspend fun apply(id: String) {
        settings.setTranslationId(id)
    }

    class Factory(
        private val manager: TranslationDownloadManager,
        private val quranRepository: QuranRepository,
        private val settingsRepository: SettingsRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return TranslationsViewModel(manager, quranRepository, settingsRepository) as T
        }
    }
}
