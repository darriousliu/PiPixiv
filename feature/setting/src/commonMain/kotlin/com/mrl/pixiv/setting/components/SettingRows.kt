package com.mrl.pixiv.setting.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
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
@Suppress("DEPRECATION")
internal fun SettingRow(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    supportingContent: (@Composable () -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null,
) {
    val shape = MaterialTheme.shapes.large
    val colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    val headline: @Composable () -> Unit = { Text(title) }
    val leading: @Composable () -> Unit = { Icon(icon, contentDescription = null) }
    if (onClick == null) {
        // The shared Material 3 version exposes only this overload for non-clickable rows.
        ListItem(
            headlineContent = headline,
            modifier = modifier.clip(shape),
            supportingContent = supportingContent,
            leadingContent = leading,
            trailingContent = trailingContent,
            colors = colors,
        )
    } else {
        ListItem(
            onClick = onClick,
            modifier = modifier,
            shapes = ListItemDefaults.shapes(shape = shape),
            colors = colors,
            leadingContent = leading,
            supportingContent = supportingContent,
            trailingContent = trailingContent,
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
    BoxWithConstraints(modifier) {
        val stackValue = maxWidth < 350.dp * LocalDensity.current.fontScale
        val valueWidth = if (stackValue) {
            (maxWidth - 124.dp).coerceAtLeast(0.dp)
        } else {
            (maxWidth * 0.34f).coerceAtMost(176.dp)
        }
        val choice: @Composable () -> Unit = {
            val colors = MaterialTheme.colorScheme
            Box {
                Surface(
                    onClick = rememberThrottleClick { expanded = !expanded },
                    modifier = Modifier.semantics { role = Role.Button },
                    shape = MaterialTheme.shapes.small,
                    color = colors.secondaryContainer,
                    contentColor = colors.onSecondaryContainer,
                ) {
                    SettingChoiceValue(
                        value = optionLabel(selected),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        maxValueWidth = valueWidth,
                        contentColor = LocalContentColor.current,
                    )
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    containerColor = colors.surfaceContainerHigh,
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
        }
        SettingRow(
            title = title,
            icon = icon,
            supportingContent = if (stackValue) choice else null,
            trailingContent = if (stackValue) null else choice,
        )
    }
}

@Composable
internal fun SettingChoiceValue(
    value: String,
    modifier: Modifier = Modifier,
    maxLines: Int = Int.MAX_VALUE,
    maxValueWidth: Dp = 96.dp,
    contentColor: Color? = null,
) {
    val style = MaterialTheme.typography.bodyMedium
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = value,
            modifier = Modifier.widthIn(max = maxValueWidth),
            style = if (maxLines == 1) style else style.copy(
                hyphens = Hyphens.Auto,
                lineBreak = LineBreak.Paragraph,
            ),
            color = contentColor ?: MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = maxLines,
            overflow = if (maxLines == 1) TextOverflow.Ellipsis else TextOverflow.Clip,
        )
        Icon(
            imageVector = Icons.Rounded.ArrowDropDown,
            contentDescription = null,
            tint = contentColor ?: LocalContentColor.current,
        )
    }
}
