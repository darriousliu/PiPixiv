package com.mrl.pixiv.common.compose

import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.compositionLocalOf

val LocalSharedTransitionScope = compositionLocalOf<SharedTransitionScope> {
    noLocalProvidedFor("LocalSharedTransitionScope")
}

val LocalSharedKeyPrefix = compositionLocalOf {
    ""
}

private fun noLocalProvidedFor(name: String): Nothing {
    error("CompositionLocal $name not present")
}