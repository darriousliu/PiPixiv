package com.mrl.pixiv.common.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * 一个用于表示两种颜色状态的不可变数据类。
 *
 * @property active 激活状态下的颜色。
 * @property inactive 非激活状态下的颜色。
 *
 * 提供了一个调用运算符，用于根据布尔值判断返回激活或非激活状态的颜色。
 */
@Immutable
data class DualColor(val active: Color, val inactive: Color) {
    @Suppress("NOTHING_TO_INLINE")
    @Composable
    inline operator fun invoke(isActive: Boolean = true) = if (isActive) active else inactive
}

val lightBlue = Color(0xFF03A9F4)
val deepBlue = Color(0xFF2B7592)

private val favoriteColor = Color.Red
private val unfavoriteColor = Color.Gray
val FavoriteDualColor = DualColor(favoriteColor, unfavoriteColor)

