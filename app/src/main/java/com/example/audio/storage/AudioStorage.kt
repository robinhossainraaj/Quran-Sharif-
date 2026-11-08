package com.example.audio.storage

import android.content.Context
import android.os.StatFs
import android.util.Log
import java.io.File

class AudioStorage(private val context: Context) {

    val baseDir: File by lazy {
        val external = context.getExternalFilesDir("audio")
        val dir = external ?: File(context.filesDir, "audio")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        dir
    }

    fun getReciterDir(reciterId: String): File {
        val dir = File(baseDir, reciterId)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    fun getAyahFile(reciterId: String, ayahId: Int): File {
        return File(getReciterDir(reciterId), "$ayahId.mp3")
    }

    fun getDoneMarkerFile(reciterId: String, surahId: Int): File {
        return File(getReciterDir(reciterId), "surah_${surahId}.done")
    }

    /**
     * A surah is considered downloaded if and only if:
     * 1. The marker file surah_{surahId}.done exists, AND
     * 2. Every required ayah file in the surah exists with size > 0, AND
     * 3. The Bismillah file (ayah 1) exists with size > 0 if [needsBismillah] is true.
     */
    fun isSurahDownloaded(
        reciterId: String,
        surahId: Int,
        ayahIds: List<Int>,
        needsBismillah: Boolean = false
    ): Boolean {
        val marker = getDoneMarkerFile(reciterId, surahId)
        if (!marker.exists()) return false

        if (ayahIds.isEmpty()) return false

        for (id in ayahIds) {
            val file = getAyahFile(reciterId, id)
            if (!file.exists() || file.length() == 0L) {
                return false
            }
        }

        if (needsBismillah) {
            val bismillahFile = getAyahFile(reciterId, 1)
            if (!bismillahFile.exists() || bismillahFile.length() == 0L) {
                return false
            }
        }

        return true
    }

    /**
     * Returns the set of surah IDs that have a valid .done marker for this reciter.
     */
    fun downloadedSurahIds(reciterId: String): Set<Int> {
        val reciterDir = File(baseDir, reciterId)
        if (!reciterDir.exists() || !reciterDir.isDirectory) return emptySet()

        val results = mutableSetOf<Int>()
        val markerFiles = reciterDir.listFiles { _, name ->
            name.startsWith("surah_") && name.endsWith(".done")
        } ?: emptyArray()

        for (file in markerFiles) {
            // surah_12.done -> 12
            val surahIdStr = file.name.removePrefix("surah_").removeSuffix(".done")
            surahIdStr.toIntOrNull()?.let { results.add(it) }
        }
        return results
    }

    fun markSurahDownloaded(reciterId: String, surahId: Int) {
        val marker = getDoneMarkerFile(reciterId, surahId)
        try {
            if (!marker.exists()) {
                marker.createNewFile()
            }
        } catch (e: Exception) {
            Log.e("AudioStorage", "Failed to create done marker for surah $surahId", e)
        }
    }

    /**
     * Calculates the disk size in bytes for a specific surah.
     */
    fun surahSizeBytes(reciterId: String, ayahIds: List<Int>): Long {
        var total = 0L
        for (id in ayahIds) {
            val f = getAyahFile(reciterId, id)
            if (f.exists()) {
                total += f.length()
            }
        }
        return total
    }

    /**
     * Calculates the disk size in bytes for all audio of a reciter.
     */
    fun reciterSizeBytes(reciterId: String): Long {
        val dir = File(baseDir, reciterId)
        if (!dir.exists()) return 0L
        return dir.walkTopDown().filter { it.isFile && it.extension == "mp3" }.map { it.length() }.sum()
    }

    /**
     * Total storage used across all reciters in bytes.
     */
    fun totalSizeBytes(): Long {
        if (!baseDir.exists()) return 0L
        return baseDir.walkTopDown().filter { it.isFile && it.extension == "mp3" }.map { it.length() }.sum()
    }

    /**
     * Deletes a downloaded surah for a reciter.
     * Removes the .done marker and the ayah mp3 files (sparing ayah 1 if it is still needed by other surahs).
     */
    fun deleteSurah(reciterId: String, surahId: Int, ayahIds: List<Int>): Boolean {
        var deletedAny = false
        val marker = getDoneMarkerFile(reciterId, surahId)
        if (marker.exists()) {
            marker.delete()
            deletedAny = true
        }

        val otherDownloadedSurahs = downloadedSurahIds(reciterId)
        val canDeleteAyah1 = otherDownloadedSurahs.isEmpty() || (otherDownloadedSurahs == setOf(1))

        for (id in ayahIds) {
            if (id == 1 && !canDeleteAyah1 && surahId != 1) {
                // Keep shared Bismillah if other surahs remain
                continue
            }
            val f = getAyahFile(reciterId, id)
            if (f.exists()) {
                f.delete()
                deletedAny = true
            }
        }
        return deletedAny
    }

    /**
     * Deletes all downloaded audio for a reciter.
     */
    fun deleteReciter(reciterId: String): Boolean {
        val dir = File(baseDir, reciterId)
        if (!dir.exists()) return false
        return dir.deleteRecursively()
    }

    /**
     * Returns available storage in bytes on the volume where audio files are stored.
     */
    fun getAvailableStorageBytes(): Long {
        return try {
            val stat = StatFs(baseDir.path)
            stat.availableBytes
        } catch (e: Exception) {
            Long.MAX_VALUE
        }
    }

    /**
     * Checks if at least 150 MB of free storage is available.
     */
    fun hasEnoughStorage(): Boolean {
        return getAvailableStorageBytes() >= MIN_FREE_SPACE_BYTES
    }

    /**
     * Cleans up any leftover .part files that may have been abandoned during unexpected termination.
     */
    fun cleanPartFiles() {
        try {
            if (baseDir.exists()) {
                baseDir.walkTopDown()
                    .filter { it.isFile && it.name.endsWith(".part") }
                    .forEach { it.delete() }
            }
        } catch (e: Exception) {
            Log.w("AudioStorage", "Failed to clean .part files", e)
        }
    }

    companion object {
        const val MIN_FREE_SPACE_BYTES = 150L * 1024L * 1024L // 150 MB
    }
}
