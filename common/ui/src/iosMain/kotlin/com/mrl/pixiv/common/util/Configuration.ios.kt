package com.mrl.pixiv.common.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalWindowInfo
import platform.Foundation.NSNotificationCenter
import platform.UIKit.UIDevice
import platform.UIKit.UIDeviceOrientation
import platform.UIKit.UIDeviceOrientationDidChangeNotification

@Composable
actual fun currentOrientation(): Orientation {
    val windowInfo = LocalWindowInfo.current
    var orientation by remember {
        mutableStateOf(
            getOrientationFromDevice(UIDevice.currentDevice.orientation)
                ?: if (windowInfo.containerSize.width > windowInfo.containerSize.height) Orientation.LANDSCAPE else Orientation.PORTRAIT
        )
    }

    DisposableEffect(Unit) {
        UIDevice.currentDevice.beginGeneratingDeviceOrientationNotifications()
        val observer = NSNotificationCenter.defaultCenter.addObserverForName(
            name = UIDeviceOrientationDidChangeNotification,
            `object` = null,
            queue = null
        ) {
            val newOrientation = getOrientationFromDevice(UIDevice.currentDevice.orientation)
            if (newOrientation != null) {
                orientation = newOrientation
            }
        }

        onDispose {
            NSNotificationCenter.defaultCenter.removeObserver(observer)
            UIDevice.currentDevice.endGeneratingDeviceOrientationNotifications()
        }
    }

    return orientation
}

private fun getOrientationFromDevice(deviceOrientation: UIDeviceOrientation): Orientation? {
    return when (deviceOrientation) {
        UIDeviceOrientation.UIDeviceOrientationPortrait,
        UIDeviceOrientation.UIDeviceOrientationPortraitUpsideDown -> Orientation.PORTRAIT

        UIDeviceOrientation.UIDeviceOrientationLandscapeLeft,
        UIDeviceOrientation.UIDeviceOrientationLandscapeRight -> Orientation.LANDSCAPE

        else -> null
    }
}