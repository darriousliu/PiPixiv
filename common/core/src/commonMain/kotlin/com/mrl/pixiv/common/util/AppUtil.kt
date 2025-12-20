package com.mrl.pixiv.common.util

import coil3.PlatformContext
import com.mrl.pixiv.common.BuildKonfig
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.StringResource

object AppUtil {
    lateinit var appContext: PlatformContext
        internal set
    val versionName = BuildKonfig.versionName

    val versionCode = BuildKonfig.versionCode

    lateinit var flavor: String
        private set

    fun init(appContext: PlatformContext, flavor: String) {
        this.appContext = appContext
        this.flavor = flavor
    }

    fun getString(resId: StringResource, vararg args: Any): String {
        return runBlocking { org.jetbrains.compose.resources.getString(resId, *args) }
    }

    fun getString(resId: Int, vararg args: Any): String {
        return runBlocking { getPlatformString(resId, *args) }
    }
}

expect fun getPlatformString(resId: Int, vararg args: Any): String
