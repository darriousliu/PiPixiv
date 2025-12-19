package com.mrl.pixiv.common.repository

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import coil3.annotation.InternalCoilApi
import coil3.util.MimeTypeMap
import com.mrl.pixiv.common.datasource.local.dao.DownloadDao
import com.mrl.pixiv.common.datasource.local.entity.DownloadEntity
import com.mrl.pixiv.common.datasource.local.entity.DownloadStatus
import com.mrl.pixiv.common.repository.util.generateFileName
import com.mrl.pixiv.common.repository.worker.DownloadWorker
import com.mrl.pixiv.common.util.PictureType
import com.mrl.pixiv.common.util.getDownloadPath
import com.mrl.pixiv.common.util.isImageExists
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import org.koin.core.annotation.Single

@Single
class DownloadManager(
    private val context: Context,
    private val downloadDao: DownloadDao,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun getAllDownloads(): Flow<List<DownloadEntity>> = downloadDao.getAllDownloads()

    fun getDownloadsByStatus(status: Int): Flow<List<DownloadEntity>> =
        downloadDao.getDownloadsByStatus(status)

    @OptIn(InternalCoilApi::class)
    suspend fun enqueueDownload(
        illustId: Long,
        index: Int,
        title: String,
        userId: Long,
        userName: String,
        thumbnailUrl: String,
        originalUrl: String,
        subFolder: String? = null,
        downloadManagerListener: DownloadManagerListener? = null
    ) {
        val existing = downloadDao.getDownload(illustId, index)
        if (existing != null && existing.status == DownloadStatus.SUCCESS.value) {
            // 双重检查，查看本地文件是否存在
            val fileName = generateFileName(illustId, title, userId, userName, index)
            val mimeType = MimeTypeMap.getMimeTypeFromUrl(originalUrl)
            val type = PictureType.fromMimeType(mimeType)
            if (type != null && isImageExists(fileName, type, subFolder)) {
                val updated = if (existing.filePath.isEmpty() || existing.fileUri.isEmpty()) {
                    val (fileUri, filePath) = getDownloadPath(fileName, type, subFolder)
                    existing.copy(filePath = filePath, fileUri = fileUri)
                } else {
                    existing
                }
                downloadDao.update(updated)
                downloadManagerListener?.onDownloadCompleted(updated)
                return
            }
        }


        val entity = existing?.copy(
            status = DownloadStatus.PENDING.value,
            progress = 0f,
            createTime = System.currentTimeMillis()
        ) ?: DownloadEntity(
            illustId = illustId,
            index = index,
            title = title,
            userId = userId,
            userName = userName,
            thumbnailUrl = thumbnailUrl,
            originalUrl = originalUrl,
            subFolder = subFolder,
            status = DownloadStatus.PENDING.value,
            progress = 0f,
            filePath = "",
            fileUri = "",
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

        if (downloadManagerListener != null) {
            scope.launch {
                workManager.getWorkInfoByIdFlow(request.id).collect { workInfo ->
                    val state = workInfo?.state
                    when (state) {
                        WorkInfo.State.SUCCEEDED -> {
                            val newEntity = downloadDao.getDownload(entity.illustId, entity.index)
                            downloadManagerListener.onDownloadCompleted(newEntity)
                        }

                        WorkInfo.State.FAILED -> {
                            downloadManagerListener.onDownloadCompleted(null)
                        }

                        else -> Unit
                    }
                    if (state != null && state.isFinished) {
                        this.cancel()
                    }
                }
            }
        }
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
            entity.userId,
            entity.userName,
            entity.thumbnailUrl,
            entity.originalUrl,
            entity.subFolder
        )
    }

    fun interface DownloadManagerListener {
        fun onDownloadCompleted(entity: DownloadEntity?)
    }
}
