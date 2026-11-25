package com.mrl.pixiv.setting.components

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.dp
import com.mrl.pixiv.common.compose.rememberThrottleClick

@Composable
internal fun SettingSectionTitle(title: String) {
    Text(
        text = title,
        modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 8.dp),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
    )
}

@Composable
internal fun SettingRow(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    supportingContent: (@Composable () -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null,
) {
    val shapes = ListItemDefaults.shapes(shape = MaterialTheme.shapes.large)
    val colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    val headline: @Composable () -> Unit = { Text(title) }
    val leading: @Composable () -> Unit = { Icon(icon, contentDescription = null) }
    if (onClick == null) {
        ListItem(
            modifier = modifier,
            leadingContent = leading,
            trailingContent = trailingContent,
            supportingContent = supportingContent,
            verticalAlignment = Alignment.CenterVertically,
            shapes = shapes,
            colors = colors,
            content = headline,
        )
    } else {
        ListItem(
            onClick = onClick,
            modifier = modifier,
            leadingContent = leading,
            trailingContent = trailingContent,
            supportingContent = supportingContent,
            verticalAlignment = Alignment.CenterVertically,
            shapes = shapes,
            colors = colors,
            content = headline,
        )
    }
}

@Composable
internal fun SettingSwitchRow(
    title: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
) {
    SettingRow(
        title = title,
        icon = icon,
        modifier = modifier.semantics {
            role = Role.Switch
            toggleableState = ToggleableState(checked)
        },
        onClick = { onCheckedChange(!checked) },
        supportingContent = supportingText?.let { text -> { Text(text) } },
        trailingContent = { Switch(checked = checked, onCheckedChange = null) },
    )
}

@Composable
internal fun <T> SettingChoiceRow(
    title: String,
    icon: ImageVector,
    selected: T,
    options: List<T>,
    onSelectionChange: (T) -> Unit,
    optionLabel: @Composable (T) -> String,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    SettingDropdownRow(
        title = title,
        icon = icon,
        current = optionLabel(selected),
        expanded = expanded,
        onClick = { expanded = !expanded },
        onDismissRequest = { expanded = false },
        modifier = modifier,
    ) {
        options.forEach { option ->
            DropdownMenuItem(
                text = { Text(optionLabel(option)) },
                trailingIcon = if (option == selected) {
                    { Icon(Icons.Rounded.Check, contentDescription = null) }
                } else {
                    null
                },
                onClick = {
                    onSelectionChange(option)
                    expanded = false
                },
            )
        }
    }
}

@Composable
internal fun SettingDropdownRow(
    title: String,
    icon: ImageVector,
    current: String,
    expanded: Boolean,
    onClick: () -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    clickWholeRow: Boolean = false,
    maxValueLines: Int = Int.MAX_VALUE,
    items: @Composable ColumnScope.() -> Unit,
) {
    BoxWithConstraints(modifier) {
        val stackValue = maxWidth < 350.dp * LocalDensity.current.fontScale
        val valueWidth = if (stackValue) {
            (maxWidth - 124.dp).coerceAtLeast(0.dp)
        } else {
            (maxWidth * 0.34f).coerceAtMost(176.dp)
        }
        val choice: @Composable () -> Unit = {
            DropDownSelector(
                expanded = expanded,
                onClick = if (clickWholeRow) null else onClick,
                onDismissRequest = onDismissRequest,
                current = current,
                maxValueWidth = valueWidth,
                maxLines = maxValueLines,
                items = items,
            )
        }
        SettingRow(
            title = title,
            icon = icon,
            onClick = if (clickWholeRow) rememberThrottleClick(onClick = onClick) else null,
            supportingContent = if (stackValue) choice else null,
            trailingContent = if (stackValue) null else choice,
        )
    }
}
