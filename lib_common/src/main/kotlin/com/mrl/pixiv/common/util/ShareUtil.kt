package com.mrl.pixiv.common.util

import android.content.ContentValues
import android.content.Intent
import android.os.Environment
import android.provider.MediaStore
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.result.ActivityResult
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import coil3.toBitmap
import com.mrl.pixiv.common.data.Illust

object ShareUtil {
    fun createShareIntent(text: String): Intent {
        val shareIntent = Intent(Intent.ACTION_SEND)
        shareIntent.type = "text/plain"
        shareIntent.putExtra(Intent.EXTRA_TEXT, text)
        return Intent.createChooser(shareIntent, "Share")
    }


    /**
     * 生成并分享图片的方法。
     *
     * 如果本地不存在目标文件，则从指定下载地址下载图片保存到本地相册；
     * 如果文件已存在，则直接启动分享操作。
     *
     * @param index 图片索引，用于生成文件名。
     * @param downloadUrl 图片下载地址。
     * @param illust 插图信息对象，用于获取插图相关数据。
     * @param shareLauncher 用于启动分享操作的Activity结果启动器。
     */
    suspend fun createShareImage(
        index: Int,
        downloadUrl: String,
        illust: Illust,
        shareLauncher: ManagedActivityResultLauncher<Intent, ActivityResult>
    ) {
        val fileName = "${illust.id}_${index}${PictureType.PNG.extension}"
        val context = AppUtil.appContext

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
            put(
                MediaStore.MediaColumns.RELATIVE_PATH,
                "${Environment.DIRECTORY_DCIM}/${DOWNLOAD_DIR}"
            )
        }

        val uri = context.contentResolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues
        )
        if (uri != null) {
            val imageLoader = SingletonImageLoader.get(context)
            val request = ImageRequest
                .Builder(context)
                .data(downloadUrl)
                .build()
            val result = imageLoader.execute(request)
            val bitmap = result.image?.toBitmap() ?: return

            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, outputStream)
            }

            val intent = Intent(Intent.ACTION_SEND)
            intent.type = "image/*"
            intent.putExtra(Intent.EXTRA_STREAM, uri)
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            shareLauncher.launch(intent)
        }
    }
}