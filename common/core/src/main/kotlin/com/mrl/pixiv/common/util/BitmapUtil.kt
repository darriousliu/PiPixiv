package com.mrl.pixiv.common.util

import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.OutputStream


val OLD_DOWNLOAD_DIR = "${Environment.DIRECTORY_DCIM}/PiPixiv/"
val DOWNLOAD_DIR = "${Environment.DIRECTORY_PICTURES}/PiPixiv/"

enum class PictureType(
    val extension: String,
    val mimeType: String,
    val compressFormat: Bitmap.CompressFormat? = null
) {
    PNG(".png", "image/png", Bitmap.CompressFormat.PNG),
    JPG(".jpg", "image/jpeg", Bitmap.CompressFormat.JPEG),
    JPEG(".jpeg", "image/jpeg", Bitmap.CompressFormat.JPEG),
    GIF(".gif", "image/gif", null)
}

fun isImageExists(fileName: String, type: PictureType, subFolder: String? = null): Boolean {
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
    } catch (e: Exception) {
        false
    }
}

suspend fun saveToAlbum(
    bytes: ByteArray,
    fileName: String,
    mimeType: String?,
    subFolder: String? = null,
): Boolean = withContext(Dispatchers.IO) {
    val type = when (mimeType?.lowercase()) {
        PictureType.PNG.mimeType -> PictureType.PNG
        PictureType.JPG.mimeType, PictureType.JPEG.mimeType -> PictureType.JPG
        PictureType.GIF.mimeType -> PictureType.GIF
        else -> return@withContext false
    }

    if (isImageExists(fileName, type, subFolder)) {
        return@withContext true
    }

    val context = AppUtil.appContext
    val contentValues = createContentValues(fileName, type, subFolder)

    try {
        val uri = context.contentResolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues
        )
        uri?.let {
            context.contentResolver.openOutputStream(it)?.use { out ->
                out.write(bytes)
            }
            true
        } ?: false
    } catch (e: Exception) {
        e.printStackTrace()
        false
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

fun createDownloadOutputStream(
    fileName: String,
    type: PictureType,
    subFolder: String? = null
): OutputStream? {
    val context = AppUtil.appContext
    val contentValues = createContentValues(fileName, type, subFolder)
    val uri = context.contentResolver.insert(
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        contentValues
    )
    return uri?.let { context.contentResolver.openOutputStream(it) }
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
