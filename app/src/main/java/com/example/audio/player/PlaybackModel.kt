package com.example.audio.player

enum class AudioRepeatMode {
    OFF,
    AYAH,
    SURAH;

    fun next(): AudioRepeatMode {
        return when (this) {
            OFF -> AYAH
            AYAH -> SURAH
            SURAH -> OFF
        }
    }
}

data class PlaybackUiState(
    val reciterId: String = "",
    val reciterName: String = "",
    val surahId: Int = 0,
    val surahName: String = "",
    val currentAyahId: Int? = null,
    val currentAyahNo: Int = 1,
    val totalAyahsInSurah: Int = 0,
    val isBismillahItem: Boolean = false,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val speed: Float = 1.0f,
    val repeatMode: AudioRepeatMode = AudioRepeatMode.OFF,
    // Position and length of the CURRENT ayah audio only. Every ayah is its own file,
    // so these reset to 0 each time the next ayah starts.
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val error: String? = null
) {
    val isActive: Boolean
        get() = surahId > 0 && (isPlaying || isBuffering || currentAyahId != null)
}
