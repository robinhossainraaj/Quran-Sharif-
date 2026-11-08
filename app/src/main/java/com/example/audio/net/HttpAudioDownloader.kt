package com.example.audio.net

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlin.coroutines.coroutineContext

class RecitationUnavailableException(message: String) : IOException(message)

object HttpAudioDownloader {
    private const val CONNECT_TIMEOUT_MS = 15_000
    private const val READ_TIMEOUT_MS = 30_000
    private const val MIN_FILE_SIZE_BYTES = 2048L

    /**
     * Downloads an MP3 file from [urlString] and saves it to [targetFile].
     * Downloads to a temporary .part file first, validates HTTP 200, content length,
     * minimum file size (>=2048 bytes), and valid MP3 header before renaming.
     *
     * @param onBytesRead Callback for tracking download progress (bytes written for this file).
     */
    suspend fun downloadToFile(
        urlString: String,
        targetFile: File,
        onBytesRead: ((bytesDownloaded: Long) -> Unit)? = null
    ): Long = withContext(Dispatchers.IO) {
        val partFile = File(targetFile.parentFile, "${targetFile.name}.part")
        targetFile.parentFile?.mkdirs()

        if (partFile.exists()) {
            partFile.delete()
        }

        var connection: HttpURLConnection? = null
        try {
            val url = URL(urlString)
            connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                instanceFollowRedirects = true
                requestMethod = "GET"
                setRequestProperty("User-Agent", "QuranSharif-Android")
            }

            val responseCode = connection.responseCode
            if (responseCode == 404 || responseCode == 410) {
                throw RecitationUnavailableException("This recitation is not available (HTTP $responseCode)")
            }
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw IOException("Server returned HTTP $responseCode: ${connection.responseMessage}")
            }

            val contentLength = connection.contentLengthLong

            var bytesWritten = 0L
            connection.inputStream.use { input ->
                FileOutputStream(partFile).use { output ->
                    val buffer = ByteArray(8192)
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        if (!coroutineContext.isActive) {
                            throw IOException("Download cancelled")
                        }
                        output.write(buffer, 0, read)
                        bytesWritten += read
                        onBytesRead?.invoke(bytesWritten)
                    }
                    output.flush()
                }
            }

            // Validation 1: Content-Length check
            if (contentLength > 0 && bytesWritten != contentLength) {
                throw IOException("Incomplete download: expected $contentLength bytes but got $bytesWritten")
            }

            // Validation 2: Minimum size check
            if (bytesWritten < MIN_FILE_SIZE_BYTES) {
                throw IOException("Downloaded file too small: $bytesWritten bytes (< $MIN_FILE_SIZE_BYTES bytes)")
            }

            // Validation 3: MP3 header check
            val headerBytes = ByteArray(16)
            val headerLength = FileInputStream(partFile).use { it.read(headerBytes) }
            if (headerLength < 2 || !Mp3Validator.isValidMp3Header(headerBytes.copyOf(headerLength))) {
                throw IOException("Downloaded file is not a valid MP3 file (invalid audio header)")
            }

            // Validation succeeded -> Atomic rename to target
            if (targetFile.exists()) {
                targetFile.delete()
            }
            val renamed = partFile.renameTo(targetFile)
            if (!renamed) {
                // Fallback copy if renameTo fails across file systems
                partFile.copyTo(targetFile, overwrite = true)
                partFile.delete()
            }

            return@withContext bytesWritten
        } catch (e: Exception) {
            if (partFile.exists()) {
                partFile.delete()
            }
            throw e
        } finally {
            connection?.disconnect()
        }
    }
}
