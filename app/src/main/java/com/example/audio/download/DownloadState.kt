package com.example.audio.download

sealed interface DownloadState {
    data object Idle : DownloadState
    data object Queued : DownloadState
    data class Running(
        val doneCount: Int,
        val totalCount: Int,
        val bytesDownloaded: Long
    ) : DownloadState {
        val progressFraction: Float
            get() = if (totalCount > 0) (doneCount.toFloat() / totalCount.toFloat()).coerceIn(0f, 1f) else 0f
    }
    data class Failed(val reason: String) : DownloadState
    data object Done : DownloadState
}
