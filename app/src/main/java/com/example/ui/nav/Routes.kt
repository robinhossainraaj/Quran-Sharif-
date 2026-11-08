package com.example.ui.nav

object Routes {
    const val HOME = "home"
    const val QURAN = "quran"
    const val BOOKMARKS = "bookmarks"
    const val SETTINGS = "settings"
    const val ABOUT = "about"
    const val DOWNLOADS = "downloads"
    const val SEARCH = "search"
    const val TRANSLATIONS = "translations"
    const val CREDITS = "credits"
    const val READER = "reader/{surahId}?ayah={ayahNo}"

    fun reader(surahId: Int, ayahNo: Int = -1): String {
        return "reader/$surahId?ayah=$ayahNo"
    }

    fun quranWithTab(tabIndex: Int): String {
        return "quran?tab=$tabIndex"
    }
}
