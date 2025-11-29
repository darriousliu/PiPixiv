package com.mrl.pixiv.common.util

import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.BitmapFactory.decodeFile
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.io.OutputStream


// 下载文件夹为Pictures/PiPixiv
val DOWNLOAD_DIR = "${Environment.DIRECTORY_PICTURES}/PiPixiv/"

enum class PictureType(val extension: String) {
    PNG(".png"),
    JPG(".jpg"),
    JPEG(".jpeg"),
    GIF(".gif")
}

fun isImageExists(fileName: String, type: PictureType): Boolean {
    val context = AppUtil.appContext
    val projection = arrayOf(MediaStore.Images.Media._ID)
    val selection =
        "${MediaStore.Images.Media.DISPLAY_NAME} = ? AND ${MediaStore.Images.Media.RELATIVE_PATH} = ?"
    val selectionArgs = arrayOf(fileName + type.extension, DOWNLOAD_DIR)
    context.contentResolver.query(
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        projection,
        selection,
        selectionArgs,
        null
    )?.use { cursor ->
        return cursor.count > 0
    }
    return false
}

fun Bitmap.saveToAlbum(
    fileName: String,
    type: PictureType = PictureType.PNG,
    callback: (Boolean) -> Unit = {}
) {
    val compressFormat = when (type) {
        PictureType.PNG -> Bitmap.CompressFormat.PNG
        PictureType.JPEG -> Bitmap.CompressFormat.JPEG
        PictureType.JPG -> Bitmap.CompressFormat.JPEG
        else -> return
    }
    try {
        if (isImageExists(fileName, type)) {
            callback(true)
            return
        }
        val context = AppUtil.appContext
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName + type.extension)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/${type.name.lowercase()}")
            put(MediaStore.MediaColumns.RELATIVE_PATH, DOWNLOAD_DIR)
        }
        val uri = context.contentResolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues
        )
        if (uri != null) {
            context.contentResolver.openOutputStream(uri)?.use { out ->
                if (compress(compressFormat, 100, out)) {
                    callback(true)
                    return
                }
            }
        }
        callback(false)
    } catch (_: Exception) {
        callback(false)
    }
}

fun File.toBitmap(): Bitmap? {
    return try {
        decodeFile(absolutePath)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

fun createDownloadFile(fileName: String, type: PictureType): OutputStream? {
    val context = AppUtil.appContext
    val contentValues = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, fileName + type.extension)
        put(MediaStore.MediaColumns.MIME_TYPE, "image/${type.name.lowercase()}")
        put(MediaStore.MediaColumns.RELATIVE_PATH, DOWNLOAD_DIR)
    }
    val uri = context.contentResolver.insert(
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        contentValues
    )
    return uri?.let { context.contentResolver.openOutputStream(it) }
}