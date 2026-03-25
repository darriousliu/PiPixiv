package com.mrl.pixiv.common.datasource.local.entity

import androidx.room.Entity
import kotlinx.serialization.Serializable

@Entity(tableName = "block_user", primaryKeys = ["userId"])
@Serializable
data class BlockUserEntity(
    val userId: Long,
    val name: String = "",
)
