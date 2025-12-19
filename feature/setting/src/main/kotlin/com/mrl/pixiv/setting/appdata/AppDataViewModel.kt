package com.mrl.pixiv.setting.appdata

import android.content.ContentResolver
import android.content.ContentUris
import android.content.IntentSender
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.annotation.IntRange
import androidx.annotation.StringRes
import androidx.lifecycle.viewModelScope
import com.mrl.pixiv.common.data.Tag
import com.mrl.pixiv.common.data.comment.Comment
import com.mrl.pixiv.common.data.search.Search
import com.mrl.pixiv.common.data.setting.UserPreference
import com.mrl.pixiv.common.datasource.local.PixivDatabase
import com.mrl.pixiv.common.datasource.local.entity.DownloadEntity
import com.mrl.pixiv.common.repository.BlockingRepositoryV2
import com.mrl.pixiv.common.repository.BookmarkedTagRepository
import com.mrl.pixiv.common.repository.SearchRepository
import com.mrl.pixiv.common.repository.SettingRepository
import com.mrl.pixiv.common.toast.ToastMessage
import com.mrl.pixiv.common.util.AppUtil
import com.mrl.pixiv.common.util.OLD_DOWNLOAD_DIR
import com.mrl.pixiv.common.util.RString
import com.mrl.pixiv.common.util.ToastUtil
import com.mrl.pixiv.common.util.listFilesRecursively
import com.mrl.pixiv.common.viewmodel.BaseMviViewModel
import com.mrl.pixiv.common.viewmodel.SideEffect
import com.mrl.pixiv.common.viewmodel.ViewIntent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okio.buffer
import okio.sink
import okio.source
import org.koin.android.annotation.KoinViewModel
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

@Serializable
data class AppExportData(
    val userPreference: UserPreference,
    val searchHistory: Search,
    val blockIllusts: Set<String>,
    val blockUsers: Set<String>,
    val blockComments: List<Comment>,
    val bookmarkedTags: List<Tag>,
    val downloads: List<DownloadEntity> = emptyList()
)

data class AppDataState(
    val oldImageCount: Int = 0,
    val isMigrating: Boolean = false,
    val progress: Float = 0f,
    @IntRange(from = 0)
    val migratedCount: Int = 0,
    val isLoading: Boolean = false,
    @StringRes
    val loadingMessage: Int? = null
)

data class RequestPermissionEffect(val intentSender: IntentSender) : SideEffect

private const val jsonDataFile = "data.json"

