package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface QuranDao {

    @Query("SELECT * FROM surah ORDER BY id ASC")
    fun surahs(): Flow<List<SurahEntity>>

    @Query("SELECT * FROM surah WHERE id = :id LIMIT 1")
    suspend fun surah(id: Int): SurahEntity?

    @Query("SELECT * FROM surah WHERE id = :id LIMIT 1")
    fun surahFlow(id: Int): Flow<SurahEntity?>

    @Query("SELECT * FROM ayah WHERE surahId = :surahId ORDER BY ayahNo ASC")
    fun ayahsOfSurah(surahId: Int): Flow<List<AyahEntity>>

    @Query("""
        SELECT 
            a.id,
            a.surahId,
            a.ayahNo,
            a.arabicOriginal,
            a.arabicDisplay,
            a.arabicSearch,
            COALESCE(t.text, a.bangla) AS bangla
        FROM ayah a
        LEFT JOIN translation_text t ON a.id = t.ayahId AND t.translationId = :translationId
        WHERE a.surahId = :surahId
        ORDER BY a.ayahNo ASC
    """)
    fun ayahsOfSurahWithText(surahId: Int, translationId: String): Flow<List<AyahWithText>>

    @Query("SELECT * FROM ayah WHERE id = :id LIMIT 1")
    suspend fun ayahById(id: Int): AyahEntity?

    @Query("""
        SELECT 
            a.id,
            a.surahId,
            a.ayahNo,
            a.arabicOriginal,
            a.arabicDisplay,
            a.arabicSearch,
            COALESCE(t.text, a.bangla) AS bangla
        FROM ayah a
        LEFT JOIN translation_text t ON a.id = t.ayahId AND t.translationId = :translationId
        WHERE a.id = :ayahId
        LIMIT 1
    """)
    suspend fun ayahWithText(ayahId: Int, translationId: String): AyahWithText?

    @Query("""
        SELECT 
            a.id,
            a.surahId,
            a.ayahNo,
            a.arabicOriginal,
            a.arabicDisplay,
            a.arabicSearch,
            COALESCE(t.text, a.bangla) AS bangla
        FROM ayah a
        LEFT JOIN translation_text t ON a.id = t.ayahId AND t.translationId = :translationId
        WHERE a.surahId = :surahId AND a.ayahNo = :ayahNo
        LIMIT 1
    """)
    suspend fun ayahWithTextBySurahAndNo(surahId: Int, ayahNo: Int, translationId: String): AyahWithText?

    @Query("SELECT * FROM ayah WHERE surahId = :surahId AND ayahNo = :ayahNo LIMIT 1")
    suspend fun ayahBySurahAndNo(surahId: Int, ayahNo: Int): AyahEntity?

    @Query("SELECT * FROM ayah ORDER BY RANDOM() LIMIT 1")
    suspend fun randomAyah(): AyahEntity?

    @Query("""
        SELECT 
            a.id,
            a.surahId,
            a.ayahNo,
            a.arabicOriginal,
            a.arabicDisplay,
            a.arabicSearch,
            COALESCE(t.text, a.bangla) AS bangla
        FROM ayah a
        LEFT JOIN translation_text t ON a.id = t.ayahId AND t.translationId = :translationId
        ORDER BY RANDOM()
        LIMIT 1
    """)
    suspend fun randomAyahWithText(translationId: String): AyahWithText?

    @Query("SELECT id FROM ayah WHERE LENGTH(arabicDisplay) <= :maxArabic AND LENGTH(bangla) <= :maxBangla ORDER BY RANDOM() LIMIT 1")
    suspend fun randomWidgetAyahId(maxArabic: Int, maxBangla: Int): Int?

    @Query("""
        SELECT a.id FROM ayah a
        LEFT JOIN translation_text t ON a.id = t.ayahId AND t.translationId = :translationId
        WHERE LENGTH(a.arabicDisplay) <= :maxArabic 
          AND LENGTH(COALESCE(t.text, a.bangla)) <= :maxTranslation
        ORDER BY RANDOM() LIMIT 1
    """)
    suspend fun randomWidgetAyahIdWithText(translationId: String, maxArabic: Int, maxTranslation: Int): Int?

    @Query("SELECT id FROM ayah ORDER BY RANDOM() LIMIT 1")
    suspend fun fallbackRandomAyahId(): Int?

    @Query("""
        SELECT * FROM ayah 
        WHERE bangla LIKE '%' || :q || '%' ESCAPE '\' 
           OR arabicSearch LIKE '%' || :qNorm || '%' ESCAPE '\' 
        ORDER BY id ASC LIMIT 100
    """)
    suspend fun search(q: String, qNorm: String): List<AyahEntity>

    @Query("""
        SELECT 
            a.id,
            a.surahId,
            a.ayahNo,
            a.arabicOriginal,
            a.arabicDisplay,
            a.arabicSearch,
            COALESCE(t.text, a.bangla) AS bangla
        FROM ayah a
        LEFT JOIN translation_text t ON a.id = t.ayahId AND t.translationId = :translationId
        WHERE COALESCE(t.text, a.bangla) LIKE '%' || :q || '%' ESCAPE '\' 
           OR a.arabicSearch LIKE '%' || :qNorm || '%' ESCAPE '\' 
        ORDER BY a.id ASC LIMIT 100
    """)
    suspend fun searchWithText(q: String, qNorm: String, translationId: String): List<AyahWithText>

    @Query("SELECT * FROM juz ORDER BY `index` ASC")
    fun juzList(): Flow<List<JuzEntity>>

    @Query("SELECT id, surahId, ayahNo FROM ayah")
    suspend fun ayahKeys(): List<AyahKey>

    @Query("SELECT COUNT(*) FROM ayah")
    suspend fun ayahCount(): Int

    @Query("SELECT COUNT(*) FROM surah")
    suspend fun surahCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSurahs(surahs: List<SurahEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAyahs(ayahs: List<AyahEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJuzs(juzs: List<JuzEntity>)

    @Query("DELETE FROM surah")
    suspend fun clearSurahs()

    @Query("DELETE FROM ayah")
    suspend fun clearAyahs()

    @Query("DELETE FROM juz")
    suspend fun clearJuzs()

    // --- Translations queries ---

    @Query("SELECT * FROM translation_info ORDER BY isDefault DESC, nameEn ASC")
    fun translations(): Flow<List<TranslationInfoEntity>>

    @Query("SELECT * FROM translation_info ORDER BY isDefault DESC, nameEn ASC")
    suspend fun translationsList(): List<TranslationInfoEntity>

    @Query("SELECT * FROM translation_info WHERE id = :id LIMIT 1")
    suspend fun translationInfo(id: String): TranslationInfoEntity?

    @Query("SELECT * FROM translation_info WHERE id = :id LIMIT 1")
    fun translationInfoFlow(id: String): Flow<TranslationInfoEntity?>

    @Query("SELECT COUNT(*) FROM translation_text WHERE translationId = :translationId")
    suspend fun translationTextCount(translationId: String): Int

    @Query("SELECT COUNT(*) FROM translation_info")
    suspend fun translationInfoCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTranslationTexts(texts: List<TranslationTextEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTranslationInfo(info: TranslationInfoEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTranslationInfos(infos: List<TranslationInfoEntity>)

    @Query("DELETE FROM translation_text WHERE translationId = :translationId")
    suspend fun clearTranslationTexts(translationId: String)
}
