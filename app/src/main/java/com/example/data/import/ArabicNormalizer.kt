package com.example.data.import

object ArabicNormalizer {
    /**
     * Normalizes Arabic text according to Section 5.2 of the specification:
     * Removes:
     * - U+0640 (tatweel)
     * - U+0610..U+061A
     * - U+064B..U+065F (harakat / diacritics)
     * - U+0670 (superscript alef)
     * - U+06D6..U+06DC (Quranic annotation signs)
     * - U+06DF..U+06E8
     * - U+06EA..U+06ED
     * - U+08D3..U+08FF
     *
     * Maps:
     * - 'أ', 'إ', 'آ', 'ٱ' -> 'ا'
     * - 'ى' -> 'ي'
     * - Trims leading and trailing whitespace and collapses multiple spaces.
     */
    fun normalize(text: String): String {
        val sb = java.lang.StringBuilder(text.length)
        for (char in text) {
            val code = char.code
            // Check ranges to remove
            if (code == 0x0640) continue
            if (code in 0x0610..0x061A) continue
            if (code in 0x064B..0x065F) continue
            if (code == 0x0670) continue
            if (code in 0x06D6..0x06DC) continue
            if (code in 0x06DF..0x06E8) continue
            if (code in 0x06EA..0x06ED) continue
            if (code in 0x08D3..0x08FF) continue

            // Character mapping
            when (char) {
                'أ', 'إ', 'آ', 'ٱ' -> sb.append('ا')
                'ى' -> sb.append('ي')
                else -> sb.append(char)
            }
        }
        return sb.toString().trim().replace(Regex("\\s+"), " ")
    }
}
