package com.example.widget

object WidgetSizing {
    const val MIN_SCALE = 70
    const val MAX_SCALE = 150
    const val DEFAULT_SCALE = 100

    const val BASE_ARABIC_START_PX = 46f
    const val BASE_ARABIC_MIN_PX = 24f
    const val BASE_TRANSLATION_SP = 14f

    fun computeArabicStartPx(scalePct: Int): Float =
        (BASE_ARABIC_START_PX * scalePct / 100f).coerceIn(20f, 90f)

    fun computeArabicMinPx(scalePct: Int): Float =
        (BASE_ARABIC_MIN_PX * scalePct / 100f).coerceAtLeast(16f)

    fun computeTranslationSp(scalePct: Int): Float =
        (BASE_TRANSLATION_SP * scalePct / 100f).coerceIn(9f, 26f)

    fun maxArabicLength(scalePct: Int): Int =
        (180 * 100f / scalePct).toInt().coerceAtLeast(60)

    fun maxTranslationLength(scalePct: Int): Int =
        (260 * 100f / scalePct).toInt().coerceAtLeast(80)
}
