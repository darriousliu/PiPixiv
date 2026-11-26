package com.mrl.pixiv

import coil3.PlatformContext
import coil3.memory.MemoryCache

private const val MEBIBYTE = 1024L * 1024L
private const val IOS_MAIN_APP_MIN_MEMORY_CACHE_BYTES = 16L * MEBIBYTE
private const val IOS_MAIN_APP_MAX_MEMORY_CACHE_BYTES = 64L * MEBIBYTE
private const val IOS_EXTENSION_MIN_MEMORY_CACHE_BYTES = 8L * MEBIBYTE
private const val IOS_EXTENSION_MAX_MEMORY_CACHE_BYTES = 24L * MEBIBYTE

internal expect fun createPlatformCoilMemoryCache(context: PlatformContext): MemoryCache

/**
 * 判断当前 iOS 进程是否为 App Extension。
 *
 * LiveContainer 的 LiveProcess.appex 会把 mainBundle 改写成被加载 App 的包，
 * 因此还需检查 dyld 第 0 个镜像（进程真实的主执行文件）路径。
 */
internal fun isIosAppExtensionProcess(bundlePath: String?, mainExecutableImagePath: String?): Boolean =
    bundlePath?.endsWith(".appex", ignoreCase = true) == true ||
        mainExecutableImagePath?.contains(".appex/", ignoreCase = true) == true

/** 根据 iOS 当前进程的可用 Jetsam 预算与运行形态计算图片内存缓存上限。 */
internal fun calculateIosMemoryCacheBytes(
    availableMemoryBytes: Long?,
    isAppExtension: Boolean,
): Long {
    val minBytes = if (isAppExtension) IOS_EXTENSION_MIN_MEMORY_CACHE_BYTES else IOS_MAIN_APP_MIN_MEMORY_CACHE_BYTES
    val maxBytes = if (isAppExtension) IOS_EXTENSION_MAX_MEMORY_CACHE_BYTES else IOS_MAIN_APP_MAX_MEMORY_CACHE_BYTES
    val budget = availableMemoryBytes?.takeIf { it > 0L } ?: return maxBytes
    val targetBytes = budget / 10L
    return targetBytes.coerceIn(minBytes, maxBytes)
}
