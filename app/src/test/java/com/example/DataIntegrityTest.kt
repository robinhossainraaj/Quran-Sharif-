package com.example

import com.example.data.import.ArabicNormalizer
import com.example.data.import.BismillahHelper
import com.example.data.import.QuranDataXmlParser
import com.example.data.import.TanzilParser
import com.example.widget.WidgetSizing
import java.io.File
import java.io.FileInputStream
import org.json.JSONArray
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DataIntegrityTest {

    private fun getAssetFile(filename: String): File {
        val candidates = listOf(
            File("src/main/assets", filename),
            File("app/src/main/assets", filename),
            File("../app/src/main/assets", filename)
        )
        return candidates.firstOrNull { it.exists() }
            ?: throw IllegalStateException("Could not find asset $filename in paths: $candidates")
    }

    @Test
    fun testAllInvariantsAndSpotChecks() {
        val uthmaniFile = getAssetFile("quran-uthmani.txt")
        val cleanFile = getAssetFile("quran-simple-clean.txt")
        val banglaFile = getAssetFile("bn.bengali.txt")
        val xmlFile = getAssetFile("quran-data.xml")

        val uthmaniResult = FileInputStream(uthmaniFile).use { TanzilParser.parse(it, "uthmani") }
        val cleanResult = FileInputStream(cleanFile).use { TanzilParser.parse(it, "clean") }
        val banglaResult = FileInputStream(banglaFile).use { TanzilParser.parse(it, "bangla") }
        val xmlResult = FileInputStream(xmlFile).use { QuranDataXmlParser.parse(it) }

        // Invariant 1: Exactly 6236 ayahs in each file
        assertEquals(6236, uthmaniResult.ayahs.size)
        assertEquals(6236, cleanResult.ayahs.size)
        assertEquals(6236, banglaResult.ayahs.size)

        // Invariant 2: Exactly 114 surahs
        assertEquals(114, xmlResult.suras.size)

        val surahAyasMap = xmlResult.suras.associate { it.index to it.ayas }
        val totalXmlAyas = xmlResult.suras.sumOf { it.ayas }
        assertEquals(6236, totalXmlAyas)

        // Spot checks on surah ayah counts
        assertEquals(7, surahAyasMap[1])
        assertEquals(286, surahAyasMap[2])
        assertEquals(200, surahAyasMap[3])
        assertEquals(129, surahAyasMap[9])
        assertEquals(110, surahAyasMap[18])
        assertEquals(83, surahAyasMap[36])
        assertEquals(78, surahAyasMap[55])
        assertEquals(30, surahAyasMap[67])
        assertEquals(4, surahAyasMap[112])
        assertEquals(5, surahAyasMap[113])
        assertEquals(6, surahAyasMap[114])

        // Group by surah and check identical ordering and counts
        val uBySurah = uthmaniResult.ayahs.groupBy { it.surahId }
        val cBySurah = cleanResult.ayahs.groupBy { it.surahId }
        val bBySurah = banglaResult.ayahs.groupBy { it.surahId }

        for (s in 1..114) {
            val expected = surahAyasMap[s]
            assertNotNull(expected)
            val uList = uBySurah[s] ?: emptyList()
            val cList = cBySurah[s] ?: emptyList()
            val bList = bBySurah[s] ?: emptyList()

            assertEquals("Surah $s count in Uthmani", expected, uList.size)
            assertEquals("Surah $s count in Clean", expected, cList.size)
            assertEquals("Surah $s count in Bangla", expected, bList.size)

            for (a in 1..expected!!) {
                val uAyah = uList[a - 1]
                val cAyah = cList[a - 1]
                val bAyah = bList[a - 1]

                assertEquals(a, uAyah.ayahNo)
                assertEquals(a, cAyah.ayahNo)
                assertEquals(a, bAyah.ayahNo)

                assertTrue("Non-blank Uthmani $s:$a", uAyah.text.isNotBlank())
                assertTrue("Non-blank Clean $s:$a", cAyah.text.isNotBlank())
                assertTrue("Non-blank Bangla $s:$a", bAyah.text.isNotBlank())
            }
        }

        // Invariant 5: 30 juz entries and spot checks
        assertEquals(30, xmlResult.juzs.size)
        val j1 = xmlResult.juzs[0]
        assertEquals(1, j1.index)
        assertEquals(1, j1.sura)
        assertEquals(1, j1.aya)

        val j2 = xmlResult.juzs[1]
        assertEquals(2, j2.sura)
        assertEquals(142, j2.aya)

        val j3 = xmlResult.juzs[2]
        assertEquals(2, j3.sura)
        assertEquals(253, j3.aya)

        val j29 = xmlResult.juzs[28]
        assertEquals(67, j29.sura)
        assertEquals(1, j29.aya)

        val j30 = xmlResult.juzs[29]
        assertEquals(78, j30.sura)
        assertEquals(1, j30.aya)

        // Bismillah handling check: arabicDisplay of ayah 1 is non-empty for every surah
        val surah1Ayah1 = uthmaniResult.ayahs.first { it.surahId == 1 && it.ayahNo == 1 }
        val normalizedBismillah = ArabicNormalizer.normalize(surah1Ayah1.text)

        for (s in 1..114) {
            val ayah1 = uBySurah[s]!!.first { it.ayahNo == 1 }
            val displayArabic = BismillahHelper.extractDisplayArabic(
                surahId = s,
                ayahNo = 1,
                arabicText = ayah1.text,
                normalizedBismillah = normalizedBismillah
            )
            assertTrue("Surah $s Ayah 1 display text must not be empty", displayArabic.isNotBlank())
        }
    }

    @Test
    fun testTranslationsCatalogAndFiles() {
        val catalogFile = getAssetFile("translations/translations.json")
        val jsonText = catalogFile.readText()
        val jsonArray = JSONArray(jsonText)
        assertTrue("Catalog must contain at least 2 translations", jsonArray.length() >= 2)

        for (i in 0 until jsonArray.length()) {
            val item = jsonArray.getJSONObject(i)
            val id = item.getString("id")
            val filePath = item.getString("file")
            val transFile = getAssetFile(filePath)
            assertTrue("Translation file for $id must exist", transFile.exists())

            val parsed = FileInputStream(transFile).use { TanzilParser.parse(it, id) }
            assertEquals("Translation $id must have exactly 6236 ayahs", 6236, parsed.ayahs.size)

            // Spot check: Surah 112 Ayah 1
            val ayah112_1 = parsed.ayahs.first { it.surahId == 112 && it.ayahNo == 1 }
            assertTrue("Surah 112 Ayah 1 in $id must not be empty", ayah112_1.text.isNotBlank())
        }
    }

    @Test
    fun testWidgetSizingMath() {
        // Default 100%
        assertEquals(46f, WidgetSizing.computeArabicStartPx(100), 0.01f)
        assertEquals(24f, WidgetSizing.computeArabicMinPx(100), 0.01f)
        assertEquals(14f, WidgetSizing.computeTranslationSp(100), 0.01f)

        // 70%
        val minAr = WidgetSizing.computeArabicStartPx(70)
        assertTrue(minAr >= 20f)
        val minTr = WidgetSizing.computeTranslationSp(70)
        assertTrue(minTr >= 9f)

        // 150%
        val maxAr = WidgetSizing.computeArabicStartPx(150)
        assertTrue(maxAr <= 90f)
        val maxTr = WidgetSizing.computeTranslationSp(150)
        assertTrue(maxTr <= 26f)

        // Length limits inversely scale
        assertTrue(WidgetSizing.maxArabicLength(150) < WidgetSizing.maxArabicLength(100))
        assertTrue(WidgetSizing.maxTranslationLength(150) < WidgetSizing.maxTranslationLength(100))
    }
}
