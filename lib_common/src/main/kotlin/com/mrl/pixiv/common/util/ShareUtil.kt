package com.mrl.pixiv.common.util

import android.content.Intent
import android.os.Environment
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.result.ActivityResult
import androidx.core.content.FileProvider
import androidx.core.graphics.drawable.toBitmap
import coil3.SingletonImageLoader
import coil3.asDrawable
import coil3.request.ImageRequest
import com.mrl.pixiv.common.data.Illust
import java.io.File

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
        val file = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
            .absolutePath.let {
                File(
                    joinPaths(
                        it,
                        DOWNLOAD_DIR,
                        "${illust.id}_${index}${PictureType.PNG.extension}"
                    )
                )
            }
        if (!file.isExist) {
            val imageLoader = SingletonImageLoader.get(AppUtil.appContext)
            val request = ImageRequest
                .Builder(AppUtil.appContext)
                .data(downloadUrl)
                .build()
            val result = imageLoader.execute(request)
            result.image?.asDrawable(AppUtil.appContext.resources)
                ?.toBitmap()
                ?.saveToAlbum(file.nameWithoutExtension, PictureType.PNG)
                ?: return
        }
        val uri = FileProvider.getUriForFile(
            AppUtil.appContext,
            "${AppUtil.appContext.packageName}.fileprovider",
            file
        )
        // 分享图片
        val intent = Intent(Intent.ACTION_SEND)
        intent.type = "image/*"
        intent.putExtra(Intent.EXTRA_STREAM, uri)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        shareLauncher.launch(intent)
    }
}