package com.mrl.pixiv.picture

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.mrl.pixiv.common.repository.AppRepository
import com.mrl.pixiv.common.util.AppUtil
import com.mrl.pixiv.common.util.getNewDownloadDirUri
import com.mrl.pixiv.common.util.handleTreeUriGranted
import com.mrl.pixiv.common.util.throttleClick

@OptIn(ExperimentalPermissionsApi::class)
@Composable
internal actual fun Modifier.clickWithPermission(onClick: () -> Unit): Modifier = composed {
    val initialUri = remember { getNewDownloadDirUri() }
    // SAF 目录选择器 Launcher
    val dirPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            handleTreeUriGranted(AppUtil.appContext, uri)
            AppRepository.isSafGranted = true
            onClick()
        }
    }
    this.throttleClick {
        if (AppRepository.isSafGranted) {
            onClick()
        } else {
            dirPickerLauncher.launch(initialUri)
        }
    }
}
