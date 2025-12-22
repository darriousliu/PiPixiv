package com.mrl.pixiv.common.compose

import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import com.dokar.sonner.ToasterState

val LocalSharedTransitionScope = compositionLocalOf<SharedTransitionScope> {
    noLocalProvidedFor("LocalSharedTransitionScope")
}

val LocalSharedKeyPrefix = compositionLocalOf {
    ""
}

val LocalToaster = staticCompositionLocalOf<ToasterState> {
    noLocalProvidedFor("LocalToaster")
}

private fun noLocalProvidedFor(name: String): Nothing {
    error("CompositionLocal $name not present")
}