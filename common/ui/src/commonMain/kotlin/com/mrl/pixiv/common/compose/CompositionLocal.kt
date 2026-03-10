package com.mrl.pixiv.common.compose

import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.input.key.KeyEvent
import com.dokar.sonner.ToasterState
import kotlinx.coroutines.flow.SharedFlow

val LocalSharedTransitionScope = compositionLocalOf<SharedTransitionScope> {
    noLocalProvidedFor("LocalSharedTransitionScope")
}

val LocalSharedKeyPrefix = compositionLocalOf {
    ""
}

val LocalToaster = staticCompositionLocalOf<ToasterState> {
    noLocalProvidedFor("LocalToaster")
}

val LocalKeyEventFlow = staticCompositionLocalOf<SharedFlow<KeyEvent>> {
    noLocalProvidedFor("LocalKeyEventFlow")
}

private fun noLocalProvidedFor(name: String): Nothing {
    error("CompositionLocal $name not present")
}