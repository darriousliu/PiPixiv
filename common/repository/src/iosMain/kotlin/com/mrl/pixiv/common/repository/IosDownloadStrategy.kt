package com.mrl.pixiv.common.repository

import co.touchlab.kermit.Logger
import com.mrl.pixiv.common.analytics.recordCrashOperation
import com.mrl.pixiv.common.data.Constants
import com.mrl.pixiv.common.data.setting.UserPreference.BypassSetting
import com.mrl.pixiv.common.datasource.local.dao.DownloadDao
import com.mrl.pixiv.common.datasource.local.entity.DownloadEntity
import com.mrl.pixiv.common.datasource.local.entity.DownloadStatus
import com.mrl.pixiv.common.mmkv.MMKVApp
import com.mrl.pixiv.common.mmkv.mmkvSerializable
import com.mrl.pixiv.common.network.NetworkSettings
import com.mrl.pixiv.common.network.configureNetworkProxy
import com.mrl.pixiv.common.network.resolveImageUrl
import com.mrl.pixiv.common.network.withNetworkPreference
import com.mrl.pixiv.common.repository.util.generateFileName
import com.mrl.pixiv.common.util.PhotoUtil
import com.mrl.pixiv.common.util.PictureType
import com.mrl.pixiv.common.util.ZipUtil
import com.shakster.gifkt.GifEncoder
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.refTo
import kotlinx.cinterop.useContents
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.serialization.Serializable
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
import platform.Foundation.NSHTTPURLResponse
import platform.Foundation.NSLock
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.NSURLSession
import platform.Foundation.NSURLSessionConfiguration
import platform.Foundation.NSURLSessionDownloadDelegateProtocol
import platform.Foundation.NSURLSessionDownloadTask
import platform.Foundation.NSURLSessionTask
import platform.Foundation.NSURLSessionTaskMetrics
import platform.Foundation.NSURLSessionTaskTransactionMetrics
import platform.UIKit.UIImage
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.uuid.Uuid

private const val LEGACY_SESSION_ID = "com.mrl.pixiv.background_download"

private fun BypassSetting.iosDownloadRoute(): BypassSetting =
    if (this is BypassSetting.ECH) BypassSetting.Direct else this

@Serializable
private data class BackgroundDownloadAttempt(
    val sessionId: String,
    val taskIdentifier: ULong,
    val createTime: Long,
)

@Serializable
private data class BackgroundDownloadState(
    val sessions: Map<String, BypassSetting> = emptyMap(),
    val attempts: Map<String, BackgroundDownloadAttempt> = emptyMap(),
    val legacyRetired: Boolean = false,
)

private object BackgroundDownloadStore : MMKVApp {
    var iosBackgroundDownloads by mmkvSerializable(BackgroundDownloadState())
}

internal data class BackgroundDownload(val entity: DownloadEntity, val networkSettings: NetworkSettings)

private data class DownloadTaskKey(val illustId: Long, val index: Int) {
    val description: String get() = "$illustId|$index"

