package com.mrl.pixiv.picture

import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.Manifest.permission.READ_MEDIA_IMAGES
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberPermissionState
import com.mrl.pixiv.common.util.throttleClick

@OptIn(ExperimentalPermissionsApi::class)
@Composable
internal actual fun Modifier.clickWithPermission(onClick: () -> Unit): Modifier = composed {
    val readMediaImagePermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        rememberPermissionState(READ_MEDIA_IMAGES) { success ->
            if (success) {
                onClick()
            }
        }
    } else {
        rememberPermissionState(READ_EXTERNAL_STORAGE) { success ->
            if (success) {
                onClick()
            }
        }
    }
    this.throttleClick {
        readMediaImagePermission.launchPermissionRequest()
    }
}