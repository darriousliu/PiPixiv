package com.mrl.pixiv.common.datasource.local.entity

import androidx.room.Entity
import kotlinx.serialization.Serializable

@Entity(
    tableName = "novel_translation",
    primaryKeys = ["novelId", "userId", "targetLanguage"]
)
@Serializable
data class NovelTranslationEntity(
    val novelId: Long,
    val userId: Long,
    val targetLanguage: String,
    val provider: String,
    val model: String,
    val sourceMd5: String,
    val translatedText: String,
    val updatedAtMillis: Long,
)
