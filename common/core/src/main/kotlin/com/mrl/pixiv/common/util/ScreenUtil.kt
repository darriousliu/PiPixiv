package com.mrl.pixiv.common.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalWindowInfo

@Composable
fun getScreenWidth() = LocalWindowInfo.current.containerDpSize.width

@Composable
fun getScreenHeight() = LocalWindowInfo.current.containerDpSize.height