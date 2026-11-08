package com.example.util

object BanglaDigits {
    private val englishDigits = charArrayOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9')
    private val banglaDigits = charArrayOf('০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯')

    fun convert(input: String): String {
        val sb = java.lang.StringBuilder(input.length)
        for (char in input) {
            val idx = englishDigits.indexOf(char)
            if (idx >= 0) {
                sb.append(banglaDigits[idx])
            } else {
                sb.append(char)
            }
        }
        return sb.toString()
    }

    fun convert(number: Int): String = convert(number.toString())

    fun toAscii(input: String): String {
        val sb = java.lang.StringBuilder(input.length)
        for (char in input) {
            val idx = banglaDigits.indexOf(char)
            if (idx >= 0) {
                sb.append(englishDigits[idx])
            } else {
                sb.append(char)
            }
        }
        return sb.toString()
    }
}
