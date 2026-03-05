package com.mrl.pixiv.picture

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.mrl.pixiv.common.util.throttleClick
import platform.Photos.PHAuthorizationStatusAuthorized
import platform.Photos.PHAuthorizationStatusLimited
import platform.Photos.PHPhotoLibrary

@Composable
internal actual fun Modifier.clickWithPermission(onClick: () -> Unit): Modifier =
    this.throttleClick {
        PHPhotoLibrary.requestAuthorization { status ->
            if (status == PHAuthorizationStatusAuthorized || status == PHAuthorizationStatusLimited) {
                onClick()
            }
        }
    }
