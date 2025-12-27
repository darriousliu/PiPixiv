package com.mrl.pixiv.common.util

import platform.Foundation.NSURL

interface PhotoUtil {
    suspend fun saveToAlbum(fileUri: NSURL, callback: suspend (String?) -> Unit)
}