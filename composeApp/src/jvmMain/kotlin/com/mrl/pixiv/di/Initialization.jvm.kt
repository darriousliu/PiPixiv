package com.mrl.pixiv.di

import com.ctrip.flight.mmkv.initialize
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.absolutePath
import io.github.vinceglb.filekit.div
import io.github.vinceglb.filekit.filesDir

actual fun initializeMMKV() {
    initialize((FileKit.filesDir / "mmkv").absolutePath())
}