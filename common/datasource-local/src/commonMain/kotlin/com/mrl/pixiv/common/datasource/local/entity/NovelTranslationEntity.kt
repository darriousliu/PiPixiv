package com.mrl.pixiv.common.datasource.local.entity

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import kotlinx.serialization.Serializable

@Entity(
    tableName = "novel_translation",
    primaryKeys = ["novelId", "targetLanguage"]
)
@Serializable
data class NovelTranslationEntity(
    val novelId: Long,
    val targetLanguage: String,
    val provider: String,
    val model: String,
    val configFingerprint: String,
    val sourceMd5: String,
    val translatedText: String,
    val updatedAtMillis: Long,
    @ColumnInfo(defaultValue = "''")
    val translatedTitle: String = "",
    @ColumnInfo(defaultValue = "''")
    val translatedCaption: String = "",
    @ColumnInfo(defaultValue = "''")
    val metadataSourceMd5: String = "",
)
