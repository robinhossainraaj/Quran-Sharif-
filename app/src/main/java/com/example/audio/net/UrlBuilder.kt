package com.example.audio.net

object UrlBuilder {
    /**
     * Builds the exact CDN URL for an ayah audio file.
     * Pattern: "$cdnBase/$bitrate/$edition/$ayahId.mp3"
     * where ayahId is the global ayah number (1..6236).
     */
    fun buildAyahUrl(cdnBase: String, bitrate: Int, edition: String, ayahId: Int): String {
        val cleanBase = cdnBase.trim().removeSuffix("/")
        return "$cleanBase/$bitrate/$edition/$ayahId.mp3"
    }
}
