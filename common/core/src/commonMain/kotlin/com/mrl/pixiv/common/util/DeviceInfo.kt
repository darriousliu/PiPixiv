package com.mrl.pixiv.common.util

expect object DeviceInfo {
    val PLATFORM: String
    val VERSION: String
    val MODEL: String
    val APP_VERSION: String
    val DISPLAY_NAME: String
    fun appLogDetails(): AppLogDeviceDetails
}

data class AppLogDeviceDetails(
    val platform: String,
    val osVersion: String,
    val model: String?,
)
