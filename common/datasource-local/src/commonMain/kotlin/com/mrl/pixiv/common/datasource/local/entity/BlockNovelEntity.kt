package com.mrl.pixiv.common.datasource.local.entity

import androidx.room.Entity
import kotlinx.serialization.Serializable

@Entity(tableName = "block_novel", primaryKeys = ["novelId"])
@Serializable
data class BlockNovelEntity(
    val novelId: Long,
    val title: String = "",
)
