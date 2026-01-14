package com.mrl.pixiv.common.util

import coil3.PlatformContext
import com.mrl.pixiv.common.BuildKonfig
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.cacheDir
import io.github.vinceglb.filekit.databasesDir
import io.github.vinceglb.filekit.filesDir
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.StringResource

object AppUtil {
    lateinit var appContext: PlatformContext
        internal set
    val versionName = BuildKonfig.versionName

    val versionCode = BuildKonfig.versionCode

    val sentryDsn = BuildKonfig.sentryDsn

    lateinit var flavor: String
        private set

    fun init(appContext: PlatformContext, flavor: String) {
        this.appContext = appContext
        this.flavor = flavor
    }

    fun getString(resId: StringResource, vararg args: Any): String {
        return runBlocking { org.jetbrains.compose.resources.getString(resId, *args) }
    }
}

val PlatformContext.cacheDir: PlatformFile
    get() = FileKit.cacheDir

val PlatformContext.filesDir: PlatformFile
    get() = FileKit.filesDir

val PlatformContext.databasesDir: PlatformFile
    get() = FileKit.databasesDir
