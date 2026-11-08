package com.example.audio.player

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.annotation.OptIn
import androidx.core.content.ContextCompat
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.audio.catalog.ReciterCatalogLoader
import com.example.audio.storage.AudioStorage
import com.example.data.QuranRepository
import com.example.data.prefs.SettingsRepository
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PlaybackController(
    private val context: Context,
    private val storage: AudioStorage,
    private val quranRepository: QuranRepository,
    private val settingsRepository: SettingsRepository,
    private val scope: CoroutineScope
) {
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null

    private val _state = MutableStateFlow(PlaybackUiState())
    val state: StateFlow<PlaybackUiState> = _state.asStateFlow()

    private val playerListener = object : Player.Listener {
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            updateFromMediaItem(mediaItem)
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _state.value = _state.value.copy(isPlaying = isPlaying)
            if (isPlaying) {
                startPositionTicker()
            } else {
                stopPositionTicker()
                syncPosition()
            }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            when (playbackState) {
                Player.STATE_BUFFERING -> {
                    _state.value = _state.value.copy(isBuffering = true)
                }
                Player.STATE_READY -> {
                    _state.value = _state.value.copy(isBuffering = false)
                    syncPosition()
                }
                Player.STATE_ENDED -> {
                    handlePlaybackEnded()
                }
                Player.STATE_IDLE -> {
                    _state.value = _state.value.copy(isBuffering = false)
                }
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            Log.e(TAG, "Playback error", error)
            _state.value = _state.value.copy(
                isPlaying = false,
                isBuffering = false,
                error = error.localizedMessage ?: "Audio playback error"
            )
        }
    }

    // Do not eagerly bind MediaController at application cold start.
    // Connect lazily when audio is first accessed or played.
    fun ensureController(onReady: ((MediaController) -> Unit)? = null) {
        val existing = controller
        if (existing != null) {
            onReady?.invoke(existing)
            return
        }

        if (controllerFuture != null) {
            if (onReady != null) {
                controllerFuture?.addListener({
                    try {
                        controllerFuture?.get()?.let { onReady(it) }
                    } catch (_: Throwable) {}
                }, ContextCompat.getMainExecutor(context.applicationContext))
            }
            return
        }

        try {
            val sessionToken = SessionToken(
                context.applicationContext,
                ComponentName(context.applicationContext, PlaybackService::class.java)
            )
            val future = MediaController.Builder(context.applicationContext, sessionToken).buildAsync()
            controllerFuture = future
            future.addListener({
                try {
                    val mediaController = future.get()
                    controller = mediaController
                    mediaController?.addListener(playerListener)
                    onReady?.invoke(mediaController)
                    // Restore speed from settings
                    scope.launch {
                        try {
                            val settings = settingsRepository.settingsFlow.first()
                            mediaController?.playbackParameters = PlaybackParameters(settings.audioSpeed)
                            _state.value = _state.value.copy(speed = settings.audioSpeed)
                        } catch (e: Exception) {
                            Log.w(TAG, "Could not restore speed setting", e)
                        }
                    }
                } catch (e: Throwable) {
                    Log.e(TAG, "Failed to connect to MediaController", e)
                }
            }, ContextCompat.getMainExecutor(context.applicationContext))
        } catch (e: Throwable) {
            Log.e(TAG, "Error building MediaController session token", e)
        }
    }

    suspend fun playSurah(
        surahId: Int,
        startAyahNo: Int = 1,
        reciterId: String? = null
    ): Boolean = withContext(Dispatchers.IO) {
        val catalog = ReciterCatalogLoader.load(context)
        val settings = settingsRepository.settingsFlow.first()
        val effectiveReciterId = reciterId ?: settings.audioReciterId
        val reciter = catalog.findById(effectiveReciterId) ?: catalog.defaultReciter()

        val surah = quranRepository.getSurah(surahId) ?: return@withContext false
        val ayahs = quranRepository.ayahsOfSurah(surahId).first()
        if (ayahs.isEmpty()) return@withContext false

        val needsBismillah = settings.audioBismillahPrefix && (surahId != 1 && surahId != 9)
        val isDownloaded = storage.isSurahDownloaded(
            reciterId = reciter.id,
            surahId = surahId,
            ayahIds = ayahs.map { it.id },
            needsBismillah = needsBismillah
        )

        if (!isDownloaded) {
            return@withContext false
        }

        val mediaItems = mutableListOf<MediaItem>()

        // 1. Optional Bismillah item
        if (needsBismillah) {
            val bismillahFile = storage.getAyahFile(reciter.id, 1)
            if (bismillahFile.exists()) {
                val extras = Bundle().apply {
                    putBoolean(EXTRA_IS_BISMILLAH, true)
                    putInt(EXTRA_AYAH_ID, 1)
                    putInt(EXTRA_AYAH_NO, 0)
                    putInt(EXTRA_SURAH_ID, surahId)
                    putString(EXTRA_RECITER_ID, reciter.id)
                }
                val metadata = MediaMetadata.Builder()
                    .setTitle("Bismillah")
                    .setSubtitle(surah.banglaName)
                    .setArtist(reciter.nameEn)
                    .setExtras(extras)
                    .build()

                mediaItems.add(
                    MediaItem.Builder()
                        .setMediaId("bismillah_${reciter.id}")
                        .setUri(Uri.fromFile(bismillahFile))
                        .setMediaMetadata(metadata)
                        .build()
                )
            }
        }

        // 2. Surah Ayahs in sequence
        for (ayah in ayahs) {
            val ayahFile = storage.getAyahFile(reciter.id, ayah.id)
            val extras = Bundle().apply {
                putBoolean(EXTRA_IS_BISMILLAH, false)
                putInt(EXTRA_AYAH_ID, ayah.id)
                putInt(EXTRA_AYAH_NO, ayah.ayahNo)
                putInt(EXTRA_SURAH_ID, surahId)
                putString(EXTRA_RECITER_ID, reciter.id)
            }
            val metadata = MediaMetadata.Builder()
                .setTitle("${surah.banglaName} · আয়াত ${ayah.ayahNo}")
                .setSubtitle(reciter.nameEn)
                .setArtist(reciter.nameEn)
                .setExtras(extras)
                .build()

            mediaItems.add(
                MediaItem.Builder()
                    .setMediaId("ayah_${reciter.id}_${ayah.id}")
                    .setUri(Uri.fromFile(ayahFile))
                    .setMediaMetadata(metadata)
                    .build()
            )
        }

        // Determine starting media item index
        val startIndex = if (startAyahNo <= 1) {
            0
        } else {
            // Find index of ayahNo
            val foundIdx = mediaItems.indexOfFirst {
                val extras = it.mediaMetadata.extras
                extras?.getBoolean(EXTRA_IS_BISMILLAH) == false &&
                        extras.getInt(EXTRA_AYAH_NO) == startAyahNo
            }
            if (foundIdx >= 0) foundIdx else 0
        }

        withContext(Dispatchers.Main) {
            _state.value = _state.value.copy(
                reciterId = reciter.id,
                reciterName = reciter.displayName(true),
                surahId = surahId,
                surahName = surah.banglaName,
                totalAyahsInSurah = surah.ayahCount,
                error = null
            )
            ensureController { mc ->
                mc.setMediaItems(mediaItems, startIndex, 0L)
                mc.prepare()
                mc.play()
            }
        }

        return@withContext true
    }

    // --- Time line of the CURRENT ayah (each ayah is a separate audio file) ---

    private var positionTickerJob: Job? = null

    private fun currentDurationMs(): Long {
        val d = controller?.duration ?: C.TIME_UNSET
        return if (d == C.TIME_UNSET || d < 0L) 0L else d
    }

    private fun syncPosition() {
        val mc = controller ?: return
        val pos = mc.currentPosition.coerceAtLeast(0L)
        val dur = currentDurationMs()
        val s = _state.value
        if (s.positionMs != pos || s.durationMs != dur) {
            _state.value = s.copy(positionMs = pos, durationMs = dur)
        }
    }

    private fun startPositionTicker() {
        if (positionTickerJob?.isActive == true) return
        positionTickerJob = scope.launch(Dispatchers.Main) {
            while (true) {
                syncPosition()
                delay(200)
            }
        }
    }

    private fun stopPositionTicker() {
        positionTickerJob?.cancel()
        positionTickerJob = null
    }

    /** Move inside the ayah that is playing now (the bar only covers this one ayah). */
    fun seekInAyah(positionMs: Long) {
        val mc = controller ?: return
        val dur = currentDurationMs()
        val target = if (dur > 0L) positionMs.coerceIn(0L, dur) else positionMs.coerceAtLeast(0L)
        mc.seekTo(target)
        _state.value = _state.value.copy(positionMs = target)
    }

    fun playOrPause() {
        val mc = controller ?: return
        if (mc.isPlaying) {
            mc.pause()
        } else {
            mc.play()
        }
    }

    fun pause() {
        controller?.pause()
    }

    fun stop() {
        val mc = controller ?: return
        stopPositionTicker()
        mc.stop()
        mc.clearMediaItems()
        _state.value = PlaybackUiState()
    }

    fun skipToNext() {
        controller?.seekToNextMediaItem()
    }

    fun skipToPrevious() {
        controller?.seekToPreviousMediaItem()
    }

    fun seekToAyah(ayahNo: Int) {
        val mc = controller ?: return
        val count = mc.mediaItemCount
        for (i in 0 until count) {
            val item = mc.getMediaItemAt(i)
            val extras = item.mediaMetadata.extras
            if (extras?.getBoolean(EXTRA_IS_BISMILLAH) == false &&
                extras.getInt(EXTRA_AYAH_NO) == ayahNo
            ) {
                mc.seekToDefaultPosition(i)
                mc.play()
                break
            }
        }
    }

    fun setSpeed(speed: Float) {
        val mc = controller ?: return
        mc.playbackParameters = PlaybackParameters(speed)
        _state.value = _state.value.copy(speed = speed)
        scope.launch {
            settingsRepository.setAudioSpeed(speed)
        }
    }

    fun setRepeatMode(mode: AudioRepeatMode) {
        val mc = controller ?: return
        _state.value = _state.value.copy(repeatMode = mode)
        when (mode) {
            AudioRepeatMode.OFF -> {
                mc.repeatMode = Player.REPEAT_MODE_OFF
            }
            AudioRepeatMode.AYAH -> {
                mc.repeatMode = Player.REPEAT_MODE_ONE
            }
            AudioRepeatMode.SURAH -> {
                mc.repeatMode = Player.REPEAT_MODE_ALL
            }
        }
        scope.launch {
            settingsRepository.setAudioRepeatMode(mode.name)
        }
    }

    private fun handlePlaybackEnded() {
        when (_state.value.repeatMode) {
            AudioRepeatMode.SURAH -> {
                val mc = controller ?: return
                mc.seekToDefaultPosition(0)
                mc.play()
            }
            AudioRepeatMode.AYAH -> {
                // Handled automatically by Player.REPEAT_MODE_ONE
            }
            AudioRepeatMode.OFF -> {
                scope.launch {
                    delay(800)
                    _state.value = _state.value.copy(
                        isPlaying = false,
                        isBuffering = false,
                        currentAyahId = null,
                        isBismillahItem = false
                    )
                }
            }
        }
    }

    private fun updateFromMediaItem(mediaItem: MediaItem?) {
        if (mediaItem == null) return
        val extras = mediaItem.mediaMetadata.extras ?: return

        val isBismillah = extras.getBoolean(EXTRA_IS_BISMILLAH, false)
        val ayahId = extras.getInt(EXTRA_AYAH_ID, 0)
        val ayahNo = extras.getInt(EXTRA_AYAH_NO, 1)
        val surahId = extras.getInt(EXTRA_SURAH_ID, _state.value.surahId)
        val reciterId = extras.getString(EXTRA_RECITER_ID, _state.value.reciterId)

        _state.value = _state.value.copy(
            reciterId = reciterId,
            surahId = surahId,
            currentAyahId = if (ayahId > 0) ayahId else null,
            currentAyahNo = if (ayahNo > 0) ayahNo else 1,
            isBismillahItem = isBismillah,
            isPlaying = controller?.isPlaying ?: true,
            // A new ayah file started: the time line starts again from zero.
            positionMs = 0L,
            durationMs = currentDurationMs()
        )
    }

    companion object {
        private const val TAG = "PlaybackController"
        const val EXTRA_IS_BISMILLAH = "is_bismillah"
        const val EXTRA_AYAH_ID = "ayah_id"
        const val EXTRA_AYAH_NO = "ayah_no"
        const val EXTRA_SURAH_ID = "surah_id"
        const val EXTRA_RECITER_ID = "reciter_id"
    }
}
