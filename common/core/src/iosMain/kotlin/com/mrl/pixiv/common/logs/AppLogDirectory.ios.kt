package com.mrl.pixiv.common.logs

import io.github.vinceglb.filekit.PlatformFile
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSNumber
import platform.Foundation.NSURLIsExcludedFromBackupKey
import platform.Foundation.numberWithBool

@OptIn(ExperimentalForeignApi::class)
internal actual fun prepareAppLogDirectory(directory: PlatformFile): AutoCloseable {
    check(directory.nsUrl.setResourceValue(NSNumber.numberWithBool(true), forKey = NSURLIsExcludedFromBackupKey, error = null)) {
        "Unable to exclude application logs from backup"
    }
    return AutoCloseable {}
}
