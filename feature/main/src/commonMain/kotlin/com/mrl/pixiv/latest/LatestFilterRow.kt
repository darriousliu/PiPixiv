package com.mrl.pixiv.latest

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mrl.pixiv.common.compose.IllustGridSpacing
import com.mrl.pixiv.common.compose.layout.currentPaneLayoutInfo
import com.mrl.pixiv.common.data.Restrict
import com.mrl.pixiv.common.util.RStrings
import com.mrl.pixiv.strings.all
import com.mrl.pixiv.strings.discovery_collection_filters
import com.mrl.pixiv.strings.word_private
import com.mrl.pixiv.strings.word_public
import org.jetbrains.compose.resources.stringResource

private val restrictOptions = listOf(Restrict.ALL, Restrict.PUBLIC, Restrict.PRIVATE)

@Composable
internal fun LatestFilterRow(
    selected: Restrict,
    onSelect: (Restrict) -> Unit,
    modifier: Modifier = Modifier,
    showAll: Boolean = false,
    showPrivate: Boolean = true,
    onOpenFilters: (() -> Unit)? = null,
) {
    BoxWithConstraints(
        modifier = modifier.fillMaxWidth().padding(
            horizontal = IllustGridSpacing.edgePadding(currentPaneLayoutInfo()),
            vertical = 8.dp,
        ),
    ) {
        val chipAreaWidth = if (onOpenFilters == null) maxWidth else (maxWidth - 48.dp).coerceAtLeast(0.dp)
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Row(
                modifier = Modifier.widthIn(max = chipAreaWidth).horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                restrictOptions.forEach { option ->
                    if (option == Restrict.ALL && !showAll || option == Restrict.PRIVATE && !showPrivate) {
                        return@forEach
                    }
                    val label = when (option) {
                        Restrict.ALL -> RStrings.all
                        Restrict.PUBLIC -> RStrings.word_public
                        Restrict.PRIVATE -> RStrings.word_private
                    }
                    FilterChip(
                        selected = selected == option,
                        onClick = { onSelect(option) },
                        label = { Text(stringResource(label)) },
                        shape = CircleShape,
                        leadingIcon = if (selected == option) {
                            { Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                        } else null,
                    )
                }
            }
            if (onOpenFilters != null) {
                Spacer(Modifier.weight(1f))
                IconButton(
                    onClick = onOpenFilters,
                    modifier = Modifier.size(48.dp),
                    shapes = IconButtonDefaults.shapes(),
                ) {
                    Icon(Icons.Rounded.FilterList, stringResource(RStrings.discovery_collection_filters))
                }
            }
        }
    }
}
