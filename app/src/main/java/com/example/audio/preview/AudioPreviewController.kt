package com.example.audio.preview

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.example.audio.catalog.Reciter
import com.example.audio.net.UrlBuilder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed interface PreviewState {
    data object Idle : PreviewState
    data class Buffering(val reciterId: String) : PreviewState
    data class Playing(val reciterId: String, val currentAyahIndex: Int) : PreviewState
    data class Error(val reciterId: String, val message: String) : PreviewState
}

class AudioPreviewController(private val context: Context, private val cdnBase: String) {

    private var player: ExoPlayer? = null
    private var activeReciterId: String? = null

    private val _previewState = MutableStateFlow<PreviewState>(PreviewState.Idle)
    val previewState: StateFlow<PreviewState> = _previewState.asStateFlow()

    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            val reciterId = activeReciterId ?: return
            when (playbackState) {
                Player.STATE_BUFFERING -> {
                    _previewState.value = PreviewState.Buffering(reciterId)
                }
                Player.STATE_READY -> {
                    val index = player?.currentMediaItemIndex ?: 0
                    _previewState.value = PreviewState.Playing(reciterId, index)
                }
                Player.STATE_ENDED -> {
                    stopPreview()
                }
                Player.STATE_IDLE -> {
                    // Handled in stop
                }
            }
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            val reciterId = activeReciterId ?: return
            val index = player?.currentMediaItemIndex ?: 0
            if (player?.isPlaying == true) {
                _previewState.value = PreviewState.Playing(reciterId, index)
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            val reciterId = activeReciterId ?: ""
            Log.e("AudioPreviewController", "Preview player error for $reciterId", error)
            _previewState.value = PreviewState.Error(
                reciterId,
                error.localizedMessage ?: "Failed to stream audio preview"
            )
            releasePlayer()
        }
    }

    @OptIn(UnstableApi::class)
    fun playPreview(reciter: Reciter) {
        if (activeReciterId == reciter.id && _previewState.value !is PreviewState.Idle && _previewState.value !is PreviewState.Error) {
            // Tapping again stops preview
            stopPreview()
            return
        }

        stopPreview()
        activeReciterId = reciter.id
        _previewState.value = PreviewState.Buffering(reciter.id)

        val exo = ExoPlayer.Builder(context).build().apply {
            addListener(playerListener)
            playWhenReady = true
        }
        player = exo

        val mediaItems = PREVIEW_AYAH_IDS.map { ayahId ->
            val url = UrlBuilder.buildAyahUrl(
                cdnBase = cdnBase,
                bitrate = reciter.bitrate,
                edition = reciter.edition,
                ayahId = ayahId
            )
            MediaItem.fromUri(Uri.parse(url))
        }

        exo.setMediaItems(mediaItems)
        exo.prepare()
    }

    fun stopPreview() {
        activeReciterId = null
        releasePlayer()
        _previewState.value = PreviewState.Idle
    }

    private fun releasePlayer() {
        player?.removeListener(playerListener)
        player?.stop()
        player?.release()
        player = null
    }

    companion object {
        // Al-Fatiha ayahs 1, 2, 3
        val PREVIEW_AYAH_IDS = listOf(1, 2, 3)
    }
}
