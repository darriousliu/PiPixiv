@file:Suppress("NOTHING_TO_INLINE")

package com.mrl.pixiv.common.kts

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

val Int.HSpacer
    @Composable
    inline get() = Spacer(modifier = Modifier.width(this.dp))

val Int.VSpacer
    @Composable
    inline get() = Spacer(modifier = Modifier.height(this.dp))

val Float.HSpacer
    @Composable
    inline get() = Spacer(modifier = Modifier.width(this.dp))

val Float.VSpacer
    @Composable
    inline get() = Spacer(modifier = Modifier.height(this.dp))


