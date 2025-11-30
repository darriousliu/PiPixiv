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
import androidx.lifecycle.viewModelScope
import com.mrl.pixiv.common.util.AppUtil
import com.mrl.pixiv.common.util.OLD_DOWNLOAD_DIR
import com.mrl.pixiv.common.util.RString
import com.mrl.pixiv.common.util.ToastUtil
import com.mrl.pixiv.common.viewmodel.BaseMviViewModel
import com.mrl.pixiv.common.viewmodel.SideEffect
import com.mrl.pixiv.common.viewmodel.ViewIntent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel
import java.io.File

data class AppDataState(
    val oldImageCount: Int = 0,
    val isMigrating: Boolean = false,
    val progress: Float = 0f,
    @IntRange(from = 0)
    val migratedCount: Int = 0,
)

data class RequestPermissionEffect(val intentSender: IntentSender) : SideEffect

@KoinViewModel
class AppDataViewModel : BaseMviViewModel<AppDataState, ViewIntent>(
    initialState = AppDataState(),
) {
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
            if (oldDir.exists() && oldDir.isDirectory) {
                val count = oldDir.list()?.size ?: 0
                updateState { copy(oldImageCount = count) }
            } else {
                updateState { copy(oldImageCount = 0) }
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

            if (!oldDir.exists()) {
                updateState { copy(isMigrating = false) }
                return@launchIO
            }

            if (!newDir.exists()) {
                newDir.mkdirs()
            }

            val files = oldDir.listFiles() ?: emptyArray()
            val total = files.size
            var successCount = 0
            val context = AppUtil.appContext
            val resolver = context.contentResolver

            // 收集需要请求权限删除的文件的 Uri (针对 Android 11+)
            val pendingDeleteUris = mutableListOf<Uri>()

            files.forEachIndexed { index, file ->
                if (file.isFile) {
                    val destFile = File(newDir, file.name)
                    try {
                        // 1. 尝试复制 (如果目标已存在且大小相同则跳过，提高重试效率)
                        if (!destFile.exists() || destFile.length() != file.length()) {
                            file.copyTo(destFile, overwrite = true)
                            // 扫描新文件使其在相册可见
                            MediaScannerConnection.scanFile(
                                context,
                                arrayOf(destFile.absolutePath),
                                null,
                                null
                            )
                        }

                        // 2. 尝试删除源文件
                        // 优先尝试 File API 删除
                        var deleteSuccess = file.delete()

                        // 3. 如果 File API 删除失败且文件仍存在，尝试查找 Uri 准备后续处理
                        if (!deleteSuccess && file.exists()) {
                            val uri = getMediaUri(resolver, file)
                            if (uri != null) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                    // Android 11+ 收集起来批量处理
                                    pendingDeleteUris.add(uri)
                                } else {
                                    // Android 10 及以下无法批量申请删除权限，尝试单个删除（可能会失败）
                                    try {
                                        val rows = resolver.delete(uri, null, null)
                                        if (rows > 0) deleteSuccess = true
                                    } catch (e: Exception) {
                                        // 忽略异常，Android 10 上避免连续弹窗，最后统一提示用户手动删除
                                        e.printStackTrace()
                                    }
                                }
                            }
                        }

                        if (deleteSuccess || pendingDeleteUris.contains(
                                getMediaUri(
                                    resolver,
                                    file
                                )
                            )
                        ) {
                            // 即使放入待删除列表，也算作迁移步骤完成（等待最后确认）
                            successCount++
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                updateState {
                    copy(
                        progress = (index + 1) / total.toFloat(),
                        migratedCount = index + 1
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
            if (oldDir.listFiles()?.isEmpty() == true) {
                oldDir.delete()
            }

            // 重新检查剩余文件数量
            val remainingCount = oldDir.listFiles()?.size ?: 0

            updateState {
                copy(
                    isMigrating = false,
                    oldImageCount = remainingCount
                )
            }

            if (remainingCount == 0) {
                ToastUtil.safeShortToast(RString.migration_success)
            } else {
                if (pendingDeleteUris.isNotEmpty() && Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                    // Android 10 等无法自动删除的情况
                    ToastUtil.safeShortToast("迁移完成。部分旧文件需手动删除。")
                } else {
                    ToastUtil.safeShortToast(RString.migration_failed)
                }
            }
        }
    }

    private fun getMediaUri(resolver: ContentResolver, file: File): Uri? {
        val projection = arrayOf(MediaStore.Images.Media._ID)
        val selection =
            "${MediaStore.Images.Media.DISPLAY_NAME} = ? AND ${MediaStore.Images.Media.RELATIVE_PATH} = ?"
        val selectionArgs = arrayOf(file.name, OLD_DOWNLOAD_DIR)

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
}
