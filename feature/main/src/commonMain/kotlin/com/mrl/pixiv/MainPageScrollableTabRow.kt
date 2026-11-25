package com.mrl.pixiv

import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.mrl.pixiv.common.compose.IllustGridSpacing
import com.mrl.pixiv.common.compose.layout.currentPaneLayoutInfo

@Composable
internal fun MainPageScrollableTabRow(
    selectedIndex: Int,
    tabs: @Composable () -> Unit,
) {
    PrimaryScrollableTabRow(
        selectedTabIndex = selectedIndex,
        edgePadding = IllustGridSpacing.edgePadding(currentPaneLayoutInfo()),
        minTabWidth = 48.dp,
        tabs = tabs,
    )
}
