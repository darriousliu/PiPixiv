package com.mrl.pixiv.common.kts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.unit.dp

val Int.spaceBy: Arrangement.HorizontalOrVertical
    inline get() = Arrangement.spacedBy(this.dp)

val Float.spaceBy: Arrangement.HorizontalOrVertical
    inline get() = Arrangement.spacedBy(this.dp)