package com.example.data.db

/** Small row: global ayah id with its surah number and ayah number. */
data class AyahKey(
    val id: Int,
    val surahId: Int,
    val ayahNo: Int
)
