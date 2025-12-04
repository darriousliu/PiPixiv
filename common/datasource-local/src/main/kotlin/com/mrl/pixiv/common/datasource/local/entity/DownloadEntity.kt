package com.mrl.pixiv.common.datasource.local.entity

import androidx.compose.runtime.Stable
import androidx.room.Entity

@Entity(tableName = "download", primaryKeys = ["illustId", "index"])
@Stable
data class DownloadEntity(
    val illustId: Long,
    val index: Int,
    val title: String,
    val userId: Long,
    val userName: String,
    val thumbnailUrl: String,
    val originalUrl: String,
    val subFolder: String? = null,
    val status: Int, // 0: Pending, 1: Running, 2: Success, 3: Failed
    val progress: Float,
    val filePath: String,
    val createTime: Long,
)

enum class DownloadStatus(val value: Int) {
    PENDING(0),
    RUNNING(1),
    SUCCESS(2),
    FAILED(3),
}
