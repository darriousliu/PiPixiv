package com.mrl.pixiv.common.util

interface ZipUtil {
    fun unzip(sourcePath: String, destinationPath: String): Boolean
}