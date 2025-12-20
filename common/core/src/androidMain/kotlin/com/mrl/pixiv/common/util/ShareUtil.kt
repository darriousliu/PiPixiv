package com.mrl.pixiv.common.util

import android.content.Intent
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.result.ActivityResult
import androidx.core.net.toUri

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
     * @param imagePath 图片文件路径。
     * @param shareLauncher 用于启动分享操作的Activity结果启动器。
     */
    fun createShareImage(
        imagePath: String,
        shareLauncher: ManagedActivityResultLauncher<Intent, ActivityResult>
    ) {
        val uri = imagePath.toUri()
        val intent = Intent(Intent.ACTION_SEND)
        intent.type = "image/*"
        intent.putExtra(Intent.EXTRA_STREAM, uri)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        shareLauncher.launch(intent)
    }
}