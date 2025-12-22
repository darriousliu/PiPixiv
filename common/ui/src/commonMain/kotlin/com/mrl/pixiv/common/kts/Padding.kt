package com.mrl.pixiv.common.kts

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.unit.dp

val Int.padding: PaddingValues
    inline get() = PaddingValues(this.dp)

val Int.hPadding: PaddingValues
    inline get() = PaddingValues(horizontal = this.dp)

val Int.vPadding: PaddingValues
    inline get() = PaddingValues(vertical = this.dp)

val Float.padding: PaddingValues
    inline get() = PaddingValues(this.dp)

val Float.hPadding: PaddingValues
    inline get() = PaddingValues(horizontal = this.dp)

val Float.vPadding: PaddingValues
    inline get() = PaddingValues(vertical = this.dp)