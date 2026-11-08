package com.example.data

import com.example.data.db.AyahEntity
import com.example.data.db.AyahWithText
import com.example.data.db.BookmarkEntity
import com.example.data.db.JuzEntity
import com.example.data.db.QuranDatabase
import com.example.data.db.SurahEntity
import com.example.data.db.TranslationInfoEntity
import com.example.data.import.ArabicNormalizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class QuranRepository(private val database: QuranDatabase) {

    private val quranDao = database.quranDao()
    private val bookmarkDao = database.bookmarkDao()

    fun surahs(): Flow<List<SurahEntity>> = quranDao.surahs()

    suspend fun getSurah(id: Int): SurahEntity? = withContext(Dispatchers.IO) {
        quranDao.surah(id)
    }

    fun surahFlow(id: Int): Flow<SurahEntity?> = quranDao.surahFlow(id)

    fun ayahsOfSurah(surahId: Int): Flow<List<AyahEntity>> = quranDao.ayahsOfSurah(surahId)

    fun ayahsOfSurahWithText(surahId: Int, translationId: String): Flow<List<AyahWithText>> =
        quranDao.ayahsOfSurahWithText(surahId, translationId)

    suspend fun ayahById(id: Int): AyahEntity? = withContext(Dispatchers.IO) {
        quranDao.ayahById(id)
    }

    suspend fun ayahWithText(ayahId: Int, translationId: String): AyahWithText? = withContext(Dispatchers.IO) {
        quranDao.ayahWithText(ayahId, translationId)
    }

    suspend fun ayahBySurahAndNo(surahId: Int, ayahNo: Int): AyahEntity? = withContext(Dispatchers.IO) {
        quranDao.ayahBySurahAndNo(surahId, ayahNo)
    }

    suspend fun ayahWithTextBySurahAndNo(surahId: Int, ayahNo: Int, translationId: String): AyahWithText? = withContext(Dispatchers.IO) {
        quranDao.ayahWithTextBySurahAndNo(surahId, ayahNo, translationId)
    }

    suspend fun randomAyah(): AyahEntity? = withContext(Dispatchers.IO) {
        quranDao.randomAyah()
    }

    suspend fun randomAyahWithText(translationId: String): AyahWithText? = withContext(Dispatchers.IO) {
        quranDao.randomAyahWithText(translationId)
    }

    suspend fun randomWidgetAyahId(maxArabic: Int, maxBangla: Int): Int? = withContext(Dispatchers.IO) {
        val candidate = quranDao.randomWidgetAyahId(maxArabic, maxBangla)
        candidate ?: quranDao.fallbackRandomAyahId()
    }

    suspend fun randomWidgetAyahIdWithText(translationId: String, maxArabic: Int, maxTranslation: Int): Int? = withContext(Dispatchers.IO) {
        val candidate = quranDao.randomWidgetAyahIdWithText(translationId, maxArabic, maxTranslation)
        candidate ?: quranDao.fallbackRandomAyahId()
    }

    suspend fun search(rawQuery: String): List<AyahEntity> = withContext(Dispatchers.IO) {
        val trimmed = rawQuery.trim()
        if (trimmed.length < 2) return@withContext emptyList()

        val escapedBangla = escapeLike(trimmed)
        val normalizedArabic = ArabicNormalizer.normalize(trimmed)
        val escapedArabic = escapeLike(normalizedArabic)

        quranDao.search(escapedBangla, escapedArabic)
    }

    suspend fun searchWithText(rawQuery: String, translationId: String): List<AyahWithText> = withContext(Dispatchers.IO) {
        val trimmed = rawQuery.trim()
        if (trimmed.length < 2) return@withContext emptyList()

        val escapedBangla = escapeLike(trimmed)
        val normalizedArabic = ArabicNormalizer.normalize(trimmed)
        val escapedArabic = escapeLike(normalizedArabic)

        quranDao.searchWithText(escapedBangla, escapedArabic, translationId)
    }

    fun juzList(): Flow<List<JuzEntity>> = quranDao.juzList()

    fun bookmarks(): Flow<List<BookmarkEntity>> = bookmarkDao.observeAll()

    fun isBookmarked(ayahId: Int): Flow<Boolean> = bookmarkDao.isBookmarked(ayahId)

    suspend fun toggleBookmark(ayahId: Int) = withContext(Dispatchers.IO) {
        if (bookmarkDao.isBookmarkedDirect(ayahId)) {
            bookmarkDao.delete(ayahId)
        } else {
            bookmarkDao.insert(BookmarkEntity(ayahId = ayahId, createdAt = System.currentTimeMillis()))
        }
    }

    suspend fun addBookmark(ayahId: Int) = withContext(Dispatchers.IO) {
        bookmarkDao.insert(BookmarkEntity(ayahId = ayahId, createdAt = System.currentTimeMillis()))
    }

    suspend fun removeBookmark(ayahId: Int) = withContext(Dispatchers.IO) {
        bookmarkDao.delete(ayahId)
    }

    suspend fun deleteAllBookmarks() = withContext(Dispatchers.IO) {
        bookmarkDao.deleteAll()
    }

    // Translations
    fun translations(): Flow<List<TranslationInfoEntity>> = quranDao.translations()

    suspend fun translationsList(): List<TranslationInfoEntity> = withContext(Dispatchers.IO) {
        quranDao.translationsList()
    }

    suspend fun translationInfo(id: String): TranslationInfoEntity? = withContext(Dispatchers.IO) {
        quranDao.translationInfo(id)
    }

    fun translationInfoFlow(id: String): Flow<TranslationInfoEntity?> = quranDao.translationInfoFlow(id)

    private fun escapeLike(input: String): String {
        return input
            .replace("\\", "\\\\")
            .replace("%", "\\%")
            .replace("_", "\\_")
    }
}
