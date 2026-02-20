package com.mrl.pixiv.common.repository

import com.mrl.pixiv.common.mmkv.MMKVApp
import com.mrl.pixiv.common.mmkv.mmkvBool

object AppRepository : MMKVApp {
    var isSafGranted by mmkvBool(false)
}