    companion object {
        fun parse(description: String): DownloadTaskKey? {
            val parts = description.split('|', limit = 2)
            val illustId = parts.getOrNull(0)?.toLongOrNull() ?: return null
            val index = parts.getOrNull(1)?.toIntOrNull() ?: return null
            return DownloadTaskKey(illustId, index)
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
@Single
class IosDownloadStrategy(
    private val downloadDao: DownloadDao,
    zipUtil: ZipUtil,
    photoUtil: PhotoUtil
) : DownloadStrategy {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val mutex = Mutex()
    private val sessions = mutableMapOf<String, NSURLSession>()
    private val completionLock = NSLock()
    private val backgroundCompletions = mutableMapOf<String, MutableList<() -> Unit>>()
    private val delegate = DownloadDelegate(this, zipUtil, photoUtil)
    private var state = BackgroundDownloadStore.iosBackgroundDownloads
    private var legacyDownloads = emptyMap<String, Long>()
    private var activeSessionId: String? = null
    private var restored = false
    private var checkedUnscheduledDownloads = false
    override val downloadFolder = "PiPixiv"

    fun reconnectPendingDownloads() {
        scope.launch {
            try {
                mutex.withLock { reconnectPendingDownloadsLocked() }
            } catch (failure: Exception) {
                Logger.e(throwable = failure, tag = "Download") { "Unable to reconnect background downloads" }
            }
        }
    }

    fun handleBackgroundEvents(identifier: String, onFinished: () -> Unit) {
        // Register before returning to UIKit. Native delegates can finish while the session is restored.
        withCompletionLock {
            backgroundCompletions.getOrPut(identifier) { mutableListOf() }.add(onFinished)
        }
        scope.launch {
            try {
                mutex.withLock {
                    if (identifier !in state.sessions && (identifier != LEGACY_SESSION_ID || state.legacyRetired)) {
                        finishBackgroundEvents(identifier)
                    } else if (identifier !in sessions) {
                        reconnectSessionLocked(identifier)
                    }
                }
            } catch (failure: Exception) {
                Logger.e(throwable = failure, tag = "Download") { "Unable to restore background session" }
                finishBackgroundEvents(identifier)
            }
        }
    }

    override suspend fun prepareDownload(): Boolean {
        mutex.withLock { reconnectPendingDownloadsLocked() }
        return true
    }

    override suspend fun enqueue(illustId: Long, index: Int, url: String, subFolder: String?) {
        val preferences = requireUserPreferenceValue
        val setting = preferences.bypassSetting.iosDownloadRoute()
        val key = DownloadTaskKey(illustId, index)
        mutex.withLock {
            reconnectPendingDownloadsLocked()
            val entity = downloadDao.getDownload(illustId, index) ?: return
            withContext(NonCancellable) {
                val previous = removeAttemptsLocked(key)
                try {
                    val downloadUrl = try {
                        NSURL.URLWithString(resolveImageUrl(url, preferences.imageHost))
                            ?.takeIf { !it.host.isNullOrEmpty() }
                    } catch (_: Exception) {
                        null
                    }
                    if (downloadUrl == null) {
                        failUnfinished(key.description, entity.createTime)
                    } else {
                        startDownloadLocked(key, entity.createTime, setting, downloadUrl)
                    }
                } finally {
                    previous.forEach { it.cancel() }
                }
            }
        }
    }

    private suspend fun startDownloadLocked(
        key: DownloadTaskKey,
        createTime: Long,
        setting: BypassSetting,
        downloadUrl: NSURL,
    ) {
        var task: NSURLSessionDownloadTask? = null
        try {
            val sessionId = sessionForSettingLocked(setting)
            val newTask = sessions.getValue(sessionId).downloadTaskWithURL(downloadUrl)
            task = newTask
            newTask.taskDescription = key.description
            val attempt = BackgroundDownloadAttempt(sessionId, newTask.taskIdentifier, createTime)
            store(state.copy(attempts = state.attempts + (key.description to attempt)))
            if (downloadDao.updateActiveAttempt(key.illustId, key.index, createTime, DownloadStatus.RUNNING.value) > 0) {
                recordCrashOperation("DownloadStarted")
                newTask.resume()
                return
            }
        } catch (failure: Exception) {
            Logger.e(throwable = failure, tag = "Download") { "Unable to start background download" }
        }
        task?.cancel()
        if (key.description in state.attempts) {
            store(state.copy(attempts = state.attempts - key.description))
        }
        failUnfinished(key.description, createTime)
    }

    override suspend fun cancel(illustId: Long, index: Int) {
        val cancelled = mutex.withLock {
            reconnectPendingDownloadsLocked()
            removeAttemptsLocked(DownloadTaskKey(illustId, index), markFailed = true)
        }
        cancelled.forEach { it.cancel() }
    }

    override suspend fun cancelAll() {
        val cancelled = mutex.withLock {
            reconnectPendingDownloadsLocked()
            removeAttemptsLocked(key = null, markFailed = true)
        }
        cancelled.forEach { it.cancel() }
    }

    private suspend fun removeAttemptsLocked(key: DownloadTaskKey?, markFailed: Boolean = false): List<TasksToCancel> {
        val removing = if (key == null) state.attempts else state.attempts.filterKeys { it == key.description }
        if (markFailed) {
            removing.forEach { (description, attempt) -> failUnfinished(description, attempt.createTime) }
            if (key == null) {
                legacyDownloads.forEach { (description, time) -> failUnfinished(description, time) }
            } else {
                legacyDownloads[key.description]?.let { failUnfinished(key.description, it) }
            }
        }
        legacyDownloads = if (key == null) emptyMap() else legacyDownloads - key.description
        if (removing.isNotEmpty()) {
            store(state.copy(attempts = state.attempts - removing.keys))
        }
        if (key == null) {
            activeSessionId?.let { sessions[it]?.finishTasksAndInvalidate() }
            activeSessionId = null
            return sessions.values.map { TasksToCancel(it) }
        }
        return buildList {
            removing[key.description]?.let { attempt ->
                sessions[attempt.sessionId]?.let {
                    add(TasksToCancel(it, taskIdentifier = attempt.taskIdentifier))
                }
            }
            sessions[LEGACY_SESSION_ID]?.let { add(TasksToCancel(it, key.description)) }
        }
    }

    private suspend fun reconnectPendingDownloadsLocked() {
        if (!restored) {
            val identifiers = if (state.legacyRetired) state.sessions.keys else state.sessions.keys + LEGACY_SESSION_ID
            for (identifier in identifiers.filterNot(sessions::containsKey)) {
                try {
                    reconnectSessionLocked(identifier)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (failure: Exception) {
                    Logger.e(throwable = failure, tag = "Download") { "Unable to reconnect one background session" }
                }
            }
            restored = identifiers.all(sessions::containsKey)
        }
        if (!checkedUnscheduledDownloads) {
            // A process can stop after updating Room but before handing the task to Foundation.
            // The legacy session reconciles its own untracked rows after its delegate finishes.
            if (state.legacyRetired) {
                for (entity in downloadDao.getAllDownloads().first()) {
                    val description = DownloadTaskKey(entity.illustId, entity.index).description
                    if (entity.isActive() && description !in state.attempts) {
                        failUnfinished(description, entity.createTime)
                    }
                }
            }
            checkedUnscheduledDownloads = true
        }
    }

    private suspend fun reconnectSessionLocked(identifier: String) {
        if (identifier == LEGACY_SESSION_ID) {
            legacyDownloads = downloadDao.getAllDownloads().first()
                .filter { it.isActive() }
                .associate { DownloadTaskKey(it.illustId, it.index).description to it.createTime }
                .filterKeys { it !in state.attempts }
            if (identifier !in state.sessions) {
                // The original app did not persist the route for this identifier.
                val setting = requireUserPreferenceValue.bypassSetting.iosDownloadRoute()
                store(state.copy(sessions = state.sessions + (identifier to setting)))
            }
        }
        openSessionLocked(identifier, state.sessions.getValue(identifier)).finishTasksAndInvalidate()
    }

    private fun sessionForSettingLocked(setting: BypassSetting): String {
        activeSessionId?.let { previous ->
            if (state.sessions[previous] == setting) return previous
            activeSessionId = null
            sessions[previous]?.finishTasksAndInvalidate()
        }
        val identifier = "$LEGACY_SESSION_ID.${Uuid.random().toHexString()}"
        store(state.copy(sessions = state.sessions + (identifier to setting)))
        try {
            openSessionLocked(identifier, setting)
        } catch (failure: Exception) {
            store(state.copy(sessions = state.sessions - identifier))
            throw failure
        }
        activeSessionId = identifier
        return identifier
    }

    private fun openSessionLocked(identifier: String, setting: BypassSetting): NSURLSession {
        val configuration = NSURLSessionConfiguration.backgroundSessionConfigurationWithIdentifier(identifier)
        configuration.configureNetworkProxy(setting)
        configuration.HTTPAdditionalHeaders = mapOf("Referer" to Constants.PIXIV_API_BASE_URL)
        return NSURLSession.sessionWithConfiguration(configuration, delegate, null).also {
            sessions[identifier] = it
        }
    }

    private fun store(value: BackgroundDownloadState) {
        BackgroundDownloadStore.iosBackgroundDownloads = value
        state = value
    }

    internal suspend fun currentDownload(sessionId: String, task: NSURLSessionTask): BackgroundDownload? = mutex.withLock {
        val route = state.sessions[sessionId] ?: return@withLock null
        val (key, createTime) = findAttemptLocked(sessionId, task) ?: return@withLock null
        downloadDao.getDownload(key.illustId, key.index)?.takeIf {
            it.createTime == createTime && it.isActive()
        }?.let { entity ->
            BackgroundDownload(
                entity,
                NetworkSettings(route, Constants.IMAGE_HOST, requireUserPreferenceValue.appApiProxyHost),
            )
        }
    }

    internal suspend fun updateDownload(
        sessionId: String,
        task: NSURLSessionTask,
        status: DownloadStatus,
        progress: Float? = null,
        localId: String? = null,
    ) = mutex.withLock {
        val (key, createTime) = findAttemptLocked(sessionId, task) ?: return@withLock
        downloadDao.updateActiveAttempt(
            illustId = key.illustId,
            index = key.index,
            createTime = createTime,
            status = status.value,
            progress = progress,
            filePath = localId?.let { "" },
            fileUri = localId?.let { "ph://$it" },
        )
        if (status != DownloadStatus.RUNNING) {
            when (status) {
                DownloadStatus.SUCCESS -> recordCrashOperation("DownloadSucceeded")
                DownloadStatus.FAILED -> recordCrashOperation("DownloadFailed")
                else -> Unit
            }
            if (sessionId == LEGACY_SESSION_ID) {
                legacyDownloads = legacyDownloads - key.description
            } else {
                store(state.copy(attempts = state.attempts - key.description))
            }
        }
    }

    private fun findAttemptLocked(sessionId: String, task: NSURLSessionTask): Pair<DownloadTaskKey, Long>? {
        val description = task.taskDescription
        if (sessionId == LEGACY_SESSION_ID) {
            val key = description?.let { DownloadTaskKey.parse(it) } ?: return null
            return legacyDownloads[key.description]?.let { key to it }
        }

        val taskIdentifier = task.taskIdentifier
        fun BackgroundDownloadAttempt.matches() = this.sessionId == sessionId && this.taskIdentifier == taskIdentifier
        if (description != null) {
            val key = DownloadTaskKey.parse(description)
            val attempt = state.attempts[description]?.takeIf { it.matches() }
            if (key != null && attempt != null) return key to attempt.createTime
        }
        // The system may deliver tasks after relaunch without the in-process description.
        val (savedDescription, attempt) = state.attempts.entries.firstOrNull { it.value.matches() } ?: return null
        return DownloadTaskKey.parse(savedDescription)?.let { it to attempt.createTime }
    }

    internal fun finishBackgroundEvents(identifier: String) {
        val completions = withCompletionLock { backgroundCompletions.remove(identifier).orEmpty() }
        if (completions.isEmpty()) return
        dispatch_async(dispatch_get_main_queue()) {
            completions.forEach { it() }
        }
    }

    private inline fun <T> withCompletionLock(block: () -> T): T {
        completionLock.lock()
        return try {
            block()
        } finally {
            completionLock.unlock()
        }
    }

    internal suspend fun sessionInvalidated(identifier: String) = mutex.withLock {
        try {
            if (identifier == LEGACY_SESSION_ID) {
                legacyDownloads.forEach { (description, time) -> failUnfinished(description, time) }
                legacyDownloads = emptyMap()
                store(state.copy(sessions = state.sessions - identifier, legacyRetired = true))
            } else {
                val remaining = state.attempts.filterValues { it.sessionId == identifier }
                remaining.forEach { (description, attempt) -> failUnfinished(description, attempt.createTime) }
                store(state.copy(sessions = state.sessions - identifier, attempts = state.attempts - remaining.keys))
            }
        } catch (failure: Exception) {
            restored = false
            throw failure
        } finally {
            sessions.remove(identifier)
            if (activeSessionId == identifier) activeSessionId = null
            finishBackgroundEvents(identifier)
        }
    }

    private suspend fun failUnfinished(description: String, createTime: Long) {
        val key = DownloadTaskKey.parse(description) ?: return
        downloadDao.updateActiveAttempt(key.illustId, key.index, createTime, DownloadStatus.FAILED.value)
    }

    private fun DownloadEntity.isActive(): Boolean =
        status == DownloadStatus.PENDING.value || status == DownloadStatus.RUNNING.value

    private class TasksToCancel(
        private val session: NSURLSession,
        private val description: String? = null,
        private val taskIdentifier: ULong? = null,
    ) {
        fun cancel() {
            session.getTasksWithCompletionHandler { _, _, tasks ->
                tasks?.filterIsInstance<NSURLSessionDownloadTask>()?.forEach { task ->
                    val matchesDescription = description == null || task.taskDescription == description
                    val matchesIdentifier = taskIdentifier == null || task.taskIdentifier == taskIdentifier
                    if (matchesDescription && matchesIdentifier) task.cancel()
                }
            }
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
private class DownloadDelegate(
    private val strategy: IosDownloadStrategy,
    private val zipUtil: ZipUtil,
    private val photoUtil: PhotoUtil,
) : NSObject(), NSURLSessionDownloadDelegateProtocol {
    override fun URLSessionDidFinishEventsForBackgroundURLSession(session: NSURLSession) {
        val identifier = session.configuration.identifier ?: return
        strategy.finishBackgroundEvents(identifier)
    }

    override fun URLSession(session: NSURLSession, didBecomeInvalidWithError: NSError?) {
        val identifier = session.configuration.identifier ?: return
        processEvent { strategy.sessionInvalidated(identifier) }
    }

    override fun URLSession(
        session: NSURLSession,
        task: NSURLSessionTask,
        didFinishCollectingMetrics: NSURLSessionTaskMetrics,
    ) {
        // 只记录连接属性，便于核对后台下载路由，不输出下载地址或请求内容。
        didFinishCollectingMetrics.transactionMetrics
            .filterIsInstance<NSURLSessionTaskTransactionMetrics>()
            .forEach { metrics ->
                Logger.d(tag = "DownloadRoute") {
                    "后台下载连接：使用代理=${metrics.proxyConnection}，远端端口=${metrics.remotePort}"
                }
            }
    }

    override fun URLSession(
        session: NSURLSession,
        downloadTask: NSURLSessionDownloadTask,
        didFinishDownloadingToURL: NSURL
    ) {
        val sessionId = session.configuration.identifier ?: return
        processEvent {
            val download = strategy.currentDownload(sessionId, downloadTask) ?: return@processEvent
            val status = (downloadTask.response as? NSHTTPURLResponse)?.statusCode
            val localId = try {
                if (status != null && status in 200L..299L) {
                    saveDownloadedFile(download, didFinishDownloadingToURL) {
                        strategy.currentDownload(sessionId, downloadTask) != null
                    }
                } else {
                    null
                }
            } catch (failure: Exception) {
                Logger.e(throwable = failure, tag = "Download") { "Unable to save background download" }
                null
            }
            strategy.updateDownload(
                sessionId,
                downloadTask,
                status = if (localId == null) DownloadStatus.FAILED else DownloadStatus.SUCCESS,
                progress = 1f.takeIf { localId != null },
                localId = localId,
            )
        }
    }

    private suspend fun saveDownloadedFile(
        download: BackgroundDownload,
        temporaryFile: NSURL,
        isCurrent: suspend () -> Boolean,
    ): String? {
        val fileManager = NSFileManager.defaultManager
        val temporaryRoot = NSURL.fileURLWithPath(NSTemporaryDirectory())
        val directory = temporaryRoot.URLByAppendingPathComponent("PiPixiv-download-${Uuid.random()}") ?: return null
        if (!fileManager.createDirectoryAtURL(directory, withIntermediateDirectories = true, attributes = null, error = null)) {
            return null
        }
        try {
            val entity = download.entity
            val stem = generateFileName(entity.illustId, entity.title, entity.userId, entity.userName, entity.index)
            val extension = NSURL.URLWithString(entity.originalUrl)?.pathExtension
            val file = if (extension == "zip") {
                encodeUgoira(entity.illustId, temporaryFile, directory, stem, download.networkSettings)
            } else {
                val destination = directory.URLByAppendingPathComponent("$stem.${extension.orEmpty()}") ?: return null
                destination.takeIf { fileManager.copyItemAtURL(temporaryFile, it, null) }
            } ?: return null
            if (!isCurrent()) return null
            var localId: String? = null
            photoUtil.saveToAlbum(file) { localId = it }
            return localId
        } finally {
            fileManager.removeItemAtURL(directory, null)
        }
    }

    private suspend fun encodeUgoira(
        illustId: Long,
        temporaryFile: NSURL,
        directory: NSURL,
        stem: String,
        networkSettings: NetworkSettings,
    ): NSURL? {
        val zip = directory.URLByAppendingPathComponent("frames.zip") ?: return null
        val frames = directory.URLByAppendingPathComponent("frames") ?: return null
        val gif = directory.URLByAppendingPathComponent("$stem.gif") ?: return null
        val zipPath = zip.path ?: return null
        val framesPath = frames.path ?: return null
        val gifPath = gif.path ?: return null
        if (!NSFileManager.defaultManager.moveItemAtURL(temporaryFile, zip, null)) return null
        val metadata = withNetworkPreference({ networkSettings }) {
            PixivRepository.getUgoiraMetadata(illustId).ugoiraMetadata
        }
        if (!zipUtil.unzip(zipPath, framesPath)) return null

        val encoder = GifEncoder(SystemFileSystem.sink(Path(gifPath)).buffered())
        try {
            metadata.frames.forEach { frame ->
                val framePath = frames.URLByAppendingPathComponent(frame.file)?.path ?: return@forEach
                val image = UIImage.imageWithContentsOfFile(framePath) ?: return@forEach
                val pixels = getPixels(image) ?: return@forEach
                encoder.writeFrame(
                    pixels,
                    image.size.useContents { width }.toInt(),
                    image.size.useContents { height }.toInt(),
                    frame.delay.milliseconds,
                )
            }
        } finally {
            encoder.close()
        }
        return gif
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
        didCompleteWithError: NSError?,
    ) {
        if (didCompleteWithError == null) return
        val sessionId = session.configuration.identifier ?: return
        processEvent { strategy.updateDownload(sessionId, task, DownloadStatus.FAILED) }
    }

    override fun URLSession(
        session: NSURLSession,
        downloadTask: NSURLSessionDownloadTask,
        didWriteData: Long,
        totalBytesWritten: Long,
        totalBytesExpectedToWrite: Long,
    ) {
        if (totalBytesExpectedToWrite <= 0L) return
        val sessionId = session.configuration.identifier ?: return
        val progress = totalBytesWritten.toFloat() / totalBytesExpectedToWrite.toFloat()
        processEvent { strategy.updateDownload(sessionId, downloadTask, DownloadStatus.RUNNING, progress) }
    }

    private fun processEvent(block: suspend () -> Unit) {
        try {
            runBlocking { block() }
        } catch (failure: Exception) {
            Logger.e(throwable = failure, tag = "Download") { "Unable to process background download event" }
        }
    }
}
