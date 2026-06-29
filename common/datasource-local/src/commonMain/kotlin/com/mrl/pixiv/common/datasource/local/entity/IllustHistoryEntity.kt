package com.mrl.pixiv.common.datasource.local.entity

import androidx.room.Entity
import androidx.room.Index
import kotlinx.serialization.Serializable

@Entity(
    tableName = "browsing_history_illust",
    primaryKeys = ["illustId", "userId"],
    indices = [Index(value = ["userId", "viewedAtMillis"])]
)
@Serializable
data class IllustHistoryEntity(
    val illustId: Long,
    val userId: Long,
    val viewedAtMillis: Long,
    val illustJson: String,
)
