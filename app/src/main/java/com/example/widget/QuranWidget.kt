package com.example.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.example.MainActivity
import com.example.QuranApp
import com.example.R
import com.example.data.db.AyahWithText
import com.example.data.db.SurahEntity
import com.example.data.db.TranslationInfoEntity
import com.example.util.BanglaDigits

class QuranWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val app = context.applicationContext as? QuranApp
        val repository = app?.container?.quranRepository

        val translationId = WidgetPrefs.getTranslationId(context)
        val arScale = WidgetPrefs.getArabicScalePct(context)
        val trScale = WidgetPrefs.getTranslationScalePct(context)

        var currentAyahId = WidgetPrefs.getCurrentAyahId(context)
        var ayah: AyahWithText? = null
        var surah: SurahEntity? = null
        var transInfo: TranslationInfoEntity? = null

        if (repository != null) {
            transInfo = repository.translationInfo(translationId)
            if (currentAyahId > 0) {
                ayah = repository.ayahWithText(currentAyahId, translationId)
            }
            if (ayah == null) {
                val maxArabic = WidgetSizing.maxArabicLength(arScale)
                val maxTranslation = WidgetSizing.maxTranslationLength(trScale)
                val newId = repository.randomWidgetAyahIdWithText(
                    translationId,
                    maxArabic,
                    maxTranslation
                )
                if (newId != null) {
                    currentAyahId = newId
                    WidgetPrefs.setCurrentAyahId(context, newId)
                    ayah = repository.ayahWithText(newId, translationId)
                }
            }
            if (ayah != null) {
                surah = repository.getSurah(ayah.surahId)
            }
        }

        val arabicStartPx = WidgetSizing.computeArabicStartPx(arScale)
        val arabicMinPx = WidgetSizing.computeArabicMinPx(arScale)
        val translationSp = WidgetSizing.computeTranslationSp(trScale)

        val arabicBitmap = if (ayah != null) {
            ArabicBitmapRenderer.render(
                context = context,
                text = ayah.arabicDisplay,
                color = android.graphics.Color.parseColor("#F4E7BF"),
                startSizePx = arabicStartPx,
                minSizePx = arabicMinPx,
                maxHeightPx = 420
            )
        } else null

        provideContent {
            WidgetContent(
                context = context,
                ayah = ayah,
                surah = surah,
                arabicBitmap = arabicBitmap,
                translationSp = translationSp,
                translatorName = transInfo?.nameEn
            )
        }
    }

    @Composable
    private fun WidgetContent(
        context: Context,
        ayah: AyahWithText?,
        surah: SurahEntity?,
        arabicBitmap: android.graphics.Bitmap?,
        translationSp: Float,
        translatorName: String?
    ) {
        val surahIdKey = androidx.glance.action.ActionParameters.Key<Int>(MainActivity.EXTRA_SURAH_ID)
        val ayahNoKey = androidx.glance.action.ActionParameters.Key<Int>(MainActivity.EXTRA_AYAH_NO)

        val clickAction = if (ayah != null) {
            actionStartActivity<MainActivity>(
                androidx.glance.action.actionParametersOf(
                    surahIdKey to ayah.surahId,
                    ayahNoKey to ayah.ayahNo
                )
            )
        } else {
            actionStartActivity<MainActivity>()
        }

        val cardBg = Color(0xFF14352D)
        val refColor = Color(0xFF9AD3C0)
        val dividerColor = Color(0xFF2C5A4D)
        val banglaColor = Color(0xFFE3EEEA)
        val labelColor = Color(0xFF7FB8A5)

        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .cornerRadius(28.dp)
                .background(ColorProvider(cardBg))
                .padding(14.dp)
                .clickable(clickAction)
        ) {
            if (ayah == null || surah == null || arabicBitmap == null) {
                Column(
                    modifier = GlanceModifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = context.getString(R.string.app_name),
                        style = TextStyle(
                            color = ColorProvider(refColor),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(modifier = GlanceModifier.height(6.dp))
                    Text(
                        text = context.getString(R.string.widget_open_app_prompt),
                        style = TextStyle(
                            color = ColorProvider(banglaColor),
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    )
                }
            } else {
                Column(
                    modifier = GlanceModifier.fillMaxSize(),
                    verticalAlignment = Alignment.Top
                ) {
                    // Top header: Reference and refresh button
                    Row(
                        modifier = GlanceModifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val refText = "সূরা ${surah.banglaName} · ${BanglaDigits.convert(ayah.surahId)}:${BanglaDigits.convert(ayah.ayahNo)}"
                        Text(
                            text = refText,
                            style = TextStyle(
                                color = ColorProvider(refColor),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            modifier = GlanceModifier.defaultWeight()
                        )

                        // Refresh button
                        Box(
                            modifier = GlanceModifier
                                .size(28.dp)
                                .clickable(actionRunCallback<RefreshAction>()),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "⟳",
                                style = TextStyle(
                                    color = ColorProvider(refColor),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }

                    Spacer(modifier = GlanceModifier.height(4.dp))

                    // Arabic bitmap
                    Box(
                        modifier = GlanceModifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            provider = ImageProvider(arabicBitmap),
                            contentDescription = "Quran Ayah Arabic Text",
                            modifier = GlanceModifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = GlanceModifier.height(4.dp))

                    // Divider
                    Box(
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(ColorProvider(dividerColor))
                    ) {}

                    Spacer(modifier = GlanceModifier.height(4.dp))

                    // Bangla translation
                    Text(
                        text = ayah.bangla,
                        maxLines = 5,
                        style = TextStyle(
                            color = ColorProvider(banglaColor),
                            fontSize = translationSp.sp,
                            textAlign = TextAlign.Start
                        ),
                        modifier = GlanceModifier.fillMaxWidth()
                    )

                    Spacer(modifier = GlanceModifier.defaultWeight())

                    // Bottom branding label
                    Row(
                        modifier = GlanceModifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.End
                    ) {
                        val footerText = if (!translatorName.isNullOrBlank()) {
                            "QURAN SHARIF · $translatorName"
                        } else {
                            "QURAN SHARIF"
                        }
                        Text(
                            text = footerText,
                            style = TextStyle(
                                color = ColorProvider(labelColor),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }
        }
    }
}
