package com.mrl.pixiv.common.compose.layout

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp

/**
 * Space from the content viewport's bottom to the floating navigation's top, including the system
 * bottom inset once. FABs add this to Scaffold's normal spacing and exclude navigationBars from
 * their Scaffold contentWindowInsets; they must not add navigationBarsPadding again.
 */
val LocalMainNavigationBottomInset = compositionLocalOf { 0.dp }

/** Adds scrollable trailing space without shrinking the viewport behind the navigation. */
@Composable
fun PaddingValues.withMainNavigationBottomInset(): PaddingValues {
    val layoutDirection = LocalLayoutDirection.current
    return PaddingValues(
        start = calculateStartPadding(layoutDirection),
        top = calculateTopPadding(),
        end = calculateEndPadding(layoutDirection),
        bottom = calculateBottomPadding() + LocalMainNavigationBottomInset.current,
    )
}
