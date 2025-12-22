package com.mrl.pixiv.common.util

import coil3.request.ImageRequest

expect fun ImageRequest.Builder.allowRgb565(enable: Boolean): ImageRequest.Builder