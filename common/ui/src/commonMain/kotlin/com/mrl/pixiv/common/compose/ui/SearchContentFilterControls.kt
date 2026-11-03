package com.mrl.pixiv.common.compose.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mrl.pixiv.common.data.AppViewMode
import com.mrl.pixiv.common.data.search.SearchArtworkType
import com.mrl.pixiv.common.data.search.SearchContentFilter
import com.mrl.pixiv.common.util.RStrings
import com.mrl.pixiv.strings.search_content_type
import com.mrl.pixiv.strings.search_type_all
import com.mrl.pixiv.strings.search_gif_only
import com.mrl.pixiv.strings.search_exclude_gif
import com.mrl.pixiv.strings.search_manga_only
import com.mrl.pixiv.strings.search_series_only
import com.mrl.pixiv.strings.search_show_r18
import com.mrl.pixiv.strings.search_r18_only
import org.jetbrains.compose.resources.stringResource

@Composable
fun SearchContentFilterControls(
    filter: SearchContentFilter,
    defaultShowR18: Boolean,
    onChange: (SearchContentFilter) -> Unit,
    modifier: Modifier = Modifier,
    mode: AppViewMode? = null,
    horizontalContentPadding: Dp = 0.dp,
) {
    Column(modifier) {
        if (mode != AppViewMode.NOVEL) {
            Text(
                stringResource(RStrings.search_content_type),
                modifier = Modifier.padding(horizontal = horizontalContentPadding),
                style = MaterialTheme.typography.titleSmall,
            )
            FlowRow(
                modifier = Modifier.padding(horizontal = horizontalContentPadding),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SearchArtworkType.entries.forEach { type ->
                    FilterChip(
                        selected = filter.artworkType == type,
                        onClick = { onChange(filter.copy(artworkType = type)) },
                        label = {
                            Text(stringResource(when (type) {
                                SearchArtworkType.ALL -> RStrings.search_type_all
                                SearchArtworkType.GIF_ONLY -> RStrings.search_gif_only
                                SearchArtworkType.EXCLUDE_GIF -> RStrings.search_exclude_gif
                                SearchArtworkType.MANGA_ONLY -> RStrings.search_manga_only
                            }))
                        },
                    )
                }
            }
        }
        if (mode != AppViewMode.ILLUST) {
            SearchFilterSwitch(
                label = stringResource(RStrings.search_series_only),
                checked = filter.seriesOnly,
                horizontalContentPadding = horizontalContentPadding,
                onCheckedChange = { onChange(filter.copy(seriesOnly = it)) },
            )
        }
        val showR18 = filter.showR18 ?: defaultShowR18
        SearchFilterSwitch(
            label = stringResource(RStrings.search_show_r18),
            checked = showR18,
            horizontalContentPadding = horizontalContentPadding,
            onCheckedChange = { onChange(filter.withR18Enabled(it)) },
        )
        if (showR18) {
            SearchFilterSwitch(
                label = stringResource(RStrings.search_r18_only),
                checked = filter.r18Only,
                horizontalContentPadding = horizontalContentPadding,
                onCheckedChange = { onChange(filter.copy(r18Only = it)) },
            )
        }
    }
}

@Composable
fun SearchFilterSwitch(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    horizontalContentPadding: Dp = 0.dp,
) {
    Row(
        modifier = modifier.fillMaxWidth()
            .padding(vertical = 4.dp)
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
            .padding(horizontal = horizontalContentPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Switch(
            checked = checked,
            onCheckedChange = null,
            modifier = Modifier.minimumInteractiveComponentSize(),
        )
    }
}
