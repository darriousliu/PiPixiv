package com.mrl.pixiv.common.datasource.local.entity

import androidx.room.Entity
import kotlinx.serialization.Serializable

@Entity(tableName = "block_comment", primaryKeys = ["commentId"])
@Serializable
data class BlockCommentEntity(
    val commentId: Long,
    val commentJson: String,
)
