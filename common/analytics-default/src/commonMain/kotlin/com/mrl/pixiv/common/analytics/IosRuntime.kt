package com.mrl.pixiv.common.analytics

internal const val IOS_SENTRY_BUNDLE_ID = "com.mrl.pixiv.PiPixiv"

internal data class IosRuntimeInfo(
    val runtimeTag: String,
    val isLiveContainer: Boolean,
    val isAppExtension: Boolean,
)

internal fun formatIosSentryRelease(appVersion: String, appBuild: Int): String =
    "$IOS_SENTRY_BUNDLE_ID@$appVersion+$appBuild"

/**
 * 识别 iOS 是否运行于 LiveContainer 侧载容器或 App Extension，便于在 Sentry 中区分环境噪音。
 *
 * LiveContainer 会把 `NSBundle.mainBundle` 改写为被加载 App 自身的包（位于数据容器的
 * `Documents/Applications/` 下），因此仅凭 bundle 路径无法识别；真实宿主只能从 dyld 已加载镜像
 * （主执行文件 `LiveContainer.app/...`、`LiveProcess.appex/...`、`LiveContainerShared.framework`）中看出。
 */
internal fun classifyIosRuntime(
    bundleIdentifier: String?,
    bundlePath: String?,
    executablePath: String?,
    mainExecutableImagePath: String?,
    loadedImagePaths: List<String>,
    processName: String?,
    environmentKeys: Set<String>,
    dyldInsertLibraries: String?,
    hasLiveContainerClass: Boolean,
): IosRuntimeInfo {
    val isAppExtension = bundlePath?.endsWith(".appex", ignoreCase = true) == true ||
        executablePath?.contains(".appex/", ignoreCase = true) == true ||
        mainExecutableImagePath?.contains(".appex/", ignoreCase = true) == true

    // 正常安装的 App 包只会位于 Bundle 容器；包位于数据容器中说明是被容器类工具加载的。
    val isBundleInsideDataContainer = bundlePath?.contains("/Containers/Data/Application/", ignoreCase = true) == true
    val markers = listOfNotNull(
        bundleIdentifier,
        bundlePath,
        executablePath,
        mainExecutableImagePath,
        processName,
        dyldInsertLibraries,
    ) + loadedImagePaths
    val hasLiveContainerMarker = hasLiveContainerClass ||
        isBundleInsideDataContainer ||
        environmentKeys.any { key ->
            key.equals("LC_HOME_PATH", ignoreCase = true) ||
                key.startsWith("LIVECONTAINER_", ignoreCase = true)
        } ||
        markers.any { value ->
            value.contains("livecontainer", ignoreCase = true) ||
                value.contains("liveprocess", ignoreCase = true)
        }

    val runtimeTag = when {
        hasLiveContainerMarker && isAppExtension -> "livecontainer_extension"
        hasLiveContainerMarker -> "livecontainer"
        isAppExtension -> "app_extension"
        else -> "standard"
    }
    return IosRuntimeInfo(
        runtimeTag = runtimeTag,
        isLiveContainer = hasLiveContainerMarker,
        isAppExtension = isAppExtension,
    )
}
