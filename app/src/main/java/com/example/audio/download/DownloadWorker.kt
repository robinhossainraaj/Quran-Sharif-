package com.example.audio.download

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.QuranApp
import com.example.R
import com.example.audio.catalog.ReciterCatalogLoader
import com.example.audio.net.HttpAudioDownloader
import com.example.audio.net.RecitationUnavailableException
import com.example.audio.net.UrlBuilder
import com.example.audio.storage.AudioStorage
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.io.IOException
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

class DownloadWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    override suspend fun doWork(): Result = coroutineScope {
        val reciterId = inputData.getString(KEY_RECITER_ID) ?: return@coroutineScope Result.failure(
            workDataOf(KEY_ERROR to "Missing reciter ID")
        )
        val surahId = inputData.getInt(KEY_SURAH_ID, 0)
        if (surahId <= 0 || surahId > 114) {
            return@coroutineScope Result.failure(workDataOf(KEY_ERROR to "Invalid surah ID: $surahId"))
        }

        val app = context.applicationContext as QuranApp
        val container = app.container
        val storage = AudioStorage(context)

        // Check storage before starting
        if (!storage.hasEnoughStorage()) {
            return@coroutineScope Result.failure(
                workDataOf(KEY_ERROR to context.getString(R.string.audio_error_not_enough_storage))
            )
        }

        val catalog = ReciterCatalogLoader.load(context)
        val reciter = catalog.findById(reciterId) ?: catalog.defaultReciter()

        val settings = container.settingsRepository.settingsFlow.first()
        val needsBismillah = settings.audioBismillahPrefix && (surahId != 1 && surahId != 9)

        // Fetch ayahs of surah
        val ayahs = try {
            container.database.quranDao().ayahsOfSurah(surahId).first()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load ayahs for surah $surahId", e)
            return@coroutineScope Result.failure(workDataOf(KEY_ERROR to "Failed to read surah data"))
        }

        val surah = container.database.quranDao().surah(surahId)
        val surahTitle = surah?.banglaName ?: "Surah $surahId"
        val reciterTitle = reciter.displayName(useBangla = true)

        val requiredAyahIds = mutableListOf<Int>()
        if (needsBismillah) {
            requiredAyahIds.add(1) // Global ayah 1 is Bismillah
        }
        for (ayah in ayahs) {
            if (!requiredAyahIds.contains(ayah.id)) {
                requiredAyahIds.add(ayah.id)
            }
        }

        val totalFiles = requiredAyahIds.size
        val doneCounter = AtomicInteger(0)
        val totalBytesCounter = AtomicLong(0)

