package com.example.ui.bookmarks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.QuranRepository
import com.example.data.db.AyahWithText
import com.example.data.db.SurahEntity
import com.example.data.prefs.SettingsRepository
import com.example.data.prefs.UserSettings
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class BookmarkItem(
    val ayah: AyahWithText,
    val surah: SurahEntity,
    val createdAt: Long
)

data class BookmarksUiState(
    val bookmarks: List<BookmarkItem> = emptyList(),
    val userSettings: UserSettings = UserSettings(),
    val isLoading: Boolean = true
)

class BookmarksViewModel(
    private val quranRepository: QuranRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val uiState: StateFlow<BookmarksUiState> = combine(
        quranRepository.bookmarks(),
        settingsRepository.settingsFlow
    ) { bookmarks, settings ->
        val surahCache = HashMap<Int, SurahEntity>()
        val items = bookmarks.mapNotNull { bm ->
            val ayah = quranRepository.ayahWithText(bm.ayahId, settings.translationId) ?: return@mapNotNull null
            val surah = surahCache.getOrPut(ayah.surahId) {
                quranRepository.getSurah(ayah.surahId) ?: return@mapNotNull null
            }
            BookmarkItem(ayah = ayah, surah = surah, createdAt = bm.createdAt)
        }
        BookmarksUiState(
            bookmarks = items,
            userSettings = settings,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = BookmarksUiState()
    )

    fun deleteBookmark(ayahId: Int) {
        viewModelScope.launch {
            quranRepository.removeBookmark(ayahId)
        }
    }

    fun restoreBookmark(ayahId: Int) {
        viewModelScope.launch {
            quranRepository.addBookmark(ayahId)
        }
    }

    fun deleteAllBookmarks() {
        viewModelScope.launch {
            quranRepository.deleteAllBookmarks()
        }
    }

    class Factory(
        private val quranRepository: QuranRepository,
        private val settingsRepository: SettingsRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return BookmarksViewModel(quranRepository, settingsRepository) as T
        }
    }
}
