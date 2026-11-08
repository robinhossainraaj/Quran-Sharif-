package com.example.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.updateAll
import com.example.QuranApp

object WidgetUpdater {

    suspend fun refresh(context: Context) {
        try {
            val app = context.applicationContext as? QuranApp ?: return
            val repository = app.container.quranRepository
            val currentId = WidgetPrefs.getCurrentAyahId(context)

            val translationId = WidgetPrefs.getTranslationId(context)
            val arScale = WidgetPrefs.getArabicScalePct(context)
            val trScale = WidgetPrefs.getTranslationScalePct(context)

            val maxArabic = WidgetSizing.maxArabicLength(arScale)
            val maxTranslation = WidgetSizing.maxTranslationLength(trScale)

            var newId: Int? = null
            for (attempt in 0 until 3) {
                val candidate = repository.randomWidgetAyahIdWithText(translationId, maxArabic, maxTranslation)
                if (candidate != null && candidate != currentId) {
                    newId = candidate
                    break
                } else if (candidate != null && newId == null) {
                    newId = candidate
                }
            }

            if (newId != null) {
                WidgetPrefs.setCurrentAyahId(context, newId)
                QuranWidget().updateAll(context)
            }
        } catch (e: Exception) {
            // Safe fallback: never crash in widget update
        }
    }

    suspend fun rerender(context: Context) {
        try {
            val manager = GlanceAppWidgetManager(context)
            val glanceIds = manager.getGlanceIds(QuranWidget::class.java)
            if (glanceIds.isNotEmpty()) {
                QuranWidget().updateAll(context)
            }
        } catch (e: Exception) {
            // Ignore error
        }
    }

    suspend fun refreshIfWidgetPresent(context: Context) {
        try {
            val manager = GlanceAppWidgetManager(context)
            val glanceIds = manager.getGlanceIds(QuranWidget::class.java)
            if (glanceIds.isNotEmpty()) {
                refresh(context)
            }
        } catch (e: Exception) {
            // Ignore error
        }
    }
}
