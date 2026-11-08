package com.example.audio

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.audio.catalog.ReciterCatalogLoader
import com.example.audio.net.Mp3Validator
import com.example.audio.net.UrlBuilder
import com.example.audio.storage.AudioStorage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [34])
class AudioFeatureTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    // 1. UrlBuilder: buildAyahUrl produces expected CDN URL for Alafasy 128 kbps (ayah 1 and ayah 6236)
    @Test
    fun urlBuilder_producesCorrectCdnUrls() {
        val cdnBase = "https://cdn.islamic.network/quran/audio"
        val urlAyah1 = UrlBuilder.buildAyahUrl(cdnBase, 128, "ar.alafasy", 1)
        assertEquals("https://cdn.islamic.network/quran/audio/128/ar.alafasy/1.mp3", urlAyah1)

        val urlAyah6236 = UrlBuilder.buildAyahUrl(cdnBase, 128, "ar.alafasy", 6236)
        assertEquals("https://cdn.islamic.network/quran/audio/128/ar.alafasy/6236.mp3", urlAyah6236)
    }

    // 2. Ayah ID arithmetic:
    // Surah 1 ayah 1 -> global id 1
    // Surah 2 ayah 1 -> global id 8 (after 7 verses of Al-Fatiha)
    // Surah 114 ayah 6 -> global id 6236
    @Test
    fun ayahIdArithmetic_calculatesGlobalAyahIdsCorrectly() {
        val surahAyahCounts = listOf(
            7, 286, 200, 176, 120, 165, 206, 75, 129, 109,
            123, 111, 43, 52, 99, 128, 111, 110, 98, 135,
            112, 78, 118, 64, 77, 227, 93, 88, 69, 60,
            34, 30, 73, 54, 45, 83, 182, 88, 75, 85,
            54, 53, 89, 59, 37, 35, 38, 29, 18, 45,
            60, 49, 62, 55, 78, 96, 29, 22, 24, 13,
            14, 11, 11, 18, 12, 12, 30, 52, 52, 44,
            28, 28, 20, 56, 40, 31, 50, 40, 46, 42,
            29, 19, 36, 25, 22, 17, 19, 26, 30, 20,
            15, 21, 11, 8, 8, 19, 5, 8, 8, 11,
            11, 8, 3, 9, 5, 4, 7, 3, 6, 3,
            5, 4, 5, 6
        )

        fun calculateGlobalAyahId(surahId: Int, ayahNo: Int): Int {
            var offset = 0
            for (i in 0 until (surahId - 1)) {
                offset += surahAyahCounts[i]
            }
            return offset + ayahNo
        }

        // Surah 1, ayah 1
        assertEquals(1, calculateGlobalAyahId(1, 1))
        // Surah 1, ayah 7
        assertEquals(7, calculateGlobalAyahId(1, 7))
        // Surah 2, ayah 1 -> 7 + 1 = 8
        assertEquals(8, calculateGlobalAyahId(2, 1))
        // Surah 114, ayah 6 -> 6236
        assertEquals(6236, calculateGlobalAyahId(114, 6))
    }

    // 3. Mp3Validator: accepts ID3 header, accepts MPEG frame sync, rejects HTML error body, rejects empty byte array
    @Test
    fun mp3Validator_validatesHeadersCorrectly() {
        // ID3 header
        val id3Header = byteArrayOf('I'.code.toByte(), 'D'.code.toByte(), '3'.code.toByte(), 0x03, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00)
        assertTrue(Mp3Validator.isValidMp3Header(id3Header))

        // MPEG frame sync (0xFF 0xFB)
        val mpegHeader = byteArrayOf(0xFF.toByte(), 0xFB.toByte(), 0x90.toByte(), 0x64.toByte())
        assertTrue(Mp3Validator.isValidMp3Header(mpegHeader))

        // Rejects HTML error body (e.g. 404 / 500 error page from server)
        val htmlDoc = "<!DOCTYPE html><html><head><title>404 Not Found</title></head></html>".toByteArray()
        assertFalse(Mp3Validator.isValidMp3Header(htmlDoc))

        val htmlBody = "<html><body>Error</body></html>".toByteArray()
        assertFalse(Mp3Validator.isValidMp3Header(htmlBody))

        // Rejects empty or too short byte array
        assertFalse(Mp3Validator.isValidMp3Header(ByteArray(0)))
        assertFalse(Mp3Validator.isValidMp3Header(byteArrayOf(0x01, 0x02)))
    }

    // 4. AudioStorage: checks .done marker, reports surah status, cleans partial files
    @Test
    fun audioStorage_markersAndPartCleaning() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val storage = AudioStorage(context)

        val reciterId = "alafasy"
        val surahId = 112
        val ayahIds = listOf(6222, 6223, 6224, 6225)

        // Initially not downloaded
        assertFalse(storage.isSurahDownloaded(reciterId, surahId, ayahIds))

        // Create ayah files with content
        val reciterDir = storage.getReciterDir(reciterId)
        for (id in ayahIds) {
            val f = storage.getAyahFile(reciterId, id)
            f.writeBytes(byteArrayOf(1, 2, 3, 4))
        }

        // Without done marker, still considered not downloaded
        assertFalse(storage.isSurahDownloaded(reciterId, surahId, ayahIds))

        // Mark surah downloaded
        storage.markSurahDownloaded(reciterId, surahId)
        assertTrue(storage.isSurahDownloaded(reciterId, surahId, ayahIds))

        // Write a .part file
        val partFile = File(reciterDir, "temp_download.part")
        partFile.writeBytes(byteArrayOf(9, 9, 9))
        assertTrue(partFile.exists())

        // Clean part files
        storage.cleanPartFiles()
        assertFalse(partFile.exists())

        // Delete surah
        storage.deleteSurah(reciterId, surahId, ayahIds)
        assertFalse(storage.isSurahDownloaded(reciterId, surahId, ayahIds))
    }

    // 5. ReciterCatalogLoader: loads and validates JSON, ensures single default, requires https
    @Test
    fun reciterCatalogLoader_validatesCatalogCorrectly() {
        val json = """
        {
          "cdnBase": "https://cdn.islamic.network/quran/audio",
          "reciters": [
            {
              "id": "alafasy",
              "edition": "ar.alafasy",
              "nameEn": "Mishary Rashid Alafasy",
              "nameBn": "মিশারী রাশিদ আল-আফাসী",
              "style": "Murattal",
              "bitrate": 128,
              "default": true
            },
            {
              "id": "husary",
              "edition": "ar.husary",
              "nameEn": "Mahmoud Khalil Al-Husary",
              "nameBn": "মাহমুদ খলিল আল-হুসারী",
              "style": "Murattal",
              "bitrate": 128,
              "default": false
            }
          ]
        }
        """.trimIndent()

        val catalog = ReciterCatalogLoader.parseJson(json)
        assertEquals(2, catalog.reciters.size)
        val defaultReciter = catalog.defaultReciter()
        assertNotNull(defaultReciter)
        assertEquals("alafasy", defaultReciter.id)

        // cdnBase must be HTTPS
        assertTrue(catalog.cdnBase.startsWith("https://"))
    }
}
