package com.mrl.pixiv.setting.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mrl.pixiv.common.compose.rememberThrottleClick

@Composable
fun DropDownSelector(
    expanded: Boolean,
    onClick: (() -> Unit)?,
    onDismissRequest: () -> Unit,
    current: String,
    modifier: Modifier = Modifier,
    maxValueWidth: Dp = 96.dp,
    maxLines: Int = Int.MAX_VALUE,
    items: @Composable ColumnScope.() -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val shape = MaterialTheme.shapes.small
    val value: @Composable () -> Unit = {
        val style = MaterialTheme.typography.bodyMedium
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = current,
                modifier = Modifier.widthIn(max = maxValueWidth),
                style = if (maxLines == 1) style else style.copy(
                    hyphens = Hyphens.Auto,
                    lineBreak = LineBreak.Paragraph,
                ),
                color = LocalContentColor.current,
                maxLines = maxLines,
                overflow = if (maxLines == 1) TextOverflow.Ellipsis else TextOverflow.Clip,
            )
            Icon(Icons.Rounded.ArrowDropDown, contentDescription = null)
        }
    }
    Box(modifier) {
        if (onClick == null) {
            Surface(
                shape = shape,
                color = colors.secondaryContainer,
                contentColor = colors.onSecondaryContainer,
                content = value,
            )
        } else {
            Surface(
                onClick = rememberThrottleClick(onClick = onClick),
                modifier = Modifier.semantics { role = Role.Button },
                shape = shape,
                color = colors.secondaryContainer,
                contentColor = colors.onSecondaryContainer,
                content = value,
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