        // Show foreground notification
        try {
            val notification = createNotification(
                reciterTitle = reciterTitle,
                surahTitle = surahTitle,
                done = 0,
                total = totalFiles
            )
            val foregroundInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ForegroundInfo(
                    NOTIFICATION_ID_BASE + surahId,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                )
            } else {
                ForegroundInfo(NOTIFICATION_ID_BASE + surahId, notification)
            }
            setForeground(foregroundInfo)
        } catch (e: Throwable) {
            Log.w(TAG, "Could not set foreground info (running in background without notification)", e)
        }

        // Parallel download with Semaphore(3)
        val semaphore = Semaphore(3)
        val filesToDownload = requiredAyahIds.filter { ayahId ->
            val target = storage.getAyahFile(reciter.id, ayahId)
            val isComplete = target.exists() && target.length() > 0
            if (isComplete) {
                doneCounter.incrementAndGet()
                totalBytesCounter.addAndGet(target.length())
            }
            !isComplete
        }

        setProgress(
            workDataOf(
                KEY_DONE to doneCounter.get(),
                KEY_TOTAL to totalFiles,
                KEY_BYTES to totalBytesCounter.get()
            )
        )

        var failureException: Exception? = null

        val downloadJobs = filesToDownload.map { ayahId ->
            async {
                semaphore.withPermit {
                    if (failureException != null) return@withPermit

                    val url = UrlBuilder.buildAyahUrl(
                        cdnBase = catalog.cdnBase,
                        bitrate = reciter.bitrate,
                        edition = reciter.edition,
                        ayahId = ayahId
                    )
                    val targetFile = storage.getAyahFile(reciter.id, ayahId)

                    var success = false
                    var lastError: Exception? = null

                    for (attempt in 1..5) {
                        try {
                            val bytes = HttpAudioDownloader.downloadToFile(url, targetFile)
                            totalBytesCounter.addAndGet(bytes)
                            val currentDone = doneCounter.incrementAndGet()
                            success = true

                            // Update progress
                            setProgress(
                                workDataOf(
                                    KEY_DONE to currentDone,
                                    KEY_TOTAL to totalFiles,
                                    KEY_BYTES to totalBytesCounter.get()
                                )
                            )

                            // Update notification
                            try {
                                notificationManager.notify(
                                    NOTIFICATION_ID_BASE + surahId,
                                    createNotification(reciterTitle, surahTitle, currentDone, totalFiles)
                                )
                            } catch (_: Exception) {}

                            break
                        } catch (e: RecitationUnavailableException) {
                            Log.e(TAG, "Recitation unavailable for $url", e)
                            failureException = e
                            return@withPermit
                        } catch (e: Exception) {
                            lastError = e
                            Log.w(TAG, "Download attempt $attempt failed for $url: ${e.message}")
                            if (attempt < 5) {
                                delay(attempt * 1000L)
                            }
                        }
                    }

                    if (!success && failureException == null) {
                        failureException = lastError ?: IOException("Failed after 5 attempts")
                    }
                }
            }
        }

        downloadJobs.awaitAll()

        val err = failureException
        if (err != null) {
            val errorMsg = if (err is RecitationUnavailableException) {
                context.getString(R.string.audio_error_recitation_unavailable)
            } else {
                err.message ?: context.getString(R.string.audio_error_download_failed)
            }
            return@coroutineScope if (err is RecitationUnavailableException) {
                Result.failure(workDataOf(KEY_ERROR to errorMsg))
            } else {
                if (runAttemptCount < 3) Result.retry() else Result.failure(workDataOf(KEY_ERROR to errorMsg))
            }
        }

        // Verify all required files are present and valid
        val allValid = storage.isSurahDownloaded(
            reciterId = reciter.id,
            surahId = surahId,
            ayahIds = ayahs.map { it.id },
            needsBismillah = needsBismillah
        )

        if (allValid) {
            storage.markSurahDownloaded(reciter.id, surahId)
            try {
                notificationManager.cancel(NOTIFICATION_ID_BASE + surahId)
            } catch (_: Throwable) {}
            return@coroutineScope Result.success()
        } else {
            return@coroutineScope Result.failure(
                workDataOf(KEY_ERROR to context.getString(R.string.audio_error_download_failed))
            )
        }
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        val surahId = inputData.getInt(KEY_SURAH_ID, 1)
        val reciterId = inputData.getString(KEY_RECITER_ID) ?: ""
        val notification = createNotification(
            reciterTitle = reciterId,
            surahTitle = "Surah $surahId",
            done = 0,
            total = 100
        )
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(
                NOTIFICATION_ID_BASE + surahId,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            ForegroundInfo(NOTIFICATION_ID_BASE + surahId, notification)
        }
    }

    private fun createNotification(
        reciterTitle: String,
        surahTitle: String,
        done: Int,
        total: Int
    ): Notification {
        ensureNotificationChannel()

        val cancelIntent = try {
            WorkManager.getInstance(context).createCancelPendingIntent(id)
        } catch (_: Throwable) {
            null
        }

        val contentText = "$surahTitle · $done/$total"

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.audio_downloading_format, reciterTitle))
            .setContentText(contentText)
            .setProgress(total, done, false)
            .setOngoing(true)
            .setOnlyAlertOnce(true)

        if (cancelIntent != null) {
            builder.addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                context.getString(R.string.cancel),
                cancelIntent
            )
        }

        return builder.build()
    }

    private fun ensureNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.audio_download_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = context.getString(R.string.audio_download_channel_desc)
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    companion object {
        private const val TAG = "DownloadWorker"
        const val CHANNEL_ID = "audio_download"
        const val NOTIFICATION_ID_BASE = 2000

        const val KEY_RECITER_ID = "reciter_id"
        const val KEY_SURAH_ID = "surah_id"
        const val KEY_DONE = "done"
        const val KEY_TOTAL = "total"
        const val KEY_BYTES = "bytes"
        const val KEY_ERROR = "error"

        fun uniqueWorkName(reciterId: String, surahId: Int): String {
            return "dl_${reciterId}_${surahId}"
        }
    }
}
