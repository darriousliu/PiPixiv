package com.mrl.pixiv.common.util

sealed class Platform(open val name: String) {

    sealed class Android(open val version: Int) : Platform("android") {
        data class AndroidPhone(override val version: Int) : Android(version)
        data class AndroidPad(override val version: Int) : Android(version)
    }

    sealed class Desktop(override val name: String) : Platform(name) {
        data object Linux : Desktop("linux")
        data object Windows : Desktop("windows")
        data object MacOS : Desktop("macos")
    }

    sealed class Apple(override val name: String) : Platform(name) {
        data object IPhoneOS : Apple("ios")
        data object IPadOS : Apple("ipados")
    }
}

expect val platform: Platform

fun Platform.isAndroid(): Boolean = this is Platform.Android
fun Platform.isIOS(): Boolean = this is Platform.Apple
fun Platform.isDesktop(): Boolean = this is Platform.Desktop