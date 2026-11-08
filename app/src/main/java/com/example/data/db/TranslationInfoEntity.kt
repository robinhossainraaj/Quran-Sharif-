package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "translation_info")
data class TranslationInfoEntity(
    @PrimaryKey val id: String,
    val nameEn: String,
    val nameBn: String,
    val translator: String,
    val source: String,
    val version: String,
    val licenseNote: String,
    val licenseText: String,
    val ayahCount: Int,
    val isDefault: Boolean = false
)
