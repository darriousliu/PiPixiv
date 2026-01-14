package com.mrl.pixiv.common.util

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity


val WindowInsets.Companion.isImeVisible: Boolean
    @Composable
    get() {
        val density = LocalDensity.current
        val ime = ime
        val isImeVisible = remember(ime, density) { derivedStateOf { ime.getBottom(density) > 0 } }
        return isImeVisible.value
    }