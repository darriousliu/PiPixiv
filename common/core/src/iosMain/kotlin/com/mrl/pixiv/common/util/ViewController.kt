package com.mrl.pixiv.common.util

import platform.UIKit.UIApplication
import platform.UIKit.UIViewController

fun getCurrentViewController(): UIViewController? {
    val keyWindow = UIApplication.sharedApplication.keyWindow
    var rootViewController = keyWindow?.rootViewController

    while (rootViewController?.presentedViewController != null) {
        rootViewController = rootViewController.presentedViewController
    }

    return rootViewController
}