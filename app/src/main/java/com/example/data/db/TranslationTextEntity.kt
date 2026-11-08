package com.example.data.db

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "translation_text",
    primaryKeys = ["translationId", "ayahId"],
    indices = [Index(value = ["ayahId"]), Index(value = ["translationId"])]
)
data class TranslationTextEntity(
    val translationId: String,
    val ayahId: Int,
    val text: String
)
