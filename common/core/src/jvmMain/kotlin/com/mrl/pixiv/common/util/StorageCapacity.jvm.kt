package com.mrl.pixiv.common.util

import java.io.File

actual fun availableStorageBytes(path: String): Long = File(path).usableSpace
