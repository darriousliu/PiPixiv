package com.mrl.pixiv.common.repository

import co.touchlab.kermit.Logger
import com.mrl.pixiv.common.datasource.local.dao.DownloadDao
import com.mrl.pixiv.common.datasource.local.entity.DownloadEntity
import com.mrl.pixiv.common.datasource.local.entity.DownloadStatus
import com.mrl.pixiv.common.repository.util.generateFileName
import com.mrl.pixiv.common.util.PhotoUtil
import com.mrl.pixiv.common.util.PictureType
import com.mrl.pixiv.common.util.ZipUtil
import com.shakster.gifkt.GifEncoder
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.refTo
import kotlinx.cinterop.useContents
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import org.koin.core.annotation.Single
import platform.CoreGraphics.CGBitmapContextCreate
import platform.CoreGraphics.CGContextDrawImage
import platform.CoreGraphics.CGImageGetBitmapInfo
import platform.CoreGraphics.CGImageGetColorSpace
import platform.CoreGraphics.CGImageGetHeight
import platform.CoreGraphics.CGImageGetWidth
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSError
import platform.Foundation.NSFileManager
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.NSURLSession
import platform.Foundation.NSURLSessionConfiguration
import platform.Foundation.NSURLSessionDownloadDelegateProtocol
import platform.Foundation.NSURLSessionDownloadTask
import platform.Foundation.NSURLSessionTask
import platform.UIKit.UIImage
import platform.darwin.NSObject
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalForeignApi::class)
@Single
class IosDownloadStrategy(
    private val downloadDao: DownloadDao,
    zipUtil: ZipUtil,
    photoUtil: PhotoUtil
) : DownloadStrategy {
    private val session: NSURLSession
    private val delegate: DownloadDelegate
    override val downloadFolder = "PiPixiv"

    init {
        val config =
            NSURLSessionConfiguration.backgroundSessionConfigurationWithIdentifier("com.mrl.pixiv.background_download")
        config.HTTPAdditionalHeaders = mapOf(
            "Referer" to "https://app-api.pixiv.net/"
        )
        delegate = DownloadDelegate(downloadDao, zipUtil, downloadFolder, photoUtil)
        session = NSURLSession.sessionWithConfiguration(config, delegate, null)
    }

    override suspend fun enqueue(illustId: Long, index: Int, url: String, subFolder: String?) {
        val downloadUrl = NSURL.URLWithString(url) ?: return
        val task = session.downloadTaskWithURL(downloadUrl)
        task.taskDescription = "$illustId|$index"
        task.resume()
    }

    override suspend fun cancel(illustId: Long, index: Int) {
        session.getTasksWithCompletionHandler { _, _, downloadTasks ->
            val tasks = downloadTasks?.filterIsInstance<NSURLSessionDownloadTask>()
                ?: return@getTasksWithCompletionHandler
            tasks.find { it.taskDescription?.startsWith("$illustId|$index") == true }?.cancel()
        }
    }

    override suspend fun cancelAll() {
        session.getTasksWithCompletionHandler { _, _, downloadTasks ->
            val tasks = downloadTasks?.filterIsInstance<NSURLSessionDownloadTask>()
                ?: return@getTasksWithCompletionHandler
            tasks.forEach { it.cancel() }
        }
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
        return downloadDao.getDownload(illustId, index)?.let { entity ->
            entity.fileUri to entity.filePath
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
class DownloadDelegate(
    private val downloadDao: DownloadDao,
    private val zipUtil: ZipUtil,
    val downloadFolder: String,
    private val photoUtil: PhotoUtil,
) : NSObject(), NSURLSessionDownloadDelegateProtocol {
    override fun URLSession(
        session: NSURLSession,
        downloadTask: NSURLSessionDownloadTask,
        didFinishDownloadingToURL: NSURL
    ) {
        val desc = downloadTask.taskDescription ?: return
        val parts = desc.split("|")
        if (parts.size < 2) return
        val illustId = parts[0].toLongOrNull() ?: return
        val index = parts[1].toIntOrNull() ?: return

        runBlocking {
            val entity = downloadDao.getDownload(illustId, index) ?: return@runBlocking
            Logger.e("URLSession") { "$entity" }
            if (entity.originalUrl.endsWith(".zip")) {
                handleUgoira(entity, didFinishDownloadingToURL)
            } else {
                handleImage(entity, didFinishDownloadingToURL)
            }
        }
    }

    private suspend fun handleUgoira(entity: DownloadEntity, tempZipUrl: NSURL) {
        val fileManager = NSFileManager.defaultManager
        val tempDir = NSURL.fileURLWithPath(NSTemporaryDirectory())
        val zipFileName = "temp_${entity.illustId}.zip"
        val zipFileUrl = tempDir.URLByAppendingPathComponent(zipFileName)

        try {
            if (zipFileUrl != null && zipFileUrl.path != null) {
                if (fileManager.fileExistsAtPath(zipFileUrl.path!!)) {
                    fileManager.removeItemAtURL(zipFileUrl, null)
                }
                fileManager.moveItemAtURL(tempZipUrl, zipFileUrl, null)

                val metadata = PixivRepository.getUgoiraMetadata(entity.illustId).ugoiraMetadata

                val unzipDirUrl =
                    tempDir.URLByAppendingPathComponent("temp_${entity.illustId}_unzip")
                if (unzipDirUrl != null && unzipDirUrl.path != null) {
                    val unzipSuccess = zipUtil.unzip(zipFileUrl.path!!, unzipDirUrl.path!!)

                    if (unzipSuccess) {
                        val gifFileName = generateFileName(
                            entity.illustId,
                            entity.title,
                            entity.userId,
                            entity.userName,
                            entity.index
                        )
                        val gifFileUrl = tempDir.URLByAppendingPathComponent("$gifFileName.gif")

                        if (gifFileUrl != null && gifFileUrl.path != null) {
                            val sink = SystemFileSystem.sink(Path(gifFileUrl.path!!)).buffered()
                            val encoder = GifEncoder(sink)

                            try {
                                metadata.frames.forEach { frame ->
                                    val frameUrl =
                                        unzipDirUrl.URLByAppendingPathComponent(frame.file)
                                    if (frameUrl?.path != null) {
                                        val image = UIImage.imageWithContentsOfFile(frameUrl.path!!)
                                        if (image != null) {
                                            val pixels = getPixels(image)
                                            if (pixels != null) {
                                                val width = image.size.useContents { width }.toInt()
                                                val height =
                                                    image.size.useContents { height }.toInt()
                                                encoder.writeFrame(
                                                    pixels,
                                                    width,
                                                    height,
                                                    frame.delay.milliseconds
                                                )
                                            }
                                        }
                                    }
                                }
                            } finally {
                                encoder.close()
                                fileManager.removeItemAtURL(unzipDirUrl, null)
                            }

                            saveToAlbum(entity, gifFileUrl)

                            try {
                                if (fileManager.fileExistsAtPath(gifFileUrl.path!!)) {
                                    fileManager.removeItemAtURL(gifFileUrl, null)
                                }
                            } catch (_: Exception) {
                            }

                        } else {
                            downloadDao.update(entity.copy(status = DownloadStatus.FAILED.value))
                        }
                    } else {
                        downloadDao.update(entity.copy(status = DownloadStatus.FAILED.value))
                    }
                } else {
                    downloadDao.update(entity.copy(status = DownloadStatus.FAILED.value))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            downloadDao.update(entity.copy(status = DownloadStatus.FAILED.value))
        } finally {
            try {
                if (zipFileUrl?.path != null && fileManager.fileExistsAtPath(zipFileUrl.path!!)) {
                    fileManager.removeItemAtURL(zipFileUrl, null)
                }
            } catch (_: Exception) {
            }
        }
    }

    private suspend fun handleImage(entity: DownloadEntity, tempFileUrl: NSURL) {
        val fileName = generateFileName(
            entity.illustId,
            entity.title,
            entity.userId,
            entity.userName,
            entity.index
        )
        Logger.e("handleImage") { fileName }
        Logger.e("handleImage") { tempFileUrl.toString() }
        val extension = "." + entity.originalUrl.substringAfterLast('.', "")
        val finalFileName = fileName + extension

        val fileManager = NSFileManager.defaultManager
        val tempDir = NSURL.fileURLWithPath(NSTemporaryDirectory())
        val finalFileUrl = tempDir.URLByAppendingPathComponent(finalFileName)
        Logger.e("handleImage") { "$finalFileUrl" }

        if (finalFileUrl != null) {
            try {
                if (finalFileUrl.path != null && fileManager.fileExistsAtPath(finalFileUrl.path!!)) {
                    fileManager.removeItemAtURL(finalFileUrl, null)
                }

                fileManager.copyItemAtURL(tempFileUrl, finalFileUrl, null).also {
                    Logger.e("handleImage") { "File moved to ${finalFileUrl.path} $it" }
                }
                saveToAlbum(entity, finalFileUrl)
            } catch (e: Exception) {
                Logger.e("DownloadDelegate", e)
                downloadDao.update(entity.copy(status = DownloadStatus.FAILED.value))
            } finally {
                try {
                    if (finalFileUrl.path != null && fileManager.fileExistsAtPath(finalFileUrl.path!!)) {
                        fileManager.removeItemAtURL(finalFileUrl, null)
                    }
                } catch (_: Exception) {
                }
            }
        } else {
            downloadDao.update(entity.copy(status = DownloadStatus.FAILED.value))
        }
    }

    private suspend fun saveToAlbum(entity: DownloadEntity, fileUrl: NSURL) {
        photoUtil.saveToAlbum(fileUrl) { localId ->
            if (localId != null) {
                val newEntity = entity.copy(
                    status = DownloadStatus.SUCCESS.value,
                    filePath = "",
                    fileUri = "ph://$localId",
                    progress = 1f
                )
                downloadDao.update(newEntity)
            } else {
                downloadDao.update(entity.copy(status = DownloadStatus.FAILED.value))
            }
        }
    }

    private fun getPixels(image: UIImage): IntArray? {
        val cgImage = image.CGImage ?: return null
        val width = CGImageGetWidth(cgImage)
        val height = CGImageGetHeight(cgImage)
        val bytesPerPixel = 4UL
        val bytesPerRow = bytesPerPixel * width
        val bitsPerComponent = 8UL
        val size = (bytesPerRow * height).toInt()
        val data = ByteArray(size)

        val context = CGBitmapContextCreate(
            data.refTo(0),
            width,
            height,
            bitsPerComponent,
            bytesPerRow,
            CGImageGetColorSpace(cgImage),
            CGImageGetBitmapInfo(cgImage)
        )

        if (context == null) return null

        CGContextDrawImage(
            context,
            CGRectMake(0.0, 0.0, width.toDouble(), height.toDouble()),
            cgImage
        )

        val pixels = IntArray((width * height).toInt())
        for (i in pixels.indices) {
            val offset = i * 4
            val r = data[offset].toInt() and 0xFF
            val g = data[offset + 1].toInt() and 0xFF
            val b = data[offset + 2].toInt() and 0xFF
            val a = data[offset + 3].toInt() and 0xFF
            pixels[i] = (a shl 24) or (r shl 16) or (g shl 8) or b
        }
        return pixels
    }

    override fun URLSession(
        session: NSURLSession,
        task: NSURLSessionTask,
        didCompleteWithError: NSError?
    ) {
        if (didCompleteWithError != null) {
            val desc = task.taskDescription ?: return
            val parts = desc.split("|")
            if (parts.size < 2) return
            val illustId = parts[0].toLongOrNull() ?: return
            val index = parts[1].toIntOrNull() ?: return

            runBlocking {
                val entity = downloadDao.getDownload(illustId, index) ?: return@runBlocking
                downloadDao.update(entity.copy(status = DownloadStatus.FAILED.value))
            }
        }
    }

    override fun URLSession(
        session: NSURLSession,
        downloadTask: NSURLSessionDownloadTask,
        didWriteData: Long,
        totalBytesWritten: Long,
        totalBytesExpectedToWrite: Long
    ) {
        if (totalBytesExpectedToWrite <= 0L) return

        val desc = downloadTask.taskDescription ?: return
        val parts = desc.split("|")
        if (parts.size < 2) return
        val illustId = parts[0].toLongOrNull() ?: return
        val index = parts[1].toIntOrNull() ?: return

        val progress = totalBytesWritten.toFloat() / totalBytesExpectedToWrite.toFloat()
        runBlocking {
            downloadDao.updateProgress(illustId, index, progress)
        }
    }
}
