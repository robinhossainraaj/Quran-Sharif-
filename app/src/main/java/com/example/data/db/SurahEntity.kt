package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "surah")
data class SurahEntity(
    @PrimaryKey val id: Int,            // 1..114
    val arabicName: String,             // from xml "name"
    val transliteration: String,        // xml "tname"
    val englishMeaning: String,         // xml "ename"
    val banglaName: String,             // from res string-array (Appendix A), index id-1
    val revelationType: String,         // "Meccan" | "Medinan"
    val revelationOrder: Int,
    val ayahCount: Int
)
