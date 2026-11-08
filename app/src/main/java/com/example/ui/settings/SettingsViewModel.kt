package com.example.ui.settings

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.QuranRepository
import com.example.data.db.AyahWithText
import com.example.data.db.TranslationInfoEntity
import com.example.data.prefs.SettingsRepository
import com.example.data.prefs.UserSettings
import com.example.widget.WidgetUpdater
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val userSettings: UserSettings = UserSettings(),
    val previewAyah: AyahWithText? = null,
    val availableTranslations: List<TranslationInfoEntity> = emptyList(),
    val translationPreviews: Map<String, String> = emptyMap()
)

class SettingsViewModel(
    private val context: Context,
    private val quranRepository: QuranRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _previewAyah = MutableStateFlow<AyahWithText?>(null)
    private val _translationPreviews = MutableStateFlow<Map<String, String>>(emptyMap())

    val uiState: StateFlow<SettingsUiState> = combine(
        settingsRepository.settingsFlow,
        _previewAyah,
        quranRepository.translations(),
        _translationPreviews
    ) { settings, preview, translations, previews ->
        SettingsUiState(
            userSettings = settings,
            previewAyah = preview,
            availableTranslations = translations,
            translationPreviews = previews
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState()
    )

    init {
        observeTranslationChanges()
    }

    private fun observeTranslationChanges() {
        viewModelScope.launch {
            settingsRepository.settingsFlow
                .map { it.translationId }
                .distinctUntilChanged()
                .collect { transId ->
                    // Load Surah 112 Ayah 1 (Al-Ikhlas) with selected translation
                    val ayah = quranRepository.ayahWithTextBySurahAndNo(112, 1, transId)
                    _previewAyah.value = ayah
                }
        }

        viewModelScope.launch {
            quranRepository.translations().collect { list ->
                val previews = mutableMapOf<String, String>()
                for (t in list) {
                    val a = quranRepository.ayahWithTextBySurahAndNo(112, 1, t.id)
                    if (a != null) {
                        previews[t.id] = a.bangla
                    }
                }
                _translationPreviews.value = previews
            }
        }
    }

    fun setThemeMode(mode: String) {
        viewModelScope.launch {
            settingsRepository.setThemeMode(mode)
        }
    }

    fun setAppLanguage(langTag: String) {
        viewModelScope.launch {
            settingsRepository.setAppLanguage(langTag)
            val localeList = if (langTag.isEmpty()) {
                LocaleListCompat.getEmptyLocaleList()
            } else {
                LocaleListCompat.forLanguageTags(langTag)
            }
            AppCompatDelegate.setApplicationLocales(localeList)
        }
    }

    fun setArabicSize(size: Int) {
        viewModelScope.launch {
            settingsRepository.setArabicSizeSp(size)
        }
    }

    fun setBanglaSize(size: Int) {
        viewModelScope.launch {
            settingsRepository.setBanglaSizeSp(size)
        }
    }

    fun setShowBangla(show: Boolean) {
        viewModelScope.launch {
            settingsRepository.setShowBangla(show)
        }
    }

    fun setBanglaDigits(useBangla: Boolean) {
        viewModelScope.launch {
            settingsRepository.setBanglaDigits(useBangla)
        }
    }

    fun setKeepScreenOn(keepOn: Boolean) {
        viewModelScope.launch {
            settingsRepository.setKeepScreenOn(keepOn)
        }
    }

    fun setWidgetRefreshOnScreenOn(refresh: Boolean) {
        viewModelScope.launch {
            settingsRepository.setWidgetRefreshOnScreenOn(refresh)
        }
    }

    fun setTranslation(id: String) {
        viewModelScope.launch {
            settingsRepository.setTranslationId(id)
            WidgetUpdater.rerender(context)
        }
    }

    fun setWidgetArabicScale(pct: Int) {
        viewModelScope.launch {
            settingsRepository.setWidgetArabicScalePct(pct)
            WidgetUpdater.rerender(context)
        }
    }

    fun setWidgetTranslationScale(pct: Int) {
        viewModelScope.launch {
            settingsRepository.setWidgetTranslationScalePct(pct)
            WidgetUpdater.rerender(context)
        }
    }

    fun resetWidgetTextSize() {
        viewModelScope.launch {
            settingsRepository.resetWidgetTextSize()
            WidgetUpdater.rerender(context)
        }
    }

    fun resetReadingProgress() {
        viewModelScope.launch {
            settingsRepository.resetLastRead()
        }
    }

    fun deleteAllBookmarks() {
        viewModelScope.launch {
            quranRepository.deleteAllBookmarks()
        }
    }

    fun setAudioReciterId(reciterId: String) {
        viewModelScope.launch {
            settingsRepository.setAudioReciterId(reciterId)
        }
    }

    fun setAudioWifiOnly(wifiOnly: Boolean) {
        viewModelScope.launch {
            settingsRepository.setAudioWifiOnly(wifiOnly)
        }
    }

    fun setAudioBismillahPrefix(prefix: Boolean) {
        viewModelScope.launch {
            settingsRepository.setAudioBismillahPrefix(prefix)
        }
    }

    fun setAudioFollow(follow: Boolean) {
        viewModelScope.launch {
            settingsRepository.setAudioFollow(follow)
        }
    }

    class Factory(
        private val context: Context,
        private val quranRepository: QuranRepository,
        private val settingsRepository: SettingsRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SettingsViewModel(context, quranRepository, settingsRepository) as T
        }
    }
}
