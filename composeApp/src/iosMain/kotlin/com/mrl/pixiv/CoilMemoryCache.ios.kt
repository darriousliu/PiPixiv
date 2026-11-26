package com.mrl.pixiv

import co.touchlab.kermit.Logger
import coil3.PlatformContext
import coil3.memory.MemoryCache
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.UIntVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.sizeOf
import kotlinx.cinterop.toKString
import kotlinx.cinterop.value
import platform.Foundation.NSBundle
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.UIKit.UIApplicationDidEnterBackgroundNotification
import platform.UIKit.UIApplicationDidReceiveMemoryWarningNotification
import platform.darwin.DISPATCH_MEMORYPRESSURE_CRITICAL
import platform.darwin.DISPATCH_MEMORYPRESSURE_WARN
import platform.darwin.DISPATCH_SOURCE_TYPE_MEMORYPRESSURE
import platform.darwin.KERN_SUCCESS
import platform.darwin.NSObjectProtocol
import platform.darwin.TASK_VM_INFO
import platform.darwin.TASK_VM_INFO_REV4_COUNT
import platform.darwin.dispatch_get_main_queue
import platform.darwin.dispatch_resume
import platform.darwin.dispatch_source_create
import platform.darwin.dispatch_source_get_data
import platform.darwin.dispatch_source_set_event_handler
import platform.darwin.dispatch_source_t
import platform.darwin.mach_task_self_
import platform.darwin.task_info
import platform.darwin.task_vm_info_data_t
import platform.posix.Dl_info
import platform.posix.RTLD_MAIN_ONLY
import platform.posix.dladdr
import platform.posix.dlsym

private val memoryWarningObservers = mutableListOf<NSObjectProtocol>()

// 需持有强引用，否则 dispatch source 会被释放而停止回调。
private var memoryPressureSource: dispatch_source_t = null

@OptIn(ExperimentalForeignApi::class)
internal actual fun createPlatformCoilMemoryCache(context: PlatformContext): MemoryCache {
    val isAppExtension = isIosAppExtensionProcess(
        bundlePath = NSBundle.mainBundle.bundlePath,
        mainExecutableImagePath = mainExecutableImagePath(),
    )
    val maxBytes = calculateIosMemoryCacheBytes(
        availableMemoryBytes = iosAvailableMemoryBytes(),
        isAppExtension = isAppExtension,
    )
    val cache = MemoryCache.Builder()
        .maxSizeBytes(maxBytes)
        .build()
    registerIosMemoryPressureObservers(cache)
    return cache
}

/** 进程真实主执行文件的路径（LiveProcess.appex 中 mainBundle 会被改写，不可依赖）。 */
@OptIn(ExperimentalForeignApi::class)
private fun mainExecutableImagePath(): String? = memScoped {
    val header = dlsym(RTLD_MAIN_ONLY, "_mh_execute_header") ?: return@memScoped null
    val info = alloc<Dl_info>()
    if (dladdr(header, info.ptr) == 0) return@memScoped null
    info.dli_fname?.toKString()
}

@OptIn(ExperimentalForeignApi::class)
private fun iosAvailableMemoryBytes(): Long? = memScoped {
    val info = alloc<task_vm_info_data_t>()
    val count = alloc<UIntVar>()
    count.value = (sizeOf<task_vm_info_data_t>() / Int.SIZE_BYTES).toUInt()
    val result = task_info(mach_task_self_, TASK_VM_INFO.toUInt(), info.ptr.reinterpret(), count.ptr)
    if (result != KERN_SUCCESS || count.value < TASK_VM_INFO_REV4_COUNT) return@memScoped null
    info.limit_bytes_remaining.takeIf { it > 0uL }?.toLong()
}

private fun registerIosMemoryPressureObservers(cache: MemoryCache) {
    if (memoryWarningObservers.isNotEmpty()) return
    val center = NSNotificationCenter.defaultCenter
    memoryWarningObservers += center.addObserverForName(
        name = UIApplicationDidReceiveMemoryWarningNotification,
        `object` = null,
        queue = NSOperationQueue.mainQueue,
    ) { _ ->
        cache.clear()
        Logger.w(tag = "CoilMemoryCache") { "收到系统内存警告，已清空图片内存缓存" }
    }
    memoryWarningObservers += center.addObserverForName(
        name = UIApplicationDidEnterBackgroundNotification,
        `object` = null,
        queue = NSOperationQueue.mainQueue,
    ) { _ ->
        cache.trimToSize(cache.maxSize / 4L)
    }
    registerMemoryPressureSource(cache)
}

/** UIKit 内存警告在扩展等进程中未必会投递，额外监听 libdispatch 的内存压力事件兜底。 */
@OptIn(ExperimentalForeignApi::class)
private fun registerMemoryPressureSource(cache: MemoryCache) {
    val warnMask = DISPATCH_MEMORYPRESSURE_WARN.toULong()
    val criticalMask = DISPATCH_MEMORYPRESSURE_CRITICAL.toULong()
    val source = dispatch_source_create(
        DISPATCH_SOURCE_TYPE_MEMORYPRESSURE,
        0u,
        warnMask or criticalMask,
        dispatch_get_main_queue(),
    ) ?: return
    dispatch_source_set_event_handler(source) {
        val pressure = dispatch_source_get_data(source)
        if (pressure and criticalMask != 0uL) {
            cache.clear()
            Logger.w(tag = "CoilMemoryCache") { "内存压力进入 critical，已清空图片内存缓存" }
        } else if (pressure and warnMask != 0uL) {
            cache.trimToSize(cache.maxSize / 2L)
        }
    }
    dispatch_resume(source)
    memoryPressureSource = source
}
