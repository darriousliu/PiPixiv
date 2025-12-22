package com.mrl.pixiv.common.util

import coil3.request.ImageRequest

actual fun ImageRequest.Builder.allowRgb565(enable: Boolean): ImageRequest.Builder {
    return this
}