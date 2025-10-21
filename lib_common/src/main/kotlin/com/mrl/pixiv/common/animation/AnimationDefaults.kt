package com.mrl.pixiv.common.animation

import androidx.compose.animation.core.tween
import androidx.compose.ui.unit.IntOffset

const val DefaultAnimationDuration = 200
val DefaultIntOffsetAnimationSpec = tween<IntOffset>(DefaultAnimationDuration)
val DefaultFloatAnimationSpec = tween<Float>(DefaultAnimationDuration)
