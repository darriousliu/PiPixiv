package com.mrl.pixiv.common.repository

import coil3.annotation.InternalCoilApi
import coil3.util.MimeTypeMap
import com.mrl.pixiv.common.datasource.local.dao.DownloadDao
import com.mrl.pixiv.common.datasource.local.entity.DownloadEntity
import com.mrl.pixiv.common.datasource.local.entity.DownloadStatus
import com.mrl.pixiv.common.repository.util.generateFileName
import com.mrl.pixiv.common.util.PictureType
import com.mrl.pixiv.common.util.currentTimeMillis
import com.mrl.pixiv.common.util.isImageExists
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.koin.core.annotation.Single

@OptIn(InternalCoilApi::class)
@Single
class DownloadManager(
    private val downloadDao: DownloadDao,
    private val downloadStrategy: DownloadStrategy
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val downloadChanges = Mutex()

    fun getAllDownloads(): Flow<List<DownloadEntity>> = downloadDao.getAllDownloads()

    fun getDownloadsByStatus(status: Int): Flow<List<DownloadEntity>> =
        downloadDao.getDownloadsByStatus(status)

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
    ): Boolean {
        if (!downloadStrategy.prepareDownload()) {
            downloadManagerListener?.onDownloadCompleted(null)
            return false
        }
        val existingDownload = downloadChanges.withLock {
            val existing = downloadDao.getDownload(illustId, index)
            if (existing != null && existing.status == DownloadStatus.SUCCESS.value) {
                val fileName = generateFileName(illustId, title, userId, userName, index)

                val mimeType = MimeTypeMap.getMimeTypeFromUrl(originalUrl)
                val type = PictureType.fromMimeType(mimeType)
                if (type != null && isImageExists(fileName, type, subFolder, existing.fileUri)) {
                    val fileInfo =
                        downloadStrategy.getExistingFileInfo(illustId, index, fileName, type, subFolder)
                    if (fileInfo != null) {
                        val (fileUri, filePath) = fileInfo
                        val updated = if (existing.filePath.isEmpty() || existing.fileUri.isEmpty()) {
                            existing.copy(filePath = filePath, fileUri = fileUri)
                        } else {
                            existing
                        }
                        downloadDao.update(updated)
                        return@withLock updated
                    }
                }
            }

            val entity = existing?.copy(
                status = DownloadStatus.PENDING.value,
                progress = 0f,
                createTime = maxOf(currentTimeMillis(), existing.createTime + 1)
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
                createTime = currentTimeMillis()
            )
            downloadDao.insert(entity)

            downloadStrategy.enqueue(illustId, index, originalUrl, subFolder)
            null
        }
        if (existingDownload != null) {
            downloadManagerListener?.onDownloadCompleted(existingDownload)
            return true
        }

        if (downloadManagerListener != null) {
            scope.launch {
                downloadStrategy.getDownloadState(illustId, index).collect { state ->
                    when (state) {
                        DownloadState.SUCCEEDED -> {
                            val newEntity = downloadDao.getDownload(illustId, index)
                            downloadManagerListener.onDownloadCompleted(newEntity)
                            this.cancel()
                        }

                        DownloadState.FAILED -> {
                            downloadManagerListener.onDownloadCompleted(null)
                            this.cancel()
                        }

                        else -> Unit
                    }
                }
            }
        }
        return true
    }

    suspend fun deleteDownload(entity: DownloadEntity) = downloadChanges.withLock {
        downloadStrategy.cancel(entity.illustId, entity.index)
        downloadDao.delete(entity)
    }

    suspend fun deleteAllDownloads() = downloadChanges.withLock {
        downloadStrategy.cancelAll()
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
        suspend fun onDownloadCompleted(entity: DownloadEntity?)
    }
}
