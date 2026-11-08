package com.example.data.db

data class AyahWithText(
    val id: Int,
    val surahId: Int,
    val ayahNo: Int,
    val arabicOriginal: String,
    val arabicDisplay: String,
    val arabicSearch: String,
    val bangla: String
)
