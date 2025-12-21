package com.mrl.pixiv.common.util

expect object DeviceInfo {
    val PLATFORM: String
    val VERSION: String
    val MODEL: String
    val APP_VERSION: String
}