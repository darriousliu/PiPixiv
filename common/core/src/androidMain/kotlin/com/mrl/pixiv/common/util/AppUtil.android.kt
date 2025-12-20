package com.mrl.pixiv.common.util

@Suppress("UNCHECKED_CAST")
fun <T> AppUtil.getSystemService(serviceName: String): T? {
    return appContext.getSystemService(serviceName) as? T
}

actual fun getPlatformString(resId: Int, vararg args: Any): String {
    return AppUtil.appContext.getString(resId, *args)
}