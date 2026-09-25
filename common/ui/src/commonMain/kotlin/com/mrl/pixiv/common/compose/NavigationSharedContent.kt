package com.mrl.pixiv.common.compose

import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.SharedTransitionScope.SharedContentConfig
import androidx.compose.animation.SharedTransitionScope.SharedContentState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState

/** 允许导航场景控制共享元素动画，使内容始终位于预测性返回卡片内部。 */
val LocalNavigationSharedContentEnabled = compositionLocalOf { true }

@Composable
fun SharedTransitionScope.rememberNavigationSharedContentState(key: Any): SharedContentState {
    val enabled = rememberUpdatedState(LocalNavigationSharedContentEnabled.current)
    val config = remember {
        object : SharedContentConfig {
            override val SharedContentState.isEnabled: Boolean
                get() = enabled.value

            // 手势可能打断正在运行的共享元素动画，因此也要停用该动画，
            // 避免叠加层中的内容脱离导航卡片的变换和裁剪范围。
            override val shouldKeepEnabledForOngoingAnimation: Boolean
                get() = enabled.value
        }
    }
    return rememberSharedContentState(key, config)
}
