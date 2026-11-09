package com.example.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.QuranRepository
import com.example.data.db.AyahWithText
import com.example.data.db.SurahEntity
import com.example.data.prefs.SettingsRepository
import com.example.data.prefs.UserSettings
import com.example.util.BanglaDigits
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SearchResultItem(
    val ayah: AyahWithText,
    val surah: SurahEntity
)

data class DirectReferenceResult(
    val surahId: Int,
    val ayahNo: Int,
    val surahBanglaName: String
)

data class SearchUiState(
    val query: String = "",
    val directReference: DirectReferenceResult? = null,
    val results: List<SearchResultItem> = emptyList(),
    val isSearching: Boolean = false,
    val hasSearched: Boolean = false,
    val userSettings: UserSettings = UserSettings()
)

data class SearchData(
    val query: String = "",
    val directRef: DirectReferenceResult? = null,
    val results: List<SearchResultItem> = emptyList(),
    val isSearching: Boolean = false,
    val hasSearched: Boolean = false
)

@OptIn(FlowPreview::class)
class SearchViewModel(
    private val quranRepository: QuranRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _searchData = MutableStateFlow(SearchData())
    private val _queryFlow = MutableStateFlow("")

    val uiState: StateFlow<SearchUiState> = combine(
        _searchData,
        settingsRepository.settingsFlow
    ) { data, settings ->
        SearchUiState(
            query = data.query,
            directReference = data.directRef,
            results = data.results,
            isSearching = data.isSearching,
            hasSearched = data.hasSearched,
            userSettings = settings
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SearchUiState()
    )

    init {
        _queryFlow
            .debounce(300)
            .distinctUntilChanged()
            .onEach { q ->
                performSearch(q)
            }
            .launchIn(viewModelScope)

        viewModelScope.launch {
            settingsRepository.settingsFlow
                .map { it.translationId }
                .distinctUntilChanged()
                .collect {
                    if (_queryFlow.value.trim().length >= 2) {
                        performSearch(_queryFlow.value)
                    }
                }
        }
    }

    fun onQueryChange(newQuery: String) {
        _queryFlow.value = newQuery
        _searchData.value = _searchData.value.copy(query = newQuery)
    }

    fun clearQuery() {
        _queryFlow.value = ""
        _searchData.value = SearchData()
    }

    private fun performSearch(rawQuery: String) {
        // The trimmed text is used for searching only. The text the user typed (spaces included)
        // is never overwritten here, otherwise a space typed at the end would be deleted.
        val trimmed = rawQuery.trim()
        if (trimmed.length < 2) {
            _searchData.value = _searchData.value.copy(
                directRef = null,
                results = emptyList(),
                isSearching = false,
                hasSearched = false
            )
            return
        }

        _searchData.value = _searchData.value.copy(isSearching = true)

        viewModelScope.launch {
            // Check for reference pattern like 2:255 or ২:২৫৫
            val asciiQuery = BanglaDigits.toAscii(trimmed)
            val refMatch = Regex("""^(\d{1,3})\s*[:：]\s*(\d{1,3})$""").find(asciiQuery)

            if (refMatch != null) {
                val surahId = refMatch.groupValues[1].toInt()
                val ayahNo = refMatch.groupValues[2].toInt()
                val surah = quranRepository.getSurah(surahId)
                if (surah != null && ayahNo in 1..surah.ayahCount) {
                    _searchData.value = _searchData.value.copy(
                        directRef = DirectReferenceResult(
                            surahId = surahId,
                            ayahNo = ayahNo,
                            surahBanglaName = surah.banglaName
                        ),
                        results = emptyList(),
                        isSearching = false,
                        hasSearched = true
                    )
                    return@launch
                }
            }

            // Text search with current translation
            val currentTransId = settingsRepository.settingsFlow.first().translationId
            val ayahs = quranRepository.searchWithText(trimmed, currentTransId)
            val surahCache = HashMap<Int, SurahEntity>()
            val items = ayahs.mapNotNull { ayah ->
                val surah = surahCache.getOrPut(ayah.surahId) {
                    quranRepository.getSurah(ayah.surahId) ?: return@mapNotNull null
                }
                SearchResultItem(ayah = ayah, surah = surah)
            }

            _searchData.value = _searchData.value.copy(
                directRef = null,
                results = items,
                isSearching = false,
                hasSearched = true
            )
        }
    }

    class Factory(
        private val quranRepository: QuranRepository,
        private val settingsRepository: SettingsRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SearchViewModel(quranRepository, settingsRepository) as T
        }
    }
}
