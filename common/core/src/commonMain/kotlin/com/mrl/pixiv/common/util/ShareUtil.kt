package com.mrl.pixiv.common.util

expect object ShareUtil {
    fun shareText(text: String)

    /**
     * 生成并分享图片的方法。
     *
     * 如果本地不存在目标文件，则从指定下载地址下载图片保存到本地相册；
     * 如果文件已存在，则直接启动分享操作。
     *
     * @param imageUri 图片文件URI。
     */
    fun shareImage(imageUri: String)
}

