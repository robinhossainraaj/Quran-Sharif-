package com.example.ui.quran

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.QuranRepository
import com.example.data.db.JuzEntity
import com.example.data.db.SurahEntity
import com.example.data.prefs.SettingsRepository
import com.example.data.prefs.UserSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class JuzWithSurah(
    val juz: JuzEntity,
    val surahBanglaName: String
)

data class QuranUiState(
    val surahs: List<SurahEntity> = emptyList(),
    val juzs: List<JuzWithSurah> = emptyList(),
    val selectedTab: Int = 0,
    val userSettings: UserSettings = UserSettings(),
    val isLoading: Boolean = true
)

class QuranViewModel(
    private val quranRepository: QuranRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _selectedTab = MutableStateFlow(0)

    val uiState: StateFlow<QuranUiState> = combine(
        quranRepository.surahs(),
        quranRepository.juzList(),
        _selectedTab,
        settingsRepository.settingsFlow
    ) { surahs, juzs, tab, settings ->
        val surahMap = surahs.associateBy { it.id }
        val juzWithSurahList = juzs.map { j ->
            JuzWithSurah(
                juz = j,
                surahBanglaName = surahMap[j.startSurah]?.banglaName ?: ""
            )
        }
        QuranUiState(
            surahs = surahs,
            juzs = juzWithSurahList,
            selectedTab = tab,
            userSettings = settings,
            isLoading = surahs.isEmpty()
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = QuranUiState()
    )

    fun selectTab(tabIndex: Int) {
        _selectedTab.value = tabIndex
    }

    class Factory(
        private val quranRepository: QuranRepository,
        private val settingsRepository: SettingsRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return QuranViewModel(quranRepository, settingsRepository) as T
        }
    }
}
