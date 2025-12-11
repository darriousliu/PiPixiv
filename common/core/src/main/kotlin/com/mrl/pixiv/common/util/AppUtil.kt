package com.mrl.pixiv.common.util

import android.app.Application
import android.content.Context
import androidx.annotation.StringRes

object AppUtil {
    lateinit var appContext: Context
        private set
    lateinit var application: Application
        private set

    lateinit var versionName: String
        private set
    var versionCode: Int = 0
        private set

    lateinit var flavor: String

    fun init(application: Application, versionName: String, versionCode: Int, flavor: String) {
        appContext = application
        this.application = application
        this.versionName = versionName
        this.versionCode = versionCode
        this.flavor = flavor
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> getSystemService(serviceName: String): T? {
        return appContext.getSystemService(serviceName) as? T
    }

    fun getString(@StringRes resId: Int): String {
        return appContext.getString(resId)
    }
}
