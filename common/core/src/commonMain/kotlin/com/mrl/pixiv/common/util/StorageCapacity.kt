package com.mrl.pixiv.common.util

/** Returns the available capacity of the volume containing an existing directory. */
expect fun availableStorageBytes(path: String): Long
