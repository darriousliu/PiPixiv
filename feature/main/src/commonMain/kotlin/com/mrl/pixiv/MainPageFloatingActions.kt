package com.mrl.pixiv

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mrl.pixiv.common.compose.layout.LocalMainNavigationBottomInset
import com.mrl.pixiv.common.compose.ui.ViewModeToggleButton
import com.mrl.pixiv.common.data.AppViewMode

@Composable
internal fun MainPageFloatingActions(
    currentMode: AppViewMode,
    onModeChange: (AppViewMode) -> Unit,
    modifier: Modifier = Modifier,
    backToTop: @Composable () -> Unit,
) {
    Column(
        modifier = modifier.padding(bottom = LocalMainNavigationBottomInset.current),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        backToTop()
        ViewModeToggleButton(currentMode = currentMode, onModeChange = onModeChange)
    }
}
