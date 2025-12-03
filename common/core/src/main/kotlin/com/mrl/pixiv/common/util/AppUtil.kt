package com.mrl.pixiv.common.util

import android.app.Application
import android.content.Context
import androidx.annotation.StringRes

object AppUtil {
    lateinit var appContext: Context
    lateinit var application: Application

    fun init(application: Application) {
        appContext = application
        this.application = application
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> getSystemService(serviceName: String): T? {
        return appContext.getSystemService(serviceName) as? T
    }

    fun getString(@StringRes resId: Int): String {
        return appContext.getString(resId)
    }
}
