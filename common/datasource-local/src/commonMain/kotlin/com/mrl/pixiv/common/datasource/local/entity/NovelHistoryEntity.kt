package com.mrl.pixiv.common.datasource.local.entity

import androidx.room.Entity
import androidx.room.Index
import kotlinx.serialization.Serializable

@Entity(
    tableName = "browsing_history_novel",
    primaryKeys = ["novelId", "userId"],
    indices = [Index(value = ["userId", "viewedAtMillis"])]
)
@Serializable
data class NovelHistoryEntity(
    val novelId: Long,
    val userId: Long,
    val viewedAtMillis: Long,
    val novelJson: String,
)
