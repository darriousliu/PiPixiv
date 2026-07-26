package com.mrl.pixiv.common.datasource.local.entity

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "novel_read_later",
    primaryKeys = ["novelId", "userId", "targetLanguage"],
    indices = [
        Index(value = ["userId", "state", "addedAtMillis"])
    ]
)
data class NovelReadLaterEntity(
    val novelId: Long,
    val userId: Long,
    val targetLanguage: String,
    val novelTitle: String,
    val novelCaption: String,
    val novelAuthorName: String,
    val coverUrl: String,
    val novelTagsJson: String,
    val addedAtMillis: Long,
    val provider: String,
    val model: String,
    val endpoint: String,
    val responseApi: Boolean,
    val extraBody: String,
    val configFingerprint: String,
    val sourceMd5: String,
    val state: String,
    val attemptToken: String,
    val retryCount: Int,
    val lastError: String?,
    val updatedAtMillis: Long,
)
