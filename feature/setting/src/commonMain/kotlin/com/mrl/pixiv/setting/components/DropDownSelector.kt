package com.mrl.pixiv.setting.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mrl.pixiv.common.compose.rememberThrottleClick

@Composable
fun DropDownSelector(
    expanded: Boolean,
    onClick: () -> Unit,
    onDismissRequest: () -> Unit,
    current: String,
    modifier: Modifier = Modifier,
    maxValueWidth: Dp = 96.dp,
    items: @Composable ColumnScope.() -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Box(modifier) {
        Surface(
            onClick = rememberThrottleClick(onClick = onClick),
            modifier = Modifier.semantics { role = Role.Button },
            shape = MaterialTheme.shapes.small,
            color = colors.secondaryContainer,
            contentColor = colors.onSecondaryContainer,
        ) {
            SettingChoiceValue(
                value = current,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                maxValueWidth = maxValueWidth,
                contentColor = LocalContentColor.current,
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = onDismissRequest,
            containerColor = colors.surfaceContainerHigh,
            content = items,
        )
    }
}
