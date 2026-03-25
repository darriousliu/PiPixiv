package com.mrl.pixiv.common.datasource.local.entity

import androidx.room.Entity
import kotlinx.serialization.Serializable

@Entity(tableName = "block_tag", primaryKeys = ["tag"])
@Serializable
data class BlockTagEntity(
    val tag: String,
    val isRegex: Boolean = false,
)