@KoinViewModel
class AppDataViewModel(
    private val database: PixivDatabase,
) : BaseMviViewModel<AppDataState, ViewIntent>(
    initialState = AppDataState(),
) {
    private val regex = Regex("""^(\d+)_(\d+)(\..+)?$""")

    init {
        checkOldData()
    }

    override suspend fun handleIntent(intent: ViewIntent) {

    }

    private fun checkOldData() {
        viewModelScope.launch(Dispatchers.IO) {
            val oldDir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM),
                "PiPixiv"
            )
            val newDir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                "PiPixiv"
            )

            var count = 0
            if (oldDir.exists() && oldDir.isDirectory) {
                count += oldDir.list()?.size ?: 0
            }

            count += newDir.listFilesRecursively()
                .filter { it.isFile && regex.matches(it.name) }.size

            updateState { copy(oldImageCount = count) }
        }
    }

    fun exportData(uri: Uri) {
        launchIO {
            updateState { copy(isLoading = true, loadingMessage = RString.exporting) }
            try {
                val downloads = database.downloadDao().getAllDownloads().first()

                val data = AppExportData(
                    userPreference = SettingRepository.userPreferenceFlow.value,
                    searchHistory = SearchRepository.searchHistoryFlow.value,
                    blockIllusts = BlockingRepositoryV2.blockIllustsFlow.value ?: emptySet(),
                    blockUsers = BlockingRepositoryV2.blockUsersFlow.value ?: emptySet(),
                    blockComments = BlockingRepositoryV2.blockCommentsFlow.value,
                    bookmarkedTags = BookmarkedTagRepository.bookmarkedTags.value,
                    downloads = downloads
                )
                val json = Json {
                    ignoreUnknownKeys = true
                    encodeDefaults = true
                }
                val jsonString = json.encodeToString(AppExportData.serializer(), data)

                val context = AppUtil.appContext
                context.contentResolver.openOutputStream(uri)?.use { os ->
                    ZipOutputStream(os).use { zipOs ->
                        // 将 ZipOutputStream 包装为 Okio BufferedSink，方便写入
                        val sink = zipOs.sink().buffer()

                        // --- 写入 JSON ---
                        zipOs.putNextEntry(ZipEntry(jsonDataFile))
                        sink.writeUtf8(jsonString)
                        sink.flush() // 确保数据写入到底层 zip 流，但不要 close sink
                        zipOs.closeEntry()
                    }
                }
                ToastUtil.safeShortToast(RString.export_success)
            } catch (e: Exception) {
                e.printStackTrace()
                ToastUtil.safeShortToast(
                    ToastMessage.Resource(
                        RString.export_failed,
                        arrayOf(e.message.orEmpty())
                    )
                )
            } finally {
                updateState { copy(isLoading = false) }
            }
        }
    }

    fun importData(uri: Uri) {
        launchIO {
            updateState { copy(isLoading = true, loadingMessage = RString.importing) }
            try {
                val context = AppUtil.appContext
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    ZipInputStream(inputStream).use { zis ->
                        // 将 ZipInputStream 包装为 Okio BufferedSource
                        // 注意：这里我们不直接 close 这个 source，因为它会关闭底层的 zis
                        val zipSource = zis.source().buffer()
                        var entry = zis.nextEntry
                        while (entry != null) {
                            if (entry.name == jsonDataFile) {

                                val jsonString = zipSource.readUtf8()

                                val json = Json { ignoreUnknownKeys = true }
                                val data = json.decodeFromString<AppExportData>(jsonString)
                                SettingRepository.restore(data.userPreference)
                                SearchRepository.restore(data.searchHistory)
                                BlockingRepositoryV2.restore(
                                    data.blockIllusts,
                                    data.blockUsers,
                                    data.blockComments
                                )
                                BookmarkedTagRepository.restore(data.bookmarkedTags)

                                if (data.downloads.isNotEmpty()) {
                                    database.downloadDao().insertAll(data.downloads)
                                }
                            }
                            zis.closeEntry()
                            entry = zis.nextEntry
                        }
                    }
                }
                ToastUtil.safeShortToast(RString.import_success)
            } catch (e: Exception) {
                e.printStackTrace()
                ToastUtil.safeShortToast(
                    ToastMessage.Resource(
                        RString.import_failed,
                        arrayOf(e.message.orEmpty())
                    )
                )
            } finally {
                updateState { copy(isLoading = false) }
            }
        }
    }

    fun migrateData() {
        launchIO {
            updateState {
                copy(
                    isMigrating = true,
                    progress = 0f,
                    migratedCount = 0
                )
            }
            val oldDir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM),
                "PiPixiv"
            )
            val newDir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                "PiPixiv"
            )

            if (!newDir.exists()) {
                newDir.mkdirs()
            }

            val oldFiles =
                if (oldDir.exists()) oldDir.listFiles()?.filter { it.isFile } ?: emptyList()
                else emptyList()
            val newFilesToRename = mutableListOf<File>()

            if (newDir.exists()) {
                newFilesToRename.addAll(
                    newDir.listFilesRecursively().filter { it.isFile && regex.matches(it.name) })
            }

            val total = oldFiles.size + newFilesToRename.size

            if (total == 0) {
                updateState {
                    copy(
                        isMigrating = false,
                        oldImageCount = 0
                    )
                }
                ToastUtil.safeShortToast(RString.migration_success)
                return@launchIO
            }

            var successCount = 0
            var processedCount = 0
            val context = AppUtil.appContext
            val resolver = context.contentResolver

            // 收集需要请求权限删除的文件的 Uri (针对 Android 11+)
            val pendingDeleteUris = mutableListOf<Uri>()

            // 1. Migrate Old Files
            oldFiles.forEach { file ->
                var destName = file.name
                val match = regex.find(destName)
                if (match != null) {
                    val (id, idx, ext) = match.destructured
                    destName = "${id}_p${idx}${ext}"
                }
                val destFile = File(newDir, destName)
                try {
                    if (!destFile.exists() || destFile.length() != file.length()) {
                        file.copyTo(destFile, overwrite = true)
                        MediaScannerConnection.scanFile(
                            context,
                            arrayOf(destFile.absolutePath),
                            null,
                            null
                        )
                    }

                    var deleteSuccess = file.delete()

                    if (!deleteSuccess && file.exists()) {
                        val uri = getMediaUri(resolver, file, OLD_DOWNLOAD_DIR)
                        if (uri != null) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                pendingDeleteUris.add(uri)
                            } else {
                                try {
                                    val rows = resolver.delete(uri, null, null)
                                    if (rows > 0) deleteSuccess = true
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        }
                    }

                    if (deleteSuccess ||
                        (getMediaUri(resolver, file, OLD_DOWNLOAD_DIR)?.let {
                            pendingDeleteUris.contains(it)
                        } == true)
                    ) {
                        successCount++
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                processedCount++
                updateState {
                    copy(
                        progress = processedCount / total.toFloat(),
                        migratedCount = processedCount
                    )
                }
            }

            // 2. Rename New Files
            newFilesToRename.forEach { file ->
                var destName = file.name
                val match = regex.find(destName)
                if (match != null) {
                    val (id, idx, ext) = match.destructured
                    destName = "${id}_p${idx}${ext}"
                }
                val destFile = File(file.parentFile, destName)

                try {
                    var renamed = false
                    if (file.renameTo(destFile)) {
                        MediaScannerConnection.scanFile(
                            context,
                            arrayOf(destFile.absolutePath, file.absolutePath),
                            null,
                            null
                        )
                        renamed = true
                    } else {
                        // Fallback: Copy and Delete
                        if (!destFile.exists() || destFile.length() != file.length()) {
                            file.copyTo(destFile, overwrite = true)
                            MediaScannerConnection.scanFile(
                                context,
                                arrayOf(destFile.absolutePath),
                                null,
                                null
                            )
                        }
                        var deleteSuccess = file.delete()
                        if (!deleteSuccess && file.exists()) {
                            val uri = getMediaUri(resolver, file, getRelativePath(file))
                            if (uri != null) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                    pendingDeleteUris.add(uri)
                                } else {
                                    try {
                                        val rows = resolver.delete(uri, null, null)
                                        if (rows > 0) deleteSuccess = true
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }
                            }
                        }
                        if (deleteSuccess ||
                            (getMediaUri(resolver, file, getRelativePath(file))?.let {
                                pendingDeleteUris.contains(it)
                            } == true)
                        ) {
                            renamed = true
                        }
                    }
                    if (renamed) successCount++
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                processedCount++
                updateState {
                    copy(
                        progress = processedCount / total.toFloat(),
                        migratedCount = processedCount
                    )
                }
            }

            // 处理批量删除权限请求 (Android 11+)
            if (pendingDeleteUris.isNotEmpty() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                // 创建批量删除请求
                // 注意：这里会中断流程等待用户操作，用户同意后系统会执行删除
                val deleteRequest = MediaStore.createDeleteRequest(resolver, pendingDeleteUris)
                sendEffect(RequestPermissionEffect(deleteRequest.intentSender))

                updateState { copy(isMigrating = false) }
                return@launchIO
            }

            // 如果旧文件夹为空，删除它
            if (oldDir.exists() && oldDir.listFiles()?.isEmpty() == true) {
                oldDir.delete()
            }

            checkOldData()

            updateState {
                copy(isMigrating = false)
            }

            if (pendingDeleteUris.isEmpty()) {
                ToastUtil.safeShortToast(RString.migration_success)
            } else {
                ToastUtil.safeShortToast(RString.migration_manually_delete)
            }
        }
    }

    private fun getMediaUri(resolver: ContentResolver, file: File, relativePath: String): Uri? {
        val projection = arrayOf(MediaStore.Images.Media._ID)
        val selection =
            "${MediaStore.Images.Media.DISPLAY_NAME} = ? AND ${MediaStore.Images.Media.RELATIVE_PATH} = ?"
        val selectionArgs = arrayOf(file.name, relativePath)

        try {
            resolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idIndex = cursor.getColumnIndex(MediaStore.MediaColumns._ID)
                    if (idIndex != -1) {
                        val id = cursor.getLong(idIndex)
                        return ContentUris.withAppendedId(
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                            id
                        )
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    private fun getRelativePath(file: File): String {
        val root = Environment.getExternalStorageDirectory().absolutePath
        val parent = file.parentFile?.absolutePath ?: return ""
        return if (parent.startsWith(root)) {
            var rel = parent.substring(root.length)
            if (rel.startsWith("/")) rel = rel.substring(1)
            if (!rel.endsWith("/")) rel += "/"
            rel
        } else {
            ""
        }
    }
}
