package com.mrl.pixiv.common.util

import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File


val OLD_DOWNLOAD_DIR = "${Environment.DIRECTORY_DCIM}/PiPixiv/"
val DOWNLOAD_DIR = "${Environment.DIRECTORY_PICTURES}/PiPixiv/"

actual fun isImageExists(
    fileName: String,
    type: PictureType,
    subFolder: String?,
    fileUri: String
): Boolean {
    val context = AppUtil.appContext
    val projection = arrayOf(MediaStore.Images.Media._ID)
    val selection =
        "${MediaStore.Images.Media.DISPLAY_NAME} = ? AND ${MediaStore.Images.Media.RELATIVE_PATH} = ?"
    val downloadDir = if (subFolder != null) "$DOWNLOAD_DIR$subFolder/" else DOWNLOAD_DIR
    val selectionArgs = arrayOf(fileName + type.extension, downloadDir)

    return try {
        context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            null
        )?.use { it.count > 0 } == true
    } catch (_: Exception) {
        false
    }
}

/**
 * 获取指定文件的下载路径。
 *
 * 通过提供的文件名、图片类型以及可选的子文件夹，查询对应的文件在系统中的存储路径。
 *
 * @param fileName 要查询的文件名。
 * @param type 图片类型，包含文件的扩展名及相关信息。
 * @param subFolder 可选参数，子文件夹路径，如果存在则附加到查询路径中。
 * @return 一个 Pair，其中第一个值为路径或文件 URI 的字符串形式，第二个值为实际存储的文件路径；若未找到文件，则返回空字符串。
 */
fun getDownloadPath(
    fileName: String,
    type: PictureType,
    subFolder: String? = null
): Pair<String, String> {
    val context = AppUtil.appContext
    val projection = arrayOf(MediaStore.Images.Media._ID)
    val selection =
        "${MediaStore.Images.Media.DISPLAY_NAME} = ? AND ${MediaStore.Images.Media.RELATIVE_PATH} = ?"
    val downloadDir = if (subFolder != null) "$DOWNLOAD_DIR$subFolder/" else DOWNLOAD_DIR
    val selectionArgs = arrayOf(fileName + type.extension, downloadDir)
    val filePath = File(
        Environment.getExternalStoragePublicDirectory(""),
        "$downloadDir$fileName${type.extension}"
    ).absolutePath

    return try {
        context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val idIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val id = cursor.getLong(idIndex)
                val uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI.buildUpon()
                    .appendPath(id.toString())
                    .build()
                uri.toString() to filePath
            } else {
                "" to ""
            }
        } ?: ("" to "")
    } catch (_: Exception) {
        "" to ""
    }
}

suspend fun saveToAlbum(
    bytes: ByteArray,
    fileName: String,
    mimeType: String?,
    subFolder: String? = null,
): Pair<String, String>? = withContext(Dispatchers.IO) {
    val type = when (mimeType?.lowercase()) {
        PictureType.PNG.mimeType -> PictureType.PNG
        PictureType.JPG.mimeType, PictureType.JPEG.mimeType -> PictureType.JPG
        PictureType.GIF.mimeType -> PictureType.GIF
        else -> return@withContext null
    }

    if (isImageExists(fileName, type, subFolder)) {
        return@withContext getDownloadPath(fileName, type, subFolder)
    }

    val context = AppUtil.appContext
    val contentValues = createContentValues(fileName, type, subFolder)
    val downloadDir = if (subFolder != null) "$DOWNLOAD_DIR$subFolder/" else DOWNLOAD_DIR
    val filePath = File(
        Environment.getExternalStoragePublicDirectory(""),
        "$downloadDir$fileName${type.extension}"
    ).absolutePath

    try {
        val uri = context.contentResolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues
        )
        uri?.let {
            context.contentResolver.openOutputStream(it)?.use { out ->
                out.write(bytes)
            }
            it.toString() to filePath
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

fun File.toBitmap(): Bitmap? {
    return try {
        BitmapFactory.decodeFile(absolutePath)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

private fun createContentValues(
    fileName: String,
    type: PictureType,
    subFolder: String? = null
): ContentValues {
    val downloadDir = if (subFolder != null) "$DOWNLOAD_DIR$subFolder/" else DOWNLOAD_DIR
    return ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, fileName + type.extension)
        put(MediaStore.MediaColumns.MIME_TYPE, type.mimeType)
        put(MediaStore.MediaColumns.RELATIVE_PATH, downloadDir)
    }
}
