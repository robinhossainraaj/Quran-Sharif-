package com.example.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent

object ShareUtils {
    fun formatAyahText(
        arabicDisplay: String,
        bangla: String,
        surahBanglaName: String,
        surahId: Int,
        ayahNo: Int,
        useBanglaDigits: Boolean = true,
        translatorName: String? = null
    ): String {
        val ref = if (useBanglaDigits) {
            "${BanglaDigits.convert(surahId)}:${BanglaDigits.convert(ayahNo)}"
        } else {
            "$surahId:$ayahNo"
        }
        val transSuffix = if (!translatorName.isNullOrBlank()) " ($translatorName)" else ""
        return "$arabicDisplay\n\n$bangla\n\n— $surahBanglaName $ref$transSuffix\nAl Quran"
    }

    fun copyToClipboard(context: Context, label: String, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard?.setPrimaryClip(clip)
    }

    fun shareText(context: Context, text: String, title: String) {
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, text)
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, title).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(shareIntent)
    }
}
