package com.mrl.pixiv.common.repository

import com.mrl.pixiv.common.util.PictureType
import kotlinx.coroutines.flow.Flow

interface DownloadStrategy {
    val downloadFolder: String

    suspend fun enqueue(
        illustId: Long,
        index: Int,
        url: String,
        subFolder: String?
    )

    suspend fun cancel(illustId: Long, index: Int)

    suspend fun cancelAll()

    fun getDownloadState(illustId: Long, index: Int): Flow<DownloadState>

    suspend fun getExistingFileInfo(
        illustId: Long,
        index: Int,
        fileName: String,
        type: PictureType,
        subFolder: String?
    ): Pair<String, String>?
}

enum class DownloadState {
    PENDING,
    RUNNING,
    SUCCEEDED,
    FAILED,
    UNKNOWN
}
