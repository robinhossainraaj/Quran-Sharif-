package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "juz")
data class JuzEntity(
    @PrimaryKey val index: Int,
    val startSurah: Int,
    val startAyah: Int,
    val startAyahId: Int
)
