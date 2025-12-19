package com.mrl.pixiv.common.repository.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import co.touchlab.kermit.Logger
import coil3.annotation.InternalCoilApi
import coil3.util.MimeTypeMap
import com.mrl.pixiv.common.datasource.local.dao.DownloadDao
import com.mrl.pixiv.common.datasource.local.entity.DownloadEntity
import com.mrl.pixiv.common.datasource.local.entity.DownloadStatus
import com.mrl.pixiv.common.network.ImageClient
import com.mrl.pixiv.common.repository.PixivRepository
import com.mrl.pixiv.common.repository.util.generateFileName
import com.mrl.pixiv.common.util.PictureType
import com.mrl.pixiv.common.util.saveToAlbum
import com.mrl.pixiv.common.util.toBitmap
import com.shakster.gifkt.GifEncoder
import io.ktor.client.HttpClient
import io.ktor.client.plugins.onDownload
import io.ktor.client.request.get
import io.ktor.client.statement.readRawBytes
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.io.asSink
import kotlinx.io.buffered
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.qualifier.named
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipFile
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@OptIn(InternalCoilApi::class)
class DownloadWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params), KoinComponent {

    private val downloadDao: DownloadDao by inject()
    private val imageHttpClient: HttpClient by inject(named<ImageClient>())

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
            if (url.endsWith(".zip")) {
                handleUgoira(entity, url, illustId, subFolder)
            } else {
                handleImage(entity, url, illustId, subFolder)
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

    private suspend fun handleUgoira(
        entity: DownloadEntity,
        url: String,
        illustId: Long,
        subFolder: String?
    ): Result {
        val zipBytes = downloadBytes(url, entity)
        val metadata = PixivRepository.getUgoiraMetadata(illustId).ugoiraMetadata

        val zipFile = File(applicationContext.cacheDir, "temp_${illustId}.zip")
        zipFile.writeBytes(zipBytes)

        val zip = ZipFile(zipFile)
        val unzipDir = File(applicationContext.cacheDir, "temp_${illustId}_unzip")
        unzipDir.mkdirs()

        val bitmaps = metadata.frames.mapNotNull { frame ->
            val entry = zip.getEntry(frame.file)
            if (entry != null) {
                val file = File(unzipDir, frame.file)
                if (!file.exists()) {
                    zip.getInputStream(entry).use { input ->
                        FileOutputStream(file).use { output ->
                            input.copyTo(output)
                        }
                    }
                }
                file.toBitmap() to frame.delay
            } else {
                null
            }
        }
        zip.close()
        zipFile.delete()

        val gifFile = File(applicationContext.cacheDir, "temp_${illustId}.gif")
        val sink = gifFile.outputStream().asSink().buffered()
        val encoder = GifEncoder(sink)

        bitmaps.forEach { (bitmap, delay) ->
            if (bitmap != null) {
                encoder.writeFrame(bitmap, delay.milliseconds)
            }
        }
        encoder.close()
        unzipDir.deleteRecursively()

        val gifBytes = gifFile.readBytes()
        gifFile.delete()

        val fileName =
            generateFileName(illustId, entity.title, entity.userId, entity.userName, entity.index)
        val gifPair = saveToAlbum(gifBytes, fileName, PictureType.GIF.mimeType, subFolder)

        if (gifPair != null) {
            val successEntity = entity.copy(
                status = DownloadStatus.SUCCESS.value,
                progress = 1f,
                filePath = gifPair.first,
                fileUri = gifPair.second
            )
            downloadDao.update(successEntity)
            return Result.success()
        } else {
            throw Exception("Save GIF failed")
        }
    }

    private suspend fun handleImage(
        entity: DownloadEntity,
        url: String,
        illustId: Long,
        subFolder: String?
    ): Result {
        val (bytes, mimeType) = downloadBytesWithMime(url, entity)
        val fileName =
            generateFileName(illustId, entity.title, entity.userId, entity.userName, entity.index)
        val imagePair = saveToAlbum(bytes, fileName, mimeType, subFolder)
        if (imagePair != null) {
            val successEntity = entity.copy(
                status = DownloadStatus.SUCCESS.value,
                progress = 1f,
                filePath = imagePair.first,
                fileUri = imagePair.second
            )
            downloadDao.update(successEntity)
            return Result.success()
        } else {
            throw Exception("Save failed")
        }
    }

    private suspend fun downloadBytes(url: String, entity: DownloadEntity): ByteArray {
        return downloadBytesWithMime(url, entity).first
    }

    private suspend fun downloadBytesWithMime(
        url: String,
        entity: DownloadEntity
    ): Pair<ByteArray, String> {
        var currentEntity = entity
        val result = withTimeoutOrNull(60.seconds) {
            val response = imageHttpClient.get(url) {
                onDownload { bytesSentTotal, contentLength ->
                    if (contentLength != null && contentLength > 0) {
                        val progress = bytesSentTotal.toFloat() / contentLength.toFloat()
                        Logger.d("DownloadWorker") { "Downloading $bytesSentTotal/$contentLength: $progress" }
                        if (progress != currentEntity.progress) {
                            currentEntity = currentEntity.copy(progress = progress)
                            downloadDao.update(currentEntity)
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
                mimeType = MimeTypeMap.getMimeTypeFromUrl(url) ?: "application/octet-stream"
            }

            Pair(bytes, mimeType)
        }
        return result ?: throw Exception("Timeout")
    }
}
