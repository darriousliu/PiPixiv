package com.mrl.pixiv.common.util

import coil3.request.ImageRequest
import coil3.request.allowRgb565

actual fun ImageRequest.Builder.allowRgb565(enable: Boolean): ImageRequest.Builder {
    return this.allowRgb565(enable)
}