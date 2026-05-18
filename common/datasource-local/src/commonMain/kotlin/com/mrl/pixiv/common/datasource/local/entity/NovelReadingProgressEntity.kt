package com.mrl.pixiv.common.datasource.local.entity

import androidx.room.Entity
import kotlinx.serialization.Serializable

@Entity(
    tableName = "novel_reading_progress",
    primaryKeys = ["novelId", "userId"]
)
@Serializable
data class NovelReadingProgressEntity(
    val novelId: Long,
    val userId: Long,
    val paragraphIndex: Int,
    val charIndex: Int,
    val paragraphHash: Int,
    val updatedAtMillis: Long,
)
