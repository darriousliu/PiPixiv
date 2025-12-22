package com.mrl.pixiv.common.repository

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.mrl.pixiv.common.repository.worker.DownloadWorker
import com.mrl.pixiv.common.util.PictureType
import com.mrl.pixiv.common.util.getDownloadPath
import com.mrl.pixiv.common.util.isImageExists
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.koin.core.annotation.Single

@Single
class AndroidDownloadStrategy(
    private val context: Context
) : DownloadStrategy {

    override suspend fun enqueue(
        illustId: Long,
        index: Int,
        url: String,
        subFolder: String?
    ) {
        val workManager = WorkManager.getInstance(context)
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val inputData = workDataOf(
            "illustId" to illustId,
            "index" to index,
            "url" to url,
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

    override suspend fun cancel(illustId: Long, index: Int) {
        WorkManager.getInstance(context)
            .cancelUniqueWork("download_${illustId}_${index}")
    }

    override suspend fun cancelAll() {
        WorkManager.getInstance(context).cancelAllWork()
    }

    override fun getDownloadState(illustId: Long, index: Int): Flow<DownloadState> {
        val uniqueWorkName = "download_${illustId}_${index}"
        return WorkManager.getInstance(context).getWorkInfosForUniqueWorkFlow(uniqueWorkName).map { workInfos ->
            val info = workInfos.firstOrNull() ?: return@map DownloadState.UNKNOWN

            when (info.state) {
                WorkInfo.State.ENQUEUED -> DownloadState.PENDING
                WorkInfo.State.RUNNING -> DownloadState.RUNNING
                WorkInfo.State.SUCCEEDED -> DownloadState.SUCCEEDED
                WorkInfo.State.FAILED -> DownloadState.FAILED
                WorkInfo.State.BLOCKED -> DownloadState.PENDING
                WorkInfo.State.CANCELLED -> DownloadState.FAILED
            }
        }
    }

    override suspend fun getExistingFileInfo(
        illustId: Long,
        index: Int,
        fileName: String,
        type: PictureType,
        subFolder: String?
    ): Pair<String, String>? {
        if (isImageExists(fileName, type, subFolder)) {
            return getDownloadPath(fileName, type, subFolder)
        }
        return null
    }
}
