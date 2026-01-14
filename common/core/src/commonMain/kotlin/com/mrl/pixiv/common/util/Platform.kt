package com.mrl.pixiv.common.util

enum class Platform {
    ANDROID,
    IOS,
    DESKTOP
}

expect val platform: Platform

fun Platform.isAndroid(): Boolean = this == Platform.ANDROID
fun Platform.isIOS(): Boolean = this == Platform.IOS
fun Platform.isDesktop(): Boolean = this == Platform.DESKTOP