package com.mrl.pixiv.common.kts

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

val Int.round: RoundedCornerShape
    inline get() = RoundedCornerShape(this.dp)

val Int.startRound: RoundedCornerShape
    inline get() = RoundedCornerShape(topStart = this.dp, bottomStart = this.dp)

val Int.endRound: RoundedCornerShape
    inline get() = RoundedCornerShape(topEnd = this.dp, bottomEnd = this.dp)

val Int.topRound: RoundedCornerShape
    inline get() = RoundedCornerShape(topStart = this.dp, topEnd = this.dp)

val Int.bottomRound: RoundedCornerShape
    inline get() = RoundedCornerShape(bottomStart = this.dp, bottomEnd = this.dp)

val Float.round: RoundedCornerShape
    inline get() = RoundedCornerShape(this.dp)

val Float.startRound: RoundedCornerShape
    inline get() = RoundedCornerShape(topStart = this.dp, bottomStart = this.dp)

val Float.endRound: RoundedCornerShape
    inline get() = RoundedCornerShape(topEnd = this.dp, bottomEnd = this.dp)

val Float.topRound: RoundedCornerShape
    inline get() = RoundedCornerShape(topStart = this.dp, topEnd = this.dp)

val Float.bottomRound: RoundedCornerShape
    inline get() = RoundedCornerShape(bottomStart = this.dp, bottomEnd = this.dp)