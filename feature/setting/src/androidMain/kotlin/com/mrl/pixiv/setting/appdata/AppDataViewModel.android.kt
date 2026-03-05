package com.mrl.pixiv.setting.appdata

import android.content.ContentResolver
import android.content.ContentUris
import android.content.IntentSender
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.mrl.pixiv.common.util.AppUtil
import com.mrl.pixiv.common.util.OLD_DOWNLOAD_DIR
import com.mrl.pixiv.common.util.RStrings
import com.mrl.pixiv.common.util.ToastUtil
import com.mrl.pixiv.common.util.listFilesRecursively
import com.mrl.pixiv.common.viewmodel.SideEffect
import com.mrl.pixiv.strings.migration_manually_delete
import com.mrl.pixiv.strings.migration_success
import java.io.File
import kotlin.io.copyTo
import kotlin.use

private val regex = Regex("""^(\d+)_(\d+)(\..+)?$""")

data class RequestPermissionEffect(val intentSender: IntentSender) : SideEffect

actual fun androidCheckOldData(updateState: (AppDataState.() -> AppDataState) -> Unit) {
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

actual fun androidMigrateData(
    updateState: (AppDataState.() -> AppDataState) -> Unit,
    sendEffect: (SideEffect) -> Unit,
    checkOldData: () -> Unit
) {
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
        ToastUtil.safeShortToast(RStrings.migration_success)
        return
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
        return
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
        ToastUtil.safeShortToast(RStrings.migration_success)
    } else {
        ToastUtil.safeShortToast(RStrings.migration_manually_delete)
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