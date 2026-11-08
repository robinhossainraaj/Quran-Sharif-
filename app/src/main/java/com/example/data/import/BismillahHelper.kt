package com.example.data.import

object BismillahHelper {

    /**
     * Extracts display Arabic text for ayah 1 of a surah.
     * For surahs other than 1 and 9, if ayah 1 starts with the Bismillah (checked via normalized 4 tokens),
     * it strips the Bismillah prefix and returns the remainder.
     */
    fun extractDisplayArabic(
        surahId: Int,
        ayahNo: Int,
        arabicText: String,
        normalizedBismillah: String
    ): String {
        if (surahId == 1 || surahId == 9 || ayahNo != 1) {
            return arabicText
        }

        val tokens = arabicText.trim().split(Regex("\\s+"))
        if (tokens.size >= 4) {
            val firstFourTokens = tokens.take(4).joinToString(" ")
            val normalizedFirstFour = ArabicNormalizer.normalize(firstFourTokens)
            if (normalizedFirstFour == normalizedBismillah) {
                val remainder = tokens.drop(4).joinToString(" ").trim()
                if (remainder.isNotEmpty()) {
                    return remainder
                }
            }
        }
        return arabicText
    }

    /**
     * Determines whether the decorative Bismillah header should be shown in the reader
     * for a given surah.
     * True for surahs 2..114 except 9. False for surah 1 and 9.
     */
    fun shouldShowBismillahHeader(surahId: Int): Boolean {
        return surahId != 1 && surahId != 9
    }
}
