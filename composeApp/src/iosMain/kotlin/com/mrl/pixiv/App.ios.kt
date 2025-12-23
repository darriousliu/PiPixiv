package com.mrl.pixiv

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.asSkiaBitmap
import coil3.Image
import coil3.asImage
import com.mrl.pixiv.common.util.RDrawables
import com.mrl.pixiv.strings.ic_error_outline_24
import org.jetbrains.compose.resources.imageResource

@Composable
actual fun getErrorImage(): Image {
    return imageResource(RDrawables.ic_error_outline_24).asSkiaBitmap().asImage()
}