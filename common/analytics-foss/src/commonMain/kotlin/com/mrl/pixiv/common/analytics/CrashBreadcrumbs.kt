package com.mrl.pixiv.common.analytics

fun recordCrashScreen(screen: String) = Unit

fun trackTab(screen: String, tab: Enum<*>) = Unit

fun recordCrashOperation(operation: String) = Unit
