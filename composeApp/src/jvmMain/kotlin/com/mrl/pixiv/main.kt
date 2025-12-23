package com.mrl.pixiv

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.mrl.pixiv.common.util.RStrings
import com.mrl.pixiv.strings.app_name
import org.jetbrains.compose.resources.stringResource

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = stringResource(RStrings.app_name),
    ) {
        App()
    }
}