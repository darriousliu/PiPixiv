package com.mrl.pixiv.common.repository.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import co.touchlab.kermit.Logger
import coil3.annotation.InternalCoilApi
import coil3.util.MimeTypeMap
import com.mrl.pixiv.common.datasource.local.dao.DownloadDao
import com.mrl.pixiv.common.datasource.local.entity.DownloadStatus
import com.mrl.pixiv.common.network.ImageClient
import com.mrl.pixiv.common.util.generateFileName
import com.mrl.pixiv.common.util.saveToAlbum
import io.ktor.client.HttpClient
import io.ktor.client.plugins.onDownload
import io.ktor.client.request.get
import io.ktor.client.statement.readRawBytes
import io.ktor.http.contentType
import io.ktor.http.isSuccess
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
            val result = withTimeoutOrNull(60.seconds) {
                val response = imageHttpClient.get(url) {
                    onDownload { bytesSentTotal, contentLength ->
                        if (contentLength != null && contentLength > 0) {
                            val progress = bytesSentTotal.toFloat() / contentLength.toFloat()
                            Logger.d("DownloadWorker") { "Downloading $bytesSentTotal/$contentLength: $progress" }
                            if (progress != entity.progress) {
                                entity = entity.copy(progress = progress)
                                downloadDao.update(entity)
                            }
                        }
                    }
                }

                if (!response.status.isSuccess()) {
                    throw Exception("Request failed: ${response.status}")
                }

                val bytes = response.readRawBytes()
                var mimeType = response.contentType()?.withoutParameters()?.toString()

                if (mimeType == null) {
                    mimeType = MimeTypeMap.getMimeTypeFromUrl(url)
                }

                Pair(bytes, mimeType)
            }

            result ?: throw Exception("Timeout")

            val (bytes, mimeType) = result

            val success = saveToAlbum(bytes, generateFileName(illustId, index), mimeType, subFolder)

            if (success) {
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
