package com.mrl.pixiv.common.util

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCObjectVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import platform.Foundation.NSFileManager
import platform.Foundation.NSFileSystemFreeSize
import platform.Foundation.NSNumber
import platform.Foundation.NSURL
import platform.Foundation.NSURLVolumeAvailableCapacityForImportantUsageKey

@OptIn(ExperimentalForeignApi::class)
actual fun availableStorageBytes(path: String): Long = memScoped {
    val url = NSURL.fileURLWithPath(path)
    val capacity = alloc<ObjCObjectVar<Any?>>()
    if (url.getResourceValue(capacity.ptr, NSURLVolumeAvailableCapacityForImportantUsageKey, null)) {
        (capacity.value as? NSNumber)?.longLongValue?.let { return@memScoped it }
    }
    (NSFileManager.defaultManager.attributesOfFileSystemForPath(path, null)?.get(NSFileSystemFreeSize) as? NSNumber)
        ?.longLongValue ?: error("Unable to check available storage")
}
