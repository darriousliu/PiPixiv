package com.mrl.pixiv.common.analytics

import Internal.Sentry.SentryCrashMonitorTypeCPPException
import Internal.Sentry.sentrycrash_setMonitoring
import Internal.Sentry.sentrycrashcm_getActiveMonitors
import io.sentry.kotlin.multiplatform.Sentry
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.UIntVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.get
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.toKString
import kotlinx.cinterop.value
import platform.Foundation.NSBundle
import platform.Foundation.NSClassFromString
import platform.Foundation.NSProcessInfo
import platform.objc.objc_copyImageNames
import platform.posix.Dl_info
import platform.posix.RTLD_MAIN_ONLY
import platform.posix.dladdr
import platform.posix.dlsym
import platform.posix.free

@OptIn(ExperimentalForeignApi::class)
internal actual fun initializePlatformSentry(dsn: String, appVersion: String, appBuild: Int) {
    val runtimeInfo = detectIosRuntime()
    Sentry.initWithPlatformOptions { options ->
        options.setDsn(dsn)
        options.setReleaseName(formatIosSentryRelease(appVersion, appBuild))
        options.setDist(appBuild.toString())
        options.setSendDefaultPii(false)
        options.setEnableAutoSessionTracking(true)
        options.setEnableAutoBreadcrumbTracking(true)
        options.setEnableNetworkBreadcrumbs(true)
        options.setMaxBreadcrumbs(CrashBreadcrumbLimit.toULong())
        options.setEnableReportNonFullyBlockingAppHangs(false)
        options.setBeforeBreadcrumb { breadcrumb ->
            breadcrumb?.apply {
                stripHttpBreadcrumbQueryAndFragment(category, type, data) { key, value ->
                    setDataValue(value, forKey = key)
                }
            }
        }
        options.setTracesSampleRate(null)
        options.setTracePropagationTargets(emptyList<Any>())
        options.setEnableAutoPerformanceTracing(false)
        options.setEnableNetworkTracking(false)
        options.setEnableCaptureFailedRequests(false)
        options.setEnableLogs(true)
        options.sessionReplay().apply {
            setSessionSampleRate(0f)
            setOnErrorSampleRate(0f)
        }
        options.setAttachScreenshot(false)
        options.setAttachViewHierarchy(false)
        // 运行形态标签放进 initialScope：SDK 启动时异步生成的 WatchdogTermination 等事件同样能带上，
        // 不必与启动后的 configureScope 竞态。
        options.setInitialScope { scope ->
            scope?.apply {
                setTagValue(runtimeInfo.runtimeTag, forKey = "ios.runtime")
                setTagValue(runtimeInfo.isLiveContainer.toString(), forKey = "ios.livecontainer")
            }
        }
    }
    // Match Sentry KMP's C++ monitor option while keeping the Cocoa-specific privacy settings.
    // Restoring Kotlin/Native's terminate handler lets both Kotlin exception hooks run.
    val activeMonitors = sentrycrashcm_getActiveMonitors()
    if (activeMonitors and SentryCrashMonitorTypeCPPException != 0u) {
        sentrycrash_setMonitoring(activeMonitors and SentryCrashMonitorTypeCPPException.inv())
    }
}

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
private fun detectIosRuntime(): IosRuntimeInfo {
    val bundle = NSBundle.mainBundle
    val processInfo = NSProcessInfo.processInfo
    val environment = processInfo.environment
    val envKeys = environment.keys.mapNotNull { it as? String }.toSet()
    val dyldInsertLibraries = environment["DYLD_INSERT_LIBRARIES"] as? String
    val hasLiveContainerClass =
        NSClassFromString("LCSharedUtils") != null || NSClassFromString("LCTweakLoader") != null
    return classifyIosRuntime(
        bundleIdentifier = bundle.bundleIdentifier,
        bundlePath = bundle.bundlePath,
        executablePath = bundle.executablePath,
        mainExecutableImagePath = mainExecutableImagePath(),
        loadedImagePaths = loadedObjcImagePaths(),
        processName = processInfo.processName,
        environmentKeys = envKeys,
        dyldInsertLibraries = dyldInsertLibraries,
        hasLiveContainerClass = hasLiveContainerClass,
    )
}

/** 进程真实主执行文件的路径（不受 LiveContainer 改写 mainBundle 的影响）。 */
@OptIn(ExperimentalForeignApi::class)
private fun mainExecutableImagePath(): String? = memScoped {
    val header = dlsym(RTLD_MAIN_ONLY, "_mh_execute_header") ?: return@memScoped null
    val info = alloc<Dl_info>()
    if (dladdr(header, info.ptr) == 0) return@memScoped null
    info.dli_fname?.toKString()
}

/** 已加载且含 ObjC 元数据的镜像路径，LiveContainer 宿主与 LiveContainerShared.framework 都在其中。 */
@OptIn(ExperimentalForeignApi::class)
private fun loadedObjcImagePaths(): List<String> = memScoped {
    val count = alloc<UIntVar>()
    val names = objc_copyImageNames(count.ptr) ?: return@memScoped emptyList()
    try {
        (0 until count.value.toInt()).mapNotNull { index -> names[index]?.toKString() }
    } finally {
        free(names)
    }
}

internal actual fun addPlatformCrashBreadcrumb(category: String, message: String) = Unit
