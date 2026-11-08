package com.example.audio.net

object Mp3Validator {
    /**
     * Checks if the given bytes begin with a valid MP3 identifier:
     * - "ID3" tag (header starts with bytes 0x49, 0x44, 0x33), OR
     * - MPEG frame synchronization header:
     *   byte 0 is 0xFF, and byte 1 has its 3 most significant bits set (0b11100000 = 0xE0).
     *
     * Explicitly rejects HTML (e.g., "<!DOCTYPE"), plain text, or empty arrays.
     */
    fun isValidMp3Header(headerBytes: ByteArray): Boolean {
        if (headerBytes.size < 2) return false

        // Check for ID3v2 container header
        if (headerBytes.size >= 3) {
            if (headerBytes[0] == 0x49.toByte() &&   // 'I'
                headerBytes[1] == 0x44.toByte() &&   // 'D'
                headerBytes[2] == 0x33.toByte()      // '3'
            ) {
                return true
            }
        }

        // Check for MPEG audio frame synchronization
        val b0 = headerBytes[0].toInt() and 0xFF
        val b1 = headerBytes[1].toInt() and 0xFF

        val isSyncWord = (b0 == 0xFF) && ((b1 and 0xE0) == 0xE0)
        if (isSyncWord) {
            // Also verify layer bits (bits 1 and 2 of byte 1 cannot be '00', which is reserved)
            val layerBits = (b1 ushr 1) and 0x03
            return layerBits != 0
        }

        return false
    }
}
