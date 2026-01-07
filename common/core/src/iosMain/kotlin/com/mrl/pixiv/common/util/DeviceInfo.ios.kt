package com.mrl.pixiv.common.util

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.toKString
import platform.UIKit.UIDevice
import platform.posix.uname
import platform.posix.utsname

actual object DeviceInfo {
    private val systemName = UIDevice.currentDevice.systemName
    actual val PLATFORM = systemName

    actual val VERSION: String
        get() = UIDevice.currentDevice.systemVersion

    actual val MODEL: String
        get() = UIDevice.currentDevice.model

    actual val APP_VERSION = "8.4.0"
    actual val DISPLAY_NAME by lazy {
        deviceModelMapping[deviceIdentifier] ?: MODEL
    }
}

private val deviceModelMapping by lazy {
    mapOf(
        // iPhone
        "iPhone1,1" to "iPhone",
        "iPhone1,2" to "iPhone 3G",
        "iPhone2,1" to "iPhone 3GS",
        "iPhone3,1" to "iPhone 4",
        "iPhone3,2" to "iPhone 4",
        "iPhone3,3" to "iPhone 4",
        "iPhone4,1" to "iPhone 4S",
        "iPhone4,2" to "iPhone 4S",
        "iPhone4,3" to "iPhone 4S",
        "iPhone5,1" to "iPhone 5",
        "iPhone5,2" to "iPhone 5",
        "iPhone5,3" to "iPhone 5C",
        "iPhone5,4" to "iPhone 5C",
        "iPhone6,1" to "iPhone 5S",
        "iPhone6,2" to "iPhone 5S",
        "iPhone7,2" to "iPhone 6",
        "iPhone7,1" to "iPhone 6 Plus",
        "iPhone8,1" to "iPhone 6S",
        "iPhone8,2" to "iPhone 6S Plus",
        "iPhone8,4" to "iPhone SE",
        "iPhone9,1" to "iPhone 7",
        "iPhone9,3" to "iPhone 7",
        "iPhone9,2" to "iPhone 7 Plus",
        "iPhone9,4" to "iPhone 7 Plus",
        "iPhone10,1" to "iPhone 8",
        "iPhone10,4" to "iPhone 8",
        "iPhone10,2" to "iPhone 8 Plus",
        "iPhone10,5" to "iPhone 8 Plus",
        "iPhone10,3" to "iPhone X",
        "iPhone10,6" to "iPhone X",
        "iPhone11,2" to "iPhone XS",
        "iPhone11,4" to "iPhone XS Max",
        "iPhone11,6" to "iPhone XS Max",
        "iPhone11,8" to "iPhone XR",
        "iPhone12,1" to "iPhone 11",
        "iPhone12,3" to "iPhone 11 Pro",
        "iPhone12,5" to "iPhone 11 Pro Max",
        "iPhone12,8" to "iPhone SE 2",
        "iPhone13,1" to "iPhone 12 mini",
        "iPhone13,2" to "iPhone 12",
        "iPhone13,3" to "iPhone 12 Pro",
        "iPhone13,4" to "iPhone 12 Pro Max",
        "iPhone14,4" to "iPhone 13 mini",
        "iPhone14,5" to "iPhone 13",
        "iPhone14,2" to "iPhone 13 Pro",
        "iPhone14,3" to "iPhone 13 Pro Max",
        "iPhone14,6" to "iPhone SE 3",
        "iPhone14,7" to "iPhone 14",
        "iPhone14,8" to "iPhone 14 Plus",
        "iPhone15,2" to "iPhone 14 Pro",
        "iPhone15,3" to "iPhone 14 Pro Max",
        "iPhone15,4" to "iPhone 15",
        "iPhone15,5" to "iPhone 15 Plus",
        "iPhone16,1" to "iPhone 15 Pro",
        "iPhone16,2" to "iPhone 15 Pro Max",
        "iPhone17,3" to "iPhone 16",
        "iPhone17,4" to "iPhone 16 Plus",
        "iPhone17,1" to "iPhone 16 Pro",
        "iPhone17,2" to "iPhone 16 Pro Max",
        "iPhone17,5" to "iPhone 16e",
        "iPhone18,1" to "iPhone 17 Pro",
        "iPhone18,2" to "iPhone 17 Pro Max",
        "iPhone18,3" to "iPhone 17",
        "iPhone18,4" to "iPhone Air",

        // iPad
        "iPad1,1" to "iPad",
        "iPad2,1" to "iPad 2",
        "iPad2,2" to "iPad 2",
        "iPad2,3" to "iPad 2",
        "iPad2,4" to "iPad 2",
        "iPad3,1" to "iPad 3",
        "iPad3,2" to "iPad 3",
        "iPad3,3" to "iPad 3",
        "iPad3,4" to "iPad 4",
        "iPad3,5" to "iPad 4",
        "iPad3,6" to "iPad 4",
        "iPad6,11" to "iPad 5",
        "iPad6,12" to "iPad 5",
        "iPad7,5" to "iPad 6",
        "iPad7,6" to "iPad 6",
        "iPad7,11" to "iPad 7",
        "iPad7,12" to "iPad 7",
        "iPad11,6" to "iPad 8",
        "iPad11,7" to "iPad 8",
        "iPad12,1" to "iPad 9",
        "iPad12,2" to "iPad 9",
        "iPad13,18" to "iPad 10",
        "iPad13,19" to "iPad 10",
        "iPad15,7" to "iPad (A16)",
        "iPad15,8" to "iPad (A16)",
        "iPad4,1" to "iPad Air",
        "iPad4,2" to "iPad Air",
        "iPad4,3" to "iPad Air",
        "iPad5,3" to "iPad Air 2",
        "iPad5,4" to "iPad Air 2",
        "iPad11,3" to "iPad Air 3",
        "iPad11,4" to "iPad Air 3",
        "iPad13,1" to "iPad Air 4",
        "iPad13,2" to "iPad Air 4",
        "iPad13,16" to "iPad Air 5",
        "iPad13,17" to "iPad Air 5",
        "iPad14,8" to "iPad Air 11-inch (M2)",
        "iPad14,9" to "iPad Air 11-inch (M2)",
        "iPad15,3" to "iPad Air 11-inch (M3)",
        "iPad15,4" to "iPad Air 11-inch (M3)",
        "iPad14,10" to "iPad Air 13-inch (M2)",
        "iPad14,11" to "iPad Air 13-inch (M2)",
        "iPad15,5" to "iPad Air 13-inch (M3)",
        "iPad15,6" to "iPad Air 13-inch (M3)",
        "iPad2,5" to "iPad Mini",
        "iPad2,6" to "iPad Mini",
        "iPad2,7" to "iPad Mini",
        "iPad4,4" to "iPad Mini 2",
        "iPad4,5" to "iPad Mini 2",
        "iPad4,6" to "iPad Mini 2",
        "iPad4,7" to "iPad Mini 3",
        "iPad4,8" to "iPad Mini 3",
        "iPad4,9" to "iPad Mini 3",
        "iPad5,1" to "iPad Mini 4",
        "iPad5,2" to "iPad Mini 4",
        "iPad11,1" to "iPad Mini 5",
        "iPad11,2" to "iPad Mini 5",
        "iPad14,1" to "iPad Mini 6",
        "iPad14,2" to "iPad Mini 6",
        "iPad16,1" to "iPad Mini (A17 Pro)",
        "iPad6,3" to "iPad Pro 9.7-inch",
        "iPad6,4" to "iPad Pro 9.7-inch",
        "iPad7,3" to "iPad Pro 10.5-inch",
        "iPad7,4" to "iPad Pro 10.5-inch",
        "iPad8,1" to "iPad Pro 11-inch",
        "iPad8,2" to "iPad Pro 11-inch",
        "iPad8,3" to "iPad Pro 11-inch",
        "iPad8,4" to "iPad Pro 11-inch",
        "iPad8,9" to "iPad Pro 11-inch 2",
        "iPad8,10" to "iPad Pro 11-inch 2",
        "iPad13,4" to "iPad Pro 11-inch 3",
        "iPad13,5" to "iPad Pro 11-inch 3",
        "iPad13,6" to "iPad Pro 11-inch 3",
        "iPad13,7" to "iPad Pro 11-inch 3",
        "iPad14,3" to "iPad Pro 11-inch (M2)",
        "iPad14,4" to "iPad Pro 11-inch (M2)",
        "iPad16,3" to "iPad Pro 11-inch (M4)",
        "iPad16,4" to "iPad Pro 11-inch (M4)",
        "iPad17,1" to "iPad Pro 11-inch (M5)",
        "iPad17,2" to "iPad Pro 11-inch (M5)",
        "iPad6,7" to "iPad Pro 12.9-inch",
        "iPad6,8" to "iPad Pro 12.9-inch",
        "iPad7,1" to "iPad Pro 12.9-inch 2",
        "iPad7,2" to "iPad Pro 12.9-inch 2",
        "iPad8,5" to "iPad Pro 12.9-inch 3",
        "iPad8,6" to "iPad Pro 12.9-inch 3",
        "iPad8,7" to "iPad Pro 12.9-inch 3",
        "iPad8,8" to "iPad Pro 12.9-inch 3",
        "iPad8,11" to "iPad Pro 12.9-inch 4",
        "iPad8,12" to "iPad Pro 12.9-inch 4",
        "iPad13,8" to "iPad Pro 12.9-inch 5",
        "iPad13,9" to "iPad Pro 12.9-inch 5",
        "iPad13,10" to "iPad Pro 12.9-inch 5",
        "iPad13,11" to "iPad Pro 12.9-inch 5",
        "iPad14,5" to "iPad Pro 12.9-inch (M2)",
        "iPad14,6" to "iPad Pro 12.9-inch (M2)",
        "iPad16,5" to "iPad Pro 13-inch (M4)",
        "iPad16,6" to "iPad Pro 13-inch (M4)",
        "iPad17,3" to "iPad Pro 13-inch (M5)",
        "iPad17,4" to "iPad Pro 13-inch (M5)"
    )
}

@OptIn(ExperimentalForeignApi::class)
private val deviceIdentifier: String by lazy {
    memScoped {
        val systemInfo = alloc<utsname>()
        uname(systemInfo.ptr)

        // 将 machine 字段转换为字符串
        val machineBytes = systemInfo.machine
        machineBytes.toKString()
    }
}