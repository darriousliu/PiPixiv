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
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.contentLength
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.utils.io.copyTo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.io.asSink
import kotlinx.io.buffered
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.qualifier.named
import java.io.File
import java.io.FileOutputStream
import java.nio.channels.FileChannel
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
            if (runAttemptCount < 1) {
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
        val zipFile = File(applicationContext.cacheDir, "temp_${illustId}.zip")
        downloadToFile(url, entity, zipFile)
        val metadata = PixivRepository.getUgoiraMetadata(illustId).ugoiraMetadata

        val unzipDir = File(applicationContext.cacheDir, "temp_${illustId}_unzip")
        unzipDir.mkdirs()
        val gifFile = File(applicationContext.cacheDir, "temp_${illustId}.gif")

        try {
            withContext(Dispatchers.IO) {
                ZipFile(zipFile).use { zip ->
                    val sink = gifFile.outputStream().asSink().buffered()
                    val encoder = GifEncoder(sink)

                    encoder.use { encoder ->
                        metadata.frames.forEach { frame ->
                            val entry = zip.getEntry(frame.file) ?: return@forEach
                            val file = File(unzipDir, frame.file)
                            if (!file.exists()) {
                                zip.getInputStream(entry).use { input ->
                                    FileOutputStream(file).use { output ->
                                        input.copyTo(output)
                                    }
                                }
                            }

                            val bitmap = file.toBitmap()
                            if (bitmap != null) {
                                try {
                                    encoder.writeFrame(bitmap, frame.delay.milliseconds)
                                } finally {
                                    bitmap.recycle()
                                }
                            }
                            file.delete()
                        }
                    }
                }
            }

            val fileName =
                generateFileName(illustId, entity.title, entity.userId, entity.userName, entity.index)
            val gifPair = saveToAlbum(gifFile, fileName, PictureType.GIF.mimeType, subFolder)

            if (gifPair != null) {
                val (fileUri, filePath) = gifPair
                val successEntity = entity.copy(
                    status = DownloadStatus.SUCCESS.value,
                    progress = 1f,
                    filePath = filePath,
                    fileUri = fileUri
                )
                downloadDao.update(successEntity)
                return Result.success()
            } else {
                throw Exception("Save GIF failed")
            }
        } finally {
            zipFile.delete()
            gifFile.delete()
            unzipDir.deleteRecursively()
        }
    }

    private suspend fun handleImage(
        entity: DownloadEntity,
        url: String,
        illustId: Long,
        subFolder: String?
    ): Result {
        val imageFile = withContext(Dispatchers.IO) {
            File.createTempFile(
                "download_${illustId}_${entity.index}_",
                ".tmp",
                applicationContext.cacheDir
            )
        }
        try {
            val mimeType = downloadToFile(url, entity, imageFile)
            val fileName =
                generateFileName(illustId, entity.title, entity.userId, entity.userName, entity.index)
            val imagePair = saveToAlbum(imageFile, fileName, mimeType, subFolder)
            if (imagePair != null) {
                val (fileUri, filePath) = imagePair
                val successEntity = entity.copy(
                    status = DownloadStatus.SUCCESS.value,
                    progress = 1f,
                    filePath = filePath,
                    fileUri = fileUri
                )
                downloadDao.update(successEntity)
                return Result.success()
            } else {
                throw Exception("Save failed")
            }
        } finally {
            imageFile.delete()
        }
    }

    private suspend fun downloadToFile(
        url: String,
        entity: DownloadEntity,
        outputFile: File
    ): String {
        var currentEntity = entity
        val result = withTimeoutOrNull(60.seconds) {
            imageHttpClient.prepareGet(url) {
                onDownload { bytesSentTotal, contentLength ->
                    if (contentLength != null && contentLength > 0) {
                        val progress = bytesSentTotal.toFloat() / contentLength.toFloat()
                        Logger.d(tag = "DownloadWorker") { "Downloading $bytesSentTotal/$contentLength: $progress" }
                        if (progress != currentEntity.progress) {
                            currentEntity = currentEntity.copy(progress = progress)
                            downloadDao.update(currentEntity)
                        }
                    }
                }
            }.execute { response ->
                if (!response.status.isSuccess()) {
                    throw Exception("Request failed: ${response.status}")
                }

                var mimeType = response.contentType()?.withoutParameters()?.toString()

                if (mimeType == null) {
                    mimeType = MimeTypeMap.getMimeTypeFromUrl(url) ?: "application/octet-stream"
                }

                FileOutputStream(outputFile).channel.use { channel: FileChannel ->
                    response.bodyAsChannel().copyTo(channel)
                }

                if (response.contentLength() == null || currentEntity.progress < 1f) {
                    currentEntity = currentEntity.copy(progress = 1f)
                    downloadDao.update(currentEntity)
                }

                mimeType
            }
        }
        return result ?: throw Exception("Timeout")
    }
}
