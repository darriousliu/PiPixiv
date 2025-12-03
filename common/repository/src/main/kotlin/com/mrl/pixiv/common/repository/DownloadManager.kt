package com.mrl.pixiv.common.repository

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.mrl.pixiv.common.datasource.local.dao.DownloadDao
import com.mrl.pixiv.common.datasource.local.entity.DownloadEntity
import com.mrl.pixiv.common.datasource.local.entity.DownloadStatus
import com.mrl.pixiv.common.repository.worker.DownloadWorker
import kotlinx.coroutines.flow.Flow
import org.koin.core.annotation.Single

@Single
class DownloadManager(
    private val context: Context,
    private val downloadDao: DownloadDao,
) {
    fun getAllDownloads(): Flow<List<DownloadEntity>> = downloadDao.getAllDownloads()

    fun getDownloadsByStatus(status: Int): Flow<List<DownloadEntity>> =
        downloadDao.getDownloadsByStatus(status)

    suspend fun enqueueDownload(
        illustId: Long,
        index: Int,
        title: String,
        artist: String,
        thumbnailUrl: String,
        originalUrl: String,
        subFolder: String? = null,
    ) {
        val existing = downloadDao.getDownload(illustId, index)
        if (existing != null && existing.status == DownloadStatus.SUCCESS.value) {
            return
        }

        val entity = existing?.copy(
            status = DownloadStatus.PENDING.value,
            progress = 0f,
            createTime = System.currentTimeMillis()
        ) ?: DownloadEntity(
            illustId = illustId,
            index = index,
            title = title,
            artist = artist,
            thumbnailUrl = thumbnailUrl,
            originalUrl = originalUrl,
            subFolder = subFolder,
            status = DownloadStatus.PENDING.value,
            progress = 0f,
            filePath = "",
            createTime = System.currentTimeMillis()
        )
        downloadDao.insert(entity)

        val workManager = WorkManager.getInstance(context)
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val inputData = workDataOf(
            "illustId" to illustId,
            "index" to index,
            "url" to originalUrl,
            "subFolder" to subFolder
        )

        val request = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setConstraints(constraints)
            .setInputData(inputData)
            .addTag("download_${illustId}_${index}")
            .build()

        workManager.enqueueUniqueWork(
            "download_${illustId}_${index}",
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    suspend fun deleteDownload(entity: DownloadEntity) {
        WorkManager.getInstance(context)
            .cancelUniqueWork("download_${entity.illustId}_${entity.index}")
        downloadDao.delete(entity)
    }

    suspend fun deleteAllDownloads() {
        WorkManager.getInstance(context).cancelAllWork()
        downloadDao.deleteAll()
    }

    suspend fun retryDownload(entity: DownloadEntity) {
        enqueueDownload(
            entity.illustId,
            entity.index,
            entity.title,
            entity.artist,
            entity.thumbnailUrl,
            entity.originalUrl,
            entity.subFolder
        )
    }
}
