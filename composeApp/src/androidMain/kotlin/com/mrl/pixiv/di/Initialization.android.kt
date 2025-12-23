package com.mrl.pixiv.di

import com.mrl.pixiv.common.util.AppUtil
import com.tencent.mmkv.MMKV

actual fun initializeMMKV() {
    MMKV.initialize(AppUtil.appContext)
}