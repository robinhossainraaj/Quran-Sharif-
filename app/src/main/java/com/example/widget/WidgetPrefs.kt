package com.example.widget

import android.content.Context
import android.content.SharedPreferences

object WidgetPrefs {
    private const val PREFS_NAME = "quran_widget_prefs"
    private const val KEY_WIDGET_AYAH_ID = "widget_ayah_id"
    private const val KEY_TRANSLATION_ID = "widget_translation_id"
    private const val KEY_ARABIC_SCALE = "widget_arabic_scale_pct"
    private const val KEY_TRANSLATION_SCALE = "widget_translation_scale_pct"

    const val DEFAULT_TRANSLATION_ID = "bn_muhiuddin"
    const val DEFAULT_SCALE = 100

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getCurrentAyahId(context: Context): Int {
        return getPrefs(context).getInt(KEY_WIDGET_AYAH_ID, -1)
    }

    fun setCurrentAyahId(context: Context, ayahId: Int) {
        getPrefs(context).edit().putInt(KEY_WIDGET_AYAH_ID, ayahId).commit()
    }

    fun getTranslationId(context: Context): String {
        return getPrefs(context).getString(KEY_TRANSLATION_ID, DEFAULT_TRANSLATION_ID) ?: DEFAULT_TRANSLATION_ID
    }

    fun setTranslationId(context: Context, translationId: String) {
        getPrefs(context).edit().putString(KEY_TRANSLATION_ID, translationId).commit()
    }

    fun getArabicScalePct(context: Context): Int {
        return getPrefs(context).getInt(KEY_ARABIC_SCALE, DEFAULT_SCALE)
    }

    fun setArabicScalePct(context: Context, scalePct: Int) {
        getPrefs(context).edit().putInt(KEY_ARABIC_SCALE, scalePct).commit()
    }

    fun getTranslationScalePct(context: Context): Int {
        return getPrefs(context).getInt(KEY_TRANSLATION_SCALE, DEFAULT_SCALE)
    }

    fun setTranslationScalePct(context: Context, scalePct: Int) {
        getPrefs(context).edit().putInt(KEY_TRANSLATION_SCALE, scalePct).commit()
    }
}
