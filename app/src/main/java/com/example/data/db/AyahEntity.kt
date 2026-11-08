package com.example.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "ayah",
    indices = [Index(value = ["surahId", "ayahNo"], unique = true)]
)
data class AyahEntity(
    @PrimaryKey val id: Int,            // 1..6236 global
    val surahId: Int,
    val ayahNo: Int,
    val arabicOriginal: String,         // exact Tanzil Uthmani line text
    val arabicDisplay: String,          // same, minus leading Bismillah where applicable
    val arabicSearch: String,           // normalize(quran-simple-clean text)
    val bangla: String                  // exact translation text
)
