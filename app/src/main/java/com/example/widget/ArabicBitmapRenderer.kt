package com.example.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.text.Layout
import android.text.StaticLayout
import android.text.TextDirectionHeuristics
import android.text.TextPaint
import androidx.core.content.res.ResourcesCompat
import com.example.R

object ArabicBitmapRenderer {

    fun render(
        context: Context,
        text: String,
        color: Int,
        widthPx: Int = 640,
        startSizePx: Float = WidgetSizing.BASE_ARABIC_START_PX,
        minSizePx: Float = WidgetSizing.BASE_ARABIC_MIN_PX,
        maxHeightPx: Int = 420
    ): Bitmap {
        val typeface = ResourcesCompat.getFont(context, R.font.amiri_quran_regular)
        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            this.typeface = typeface
            this.color = color
        }

        var size = startSizePx
        var layout: StaticLayout
        val absoluteFloor = 16f
        val effectiveMin = minSizePx.coerceAtLeast(absoluteFloor)

        while (true) {
            paint.textSize = size
            layout = StaticLayout.Builder.obtain(text, 0, text.length, paint, widthPx)
                .setAlignment(Layout.Alignment.ALIGN_CENTER)
                .setTextDirection(TextDirectionHeuristics.RTL)
                .setLineSpacing(0f, 1.35f)
                .setIncludePad(true)
                .build()
            if (layout.height <= maxHeightPx || size <= effectiveMin) break
            size -= 2f
        }

        val bmp = Bitmap.createBitmap(
            widthPx,
            layout.height.coerceAtLeast(1),
            Bitmap.Config.ARGB_8888
        )
        layout.draw(Canvas(bmp))
        return bmp
    }
}
