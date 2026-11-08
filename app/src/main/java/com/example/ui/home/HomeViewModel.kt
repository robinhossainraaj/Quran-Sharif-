package com.example.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.QuranRepository
import com.example.data.db.AyahWithText
import com.example.data.db.SurahEntity
import com.example.data.db.TranslationInfoEntity
import com.example.data.prefs.SettingsRepository
import com.example.data.prefs.UserSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val currentAyah: AyahWithText? = null,
    val currentSurah: SurahEntity? = null,
    val translationInfo: TranslationInfoEntity? = null,
    val isAyahBookmarked: Boolean = false,
    val lastReadSurah: SurahEntity? = null,
    val lastReadAyahNo: Int = 0,
    val userSettings: UserSettings = UserSettings(),
    val playbackState: com.example.audio.player.PlaybackUiState = com.example.audio.player.PlaybackUiState(),
    val isLoading: Boolean = false
)

class HomeViewModel(
    private val quranRepository: QuranRepository,
    private val settingsRepository: SettingsRepository,
    private val playbackController: com.example.audio.player.PlaybackController? = null
) : ViewModel() {

    private val _currentAyah = MutableStateFlow<AyahWithText?>(null)
    private val _currentSurah = MutableStateFlow<SurahEntity?>(null)
    private val _isBookmarked = MutableStateFlow(false)
    private val _lastReadSurah = MutableStateFlow<SurahEntity?>(null)
    private val _translationInfo = MutableStateFlow<TranslationInfoEntity?>(null)

    val uiState: StateFlow<HomeUiState> = combine(
        combine(_currentAyah, _currentSurah, _isBookmarked) { ayah, surah, bookmarked ->
            Triple(ayah, surah, bookmarked)
        },
        combine(_lastReadSurah, _translationInfo, settingsRepository.settingsFlow) { lastSurah, transInfo, settings ->
            Triple(lastSurah, transInfo, settings)
        },
        playbackController?.state ?: MutableStateFlow(com.example.audio.player.PlaybackUiState())
    ) { (ayah, surah, bookmarked), (lastSurah, transInfo, settings), playback ->
        HomeUiState(
            currentAyah = ayah,
            currentSurah = surah,
            translationInfo = transInfo,
            isAyahBookmarked = bookmarked,
            lastReadSurah = lastSurah,
            lastReadAyahNo = settings.lastReadAyah,
            userSettings = settings,
            playbackState = playback,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState(isLoading = true)
    )

    fun togglePlayback() {
        playbackController?.playOrPause()
    }

    fun stopPlayback() {
        playbackController?.stop()
    }

    init {
        observeTranslationChanges()
        observeLastRead()
    }

    private fun observeTranslationChanges() {
        viewModelScope.launch {
            settingsRepository.settingsFlow
                .map { it.translationId }
                .distinctUntilChanged()
                .collect { transId ->
                    _translationInfo.value = quranRepository.translationInfo(transId)
                    val existing = _currentAyah.value
                    if (existing != null) {
                        val updated = quranRepository.ayahWithText(existing.id, transId)
                        if (updated != null) {
                            _currentAyah.value = updated
                        }
                    } else {
                        refreshAyah()
                    }
                }
        }
    }

    fun refreshAyah() {
        viewModelScope.launch {
            val transId = _translationInfo.value?.id ?: settingsRepository.settingsFlow.map { it.translationId }.first()
            val ayah = quranRepository.randomAyahWithText(transId)
            if (ayah != null) {
                _currentAyah.value = ayah
                _currentSurah.value = quranRepository.getSurah(ayah.surahId)
                checkBookmark(ayah.id)
            }
        }
    }

    fun toggleBookmark() {
        val ayah = _currentAyah.value ?: return
        viewModelScope.launch {
            quranRepository.toggleBookmark(ayah.id)
            checkBookmark(ayah.id)
        }
    }

    private fun checkBookmark(ayahId: Int) {
        viewModelScope.launch {
            quranRepository.isBookmarked(ayahId).collect {
                _isBookmarked.value = it
            }
        }
    }

    private fun observeLastRead() {
        viewModelScope.launch {
            settingsRepository.settingsFlow.collect { settings ->
                if (settings.lastReadSurah > 0) {
                    _lastReadSurah.value = quranRepository.getSurah(settings.lastReadSurah)
                } else {
                    _lastReadSurah.value = null
                }
            }
        }
    }

    class Factory(
        private val quranRepository: QuranRepository,
        private val settingsRepository: SettingsRepository,
        private val playbackController: com.example.audio.player.PlaybackController? = null
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return HomeViewModel(quranRepository, settingsRepository, playbackController) as T
        }
    }
}
