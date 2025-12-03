package com.mrl.pixiv.common.repository.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import coil3.SingletonImageLoader
import coil3.annotation.InternalCoilApi
import coil3.request.ImageRequest
import coil3.toBitmap
import coil3.util.MimeTypeMap
import com.mrl.pixiv.common.datasource.local.dao.DownloadDao
import com.mrl.pixiv.common.datasource.local.entity.DownloadStatus
import com.mrl.pixiv.common.network.ImageClient
import com.mrl.pixiv.common.util.saveToAlbum
import io.ktor.client.HttpClient
import kotlinx.coroutines.withTimeoutOrNull
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.qualifier.named
import kotlin.time.Duration.Companion.seconds

class DownloadWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params), KoinComponent {

    private val downloadDao: DownloadDao by inject()
    private val imageHttpClient: HttpClient by inject(named<ImageClient>())

    @OptIn(InternalCoilApi::class)
    override suspend fun doWork(): Result {
        val illustId = inputData.getLong("illustId", -1L)
        val index = inputData.getInt("index", -1)
        val url = inputData.getString("url") ?: return Result.failure()
        val subFolder = inputData.getString("subFolder")

        if (illustId == -1L || index == -1) return Result.failure()

        var entity = downloadDao.getDownload(illustId, index) ?: return Result.failure()
        entity = entity.copy(status = DownloadStatus.RUNNING.value, progress = 0f)
        downloadDao.update(entity)

        return try {
            val imageLoader = SingletonImageLoader.get(applicationContext)
            val request = ImageRequest.Builder(applicationContext)
                .data(url)
                .build()

            val result = withTimeoutOrNull(60.seconds) {
                imageLoader.execute(request)
            }

            result ?: throw Exception("Timeout")

            if (result.image == null) throw Exception("Image is null")

            val mimeType = MimeTypeMap.getMimeTypeFromUrl(url)
            val success =
                result.image?.toBitmap()?.saveToAlbum("${illustId}_$index", mimeType, subFolder)

            if (success == true) {
                entity = entity.copy(
                    status = DownloadStatus.SUCCESS.value,
                    progress = 1f
                )
                downloadDao.update(entity)
                Result.success()
            } else {
                throw Exception("Save failed")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            entity = entity.copy(status = DownloadStatus.FAILED.value)
            downloadDao.update(entity)
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }
}
