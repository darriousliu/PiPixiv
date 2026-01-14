package com.mrl.pixiv.common.repository

import coil3.annotation.InternalCoilApi
import coil3.util.MimeTypeMap
import com.mrl.pixiv.common.datasource.local.dao.DownloadDao
import com.mrl.pixiv.common.datasource.local.entity.DownloadEntity
import com.mrl.pixiv.common.datasource.local.entity.DownloadStatus
import com.mrl.pixiv.common.network.ImageClient
import com.mrl.pixiv.common.repository.util.generateFileName
import com.mrl.pixiv.common.util.PictureType
import com.shakster.gifkt.GifEncoder
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.absolutePath
import io.github.vinceglb.filekit.createDirectories
import io.github.vinceglb.filekit.div
import io.github.vinceglb.filekit.exists
import io.github.vinceglb.filekit.pictureDir
import io.ktor.client.HttpClient
import io.ktor.client.plugins.onDownload
import io.ktor.client.request.get
import io.ktor.client.statement.readRawBytes
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.io.asSink
import kotlinx.io.buffered
import org.koin.core.annotation.Single
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.zip.ZipFile
import javax.imageio.ImageIO
import kotlin.time.Duration.Companion.milliseconds

@Single
@OptIn(InternalCoilApi::class)
class JvmDownloadStrategy(
    private val downloadDao: DownloadDao,
    @ImageClient private val httpClient: HttpClient
) : DownloadStrategy {

    private val scope = CoroutineScope(Dispatchers.IO)
    private val jobs = ConcurrentHashMap<String, Job>()

    override val downloadFolder = (FileKit.pictureDir / "PiPixiv").absolutePath()

    override suspend fun enqueue(illustId: Long, index: Int, url: String, subFolder: String?) {
        val key = getKey(illustId, index)
        if (jobs.containsKey(key)) return

        val job = scope.launch {
            try {
                processDownload(illustId, index, url, subFolder)
            } catch (e: Exception) {
                if (e !is CancellationException) {
                    e.printStackTrace()
                    updateStatus(illustId, index, DownloadStatus.FAILED)
                }
            } finally {
                jobs.remove(key)
            }
        }
        jobs[key] = job
    }

    private suspend fun processDownload(
        illustId: Long,
        index: Int,
        url: String,
        subFolder: String?
    ) {
        var entity = downloadDao.getDownload(illustId, index) ?: return
        entity = entity.copy(status = DownloadStatus.RUNNING.value, progress = 0f)
        downloadDao.update(entity)

        if (url.endsWith(".zip")) {
            handleUgoira(entity, url, illustId, subFolder)
        } else {
            handleImage(entity, url, subFolder)
        }
    }

    private suspend fun handleImage(entity: DownloadEntity, url: String, subFolder: String?) {
        val (bytes, mimeType) = downloadBytes(url, entity)

        val type = PictureType.fromMimeType(mimeType) ?: PictureType.JPG
        val fileName = generateFileName(
            entity.illustId,
            entity.title,
            entity.userId,
            entity.userName,
            entity.index
        )
        val file = getFile(fileName, type, subFolder)
        file.writeBytes(bytes)

        val finalEntity = entity.copy(
            status = DownloadStatus.SUCCESS.value,
            progress = 1f,
            filePath = file.absolutePath.replace("\\", "/"),
            fileUri = "file:///${file.absolutePath.replace("\\", "/")}"
        )
        downloadDao.update(finalEntity)
    }

    private suspend fun handleUgoira(
        entity: DownloadEntity,
        url: String,
        illustId: Long,
        subFolder: String?
    ) {
        val (zipBytes, _) = downloadBytes(url, entity)

        val tempZip = File.createTempFile("ugoira_$illustId", ".zip")
        tempZip.writeBytes(zipBytes)

        val metadata = PixivRepository.getUgoiraMetadata(illustId).ugoiraMetadata
        val zip = ZipFile(tempZip)

        val fileName = generateFileName(
            entity.illustId,
            entity.title,
            entity.userId,
            entity.userName,
            entity.index
        )
        val gifFile = getFile(fileName, PictureType.GIF, subFolder)

        val sink = gifFile.outputStream().asSink().buffered()
        val encoder = GifEncoder(sink)

        try {
            metadata.frames.forEach { frame ->
                val entry = zip.getEntry(frame.file)
                if (entry != null) {
                    zip.getInputStream(entry).use { input ->
                        val image = ImageIO.read(input)
                        if (image != null) {
                            val width = image.width
                            val height = image.height
                            val argb = IntArray(width * height)
                            image.getRGB(0, 0, width, height, argb, 0, width)
                            encoder.writeFrame(argb, width, height, frame.delay.milliseconds)
                        }
                    }
                }
            }
        } finally {
            encoder.close()
            zip.close()
            tempZip.delete()
        }

        val finalEntity = entity.copy(
            status = DownloadStatus.SUCCESS.value,
            progress = 1f,
            filePath = gifFile.absolutePath.replace("\\", "/"),
            fileUri = "file:///${gifFile.absolutePath.replace("\\", "/")}"
        )
        downloadDao.update(finalEntity)
    }

    private suspend fun downloadBytes(
        url: String,
        entity: DownloadEntity
    ): Pair<ByteArray, String> {
        var currentEntity = entity
        val response = httpClient.get(url) {
            onDownload { bytesSentTotal, contentLength ->
                if (contentLength != null && contentLength > 0) {
                    val progress = bytesSentTotal.toFloat() / contentLength.toFloat()
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
        return bytes to mimeType
    }

    private suspend fun updateStatus(illustId: Long, index: Int, status: DownloadStatus) {
        val entity = downloadDao.getDownload(illustId, index)
        if (entity != null) {
            downloadDao.update(entity.copy(status = status.value))
        }
    }

    override suspend fun cancel(illustId: Long, index: Int) {
        val key = getKey(illustId, index)
        jobs[key]?.cancel()
        updateStatus(illustId, index, DownloadStatus.FAILED)
    }

    override suspend fun cancelAll() {
        jobs.values.forEach { it.cancel() }
        jobs.clear()
    }

    override fun getDownloadState(illustId: Long, index: Int): Flow<DownloadState> {
        return downloadDao.getAllDownloads().map { list ->
            val entity = list.find { it.illustId == illustId && it.index == index }
            when (entity?.status) {
                DownloadStatus.PENDING.value -> DownloadState.PENDING
                DownloadStatus.RUNNING.value -> DownloadState.RUNNING
                DownloadStatus.SUCCESS.value -> DownloadState.SUCCEEDED
                DownloadStatus.FAILED.value -> DownloadState.FAILED
                else -> DownloadState.UNKNOWN
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
        val file = getFile(fileName, type, subFolder)
        if (file.exists()) {
            return "file:///${file.absolutePath}" to file.absolutePath
        }
        return null
    }

    private fun getFile(fileName: String, type: PictureType, subFolder: String?): File {
        val baseDir = downloadFolder
        val folder =
            if (subFolder != null) PlatformFile(baseDir) / subFolder else PlatformFile(baseDir)
        if (!folder.exists()) {
            folder.createDirectories()
        }
        return PlatformFile(folder, "$fileName${type.extension}").file
    }

    private fun getKey(illustId: Long, index: Int) = "${illustId}_${index}"
}
