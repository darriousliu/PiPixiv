package com.mrl.pixiv.common.util

import platform.Foundation.NSURL

/**
 * Constructible Kotlin superclass for the Swift implementation of [PhotoUtil].
 */
open class PhotoUtilBridge : PhotoUtil {
    final override suspend fun saveToAlbum(
        fileUri: NSURL,
        callback: suspend (String?) -> Unit,
    ) {
        callback(saveToAlbumInSwift(fileUri))
    }

    /**
     * Kept separate because Swift Export 2.4.20-Beta2 cannot generate a reverse
     * bridge for an open suspend method that itself accepts a suspend function.
     */
    open suspend fun saveToAlbumInSwift(fileUri: NSURL): String? =
        error("PhotoUtilBridge must be implemented in Swift")
}
