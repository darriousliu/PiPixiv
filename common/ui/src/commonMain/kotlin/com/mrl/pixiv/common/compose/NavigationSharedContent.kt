package com.mrl.pixiv.common.compose

import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.SharedTransitionScope.SharedContentConfig
import androidx.compose.animation.SharedTransitionScope.SharedContentState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState

/** Allows a navigation scene to keep its content inside the predictive-back card. */
val LocalNavigationSharedContentEnabled = compositionLocalOf { true }

@Composable
fun SharedTransitionScope.rememberNavigationSharedContentState(key: Any): SharedContentState {
    val enabled = rememberUpdatedState(LocalNavigationSharedContentEnabled.current)
    val config = remember {
        object : SharedContentConfig {
            override val SharedContentState.isEnabled: Boolean
                get() = enabled.value

            // A gesture can interrupt an existing shared transition. Disable that animation too,
            // otherwise its overlay would escape the navigation card's transform and clipping.
            override val shouldKeepEnabledForOngoingAnimation: Boolean
                get() = enabled.value
        }
    }
    return rememberSharedContentState(key, config)
}
