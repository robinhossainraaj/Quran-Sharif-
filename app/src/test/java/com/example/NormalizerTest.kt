package com.example

import com.example.data.import.ArabicNormalizer
import org.junit.Assert.assertEquals
import org.junit.Test

class NormalizerTest {

    @Test
    fun testTatweelRemoval() {
        val input = "ت\u0640س\u0640ت"
        val expected = "تست"
        assertEquals(expected, ArabicNormalizer.normalize(input))
    }

    @Test
    fun testAlefMapping() {
        val input = "أحمد إبراهيم آمن ٱسم"
        val expected = "احمد ابراهيم امن اسم"
        assertEquals(expected, ArabicNormalizer.normalize(input))
    }

    @Test
    fun testYaMapping() {
        val input = "على هدى"
        val expected = "علي هدي"
        assertEquals(expected, ArabicNormalizer.normalize(input))
    }

    @Test
    fun testDiacriticsRemoval() {
        // Sample placeholder Arabic words with fatha, damma, kasra, shadda, sukun
        val input = "كَتَبَ يَكْتُبُ كِتَابًا"
        val expected = "كتب يكتب كتابا"
        assertEquals(expected, ArabicNormalizer.normalize(input))
    }

    @Test
    fun testWhitespaceTrimming() {
        val input = "  كلمة    اخري  "
        val expected = "كلمة اخري"
        assertEquals(expected, ArabicNormalizer.normalize(input))
    }
}
