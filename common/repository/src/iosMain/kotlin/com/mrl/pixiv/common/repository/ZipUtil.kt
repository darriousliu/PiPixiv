package com.mrl.pixiv.common.repository

interface ZipUtil {
    fun unzip(sourcePath: String, destinationPath: String): Boolean
}
