package com.mrl.pixiv.search.result.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mrl.pixiv.common.compose.ui.SearchFilterSwitch
import com.mrl.pixiv.common.data.search.SearchNumberRange
import com.mrl.pixiv.common.data.search.SearchRatio
import com.mrl.pixiv.common.data.search.parseSearchNumberRange
import com.mrl.pixiv.common.util.RStrings
import com.mrl.pixiv.search.SearchState.SearchFilter
import com.mrl.pixiv.search.result.SearchOptionsState
import com.mrl.pixiv.strings.all
import com.mrl.pixiv.strings.discovery_advanced_filters
import com.mrl.pixiv.strings.discovery_advanced_sort_note
import com.mrl.pixiv.strings.discovery_character_count
import com.mrl.pixiv.strings.discovery_genre
import com.mrl.pixiv.strings.discovery_height_pixels
import com.mrl.pixiv.strings.discovery_invalid_range
import com.mrl.pixiv.strings.discovery_landscape
import com.mrl.pixiv.strings.discovery_language
import com.mrl.pixiv.strings.discovery_maximum
import com.mrl.pixiv.strings.discovery_minimum
import com.mrl.pixiv.strings.discovery_original_only
import com.mrl.pixiv.strings.discovery_portrait
import com.mrl.pixiv.strings.discovery_ratio
import com.mrl.pixiv.strings.discovery_reading_minutes
import com.mrl.pixiv.strings.discovery_replaceable_only
import com.mrl.pixiv.strings.discovery_retry
import com.mrl.pixiv.strings.discovery_search_options_failed
import com.mrl.pixiv.strings.discovery_square
import com.mrl.pixiv.strings.discovery_tool
import com.mrl.pixiv.strings.discovery_width_pixels
import com.mrl.pixiv.strings.discovery_word_count
import com.mrl.pixiv.strings.discovery_word_count_languages
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun SearchAdvancedControls(
    filter: SearchFilter,
    isNovel: Boolean,
    options: SearchOptionsState,
    onChange: (SearchFilter) -> Unit,
    onValidityChanged: (Boolean) -> Unit,
    onRetry: () -> Unit,
) {
    val valid = remember(isNovel) { mutableStateMapOf<String, Boolean>() }
    LaunchedEffect(valid.values.toList()) { onValidityChanged(valid.values.all { it }) }
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                stringResource(RStrings.discovery_advanced_filters),
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                stringResource(RStrings.discovery_advanced_sort_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (isNovel) {
            val advanced = filter.novelAdvanced
            SearchRangeEditor(
                stringResource(RStrings.discovery_character_count),
                advanced.textLength,
                { onChange(filter.copy(novelAdvanced = advanced.copy(textLength = it))) },
                { valid["text"] = it })
            SearchRangeEditor(
                stringResource(RStrings.discovery_word_count),
                advanced.wordCount,
                { onChange(filter.copy(novelAdvanced = advanced.copy(wordCount = it))) },
                { valid["words"] = it })
            options.options?.novel?.wordCountSupportedLanguages?.takeIf(String::isNotBlank)?.let {
                Text(
                    stringResource(RStrings.discovery_word_count_languages, it),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            SearchRangeEditor(
                stringResource(RStrings.discovery_reading_minutes),
                advanced.readingTime,
                { onChange(filter.copy(novelAdvanced = advanced.copy(readingTime = it))) },
                { valid["time"] = it })
            val novelOptions = options.options?.novel
            SearchOptionGroup {
                SearchOptionMenu(
                    stringResource(RStrings.discovery_language), advanced.language,
                    novelOptions?.lang?.options.orEmpty().map { it.code to it.name }) {
                    onChange(filter.copy(novelAdvanced = advanced.copy(language = it)))
                }
                HorizontalDivider(
                    Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
                SearchOptionMenu(
                    stringResource(RStrings.discovery_genre), advanced.genre,
                    novelOptions?.genre?.options.orEmpty().map { it.id to it.label }) {
                    onChange(filter.copy(novelAdvanced = advanced.copy(genre = it)))
                }
            }
            Column {
                SearchFilterSwitch(
                    stringResource(RStrings.discovery_original_only),
                    advanced.originalOnly,
                    {
                        onChange(filter.copy(novelAdvanced = advanced.copy(originalOnly = it)))
                    })
                SearchFilterSwitch(
                    stringResource(RStrings.discovery_replaceable_only),
                    advanced.replaceableOnly,
                    {
                        onChange(filter.copy(novelAdvanced = advanced.copy(replaceableOnly = it)))
                    })
            }
        } else {
            val advanced = filter.illustAdvanced
            SearchRangeEditor(
                stringResource(RStrings.discovery_width_pixels),
                advanced.width,
                { onChange(filter.copy(illustAdvanced = advanced.copy(width = it))) },
                { valid["width"] = it })
            SearchRangeEditor(
                stringResource(RStrings.discovery_height_pixels),
                advanced.height,
                { onChange(filter.copy(illustAdvanced = advanced.copy(height = it))) },
                { valid["height"] = it })
            SearchOptionGroup {
                SearchOptionMenu(
                    stringResource(RStrings.discovery_ratio), advanced.ratio, listOf(
                        SearchRatio.PORTRAIT to stringResource(RStrings.discovery_portrait),
                        SearchRatio.LANDSCAPE to stringResource(RStrings.discovery_landscape),
                        SearchRatio.SQUARE to stringResource(RStrings.discovery_square),
                    )
                ) { onChange(filter.copy(illustAdvanced = advanced.copy(ratio = it))) }
                HorizontalDivider(
                    Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
                SearchOptionMenu(
                    stringResource(RStrings.discovery_tool), advanced.tool,
                    options.options?.illust?.tool?.options.orEmpty().map { it to it }) {
                    onChange(filter.copy(illustAdvanced = advanced.copy(tool = it)))
                }
                HorizontalDivider(
                    Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
                SearchOptionMenu(
                    stringResource(RStrings.discovery_language), advanced.language,
                    options.options?.illust?.lang?.options.orEmpty().map { it.code to it.name }) {
                    onChange(filter.copy(illustAdvanced = advanced.copy(language = it)))
                }
            }
        }
        if (options.loading) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
            }
        }
        if (options.failed) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                shape = MaterialTheme.shapes.medium
            ) {
                Column(Modifier.padding(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 4.dp)) {
                    Text(
                        stringResource(RStrings.discovery_search_options_failed),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TextButton(onClick = onRetry, modifier = Modifier.align(Alignment.End)) {
                        Text(stringResource(RStrings.discovery_retry))
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchRangeEditor(
    title: String,
    value: SearchNumberRange,
    onChange: (SearchNumberRange) -> Unit,
    onValidityChanged: (Boolean) -> Unit,
) {
    var min by remember { mutableStateOf(value.min?.toString().orEmpty()) }
    var max by remember { mutableStateOf(value.max?.toString().orEmpty()) }
    val parsed = parseSearchNumberRange(min, max)
    LaunchedEffect(min, max) {
        onValidityChanged(parsed != null)
        if (parsed != null && parsed != value) onChange(parsed)
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                min,
                { min = it.take(10) },
                Modifier.weight(1f),
                singleLine = true,
                label = { Text(stringResource(RStrings.discovery_minimum)) },
                isError = parsed == null,
                shape = MaterialTheme.shapes.medium,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            Text("—", color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedTextField(
                max,
                { max = it.take(10) },
                Modifier.weight(1f),
                singleLine = true,
                label = { Text(stringResource(RStrings.discovery_maximum)) },
                isError = parsed == null,
                shape = MaterialTheme.shapes.medium,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
        }
        if (parsed == null) Text(
            stringResource(RStrings.discovery_invalid_range),
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error
        )
    }
}

@Composable
private fun SearchOptionGroup(content: @Composable () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = MaterialTheme.shapes.large
    ) {
        Column(Modifier.fillMaxWidth()) { content() }
    }
}

@Composable
private fun <T> SearchOptionMenu(
    label: String,
    selected: T?,
    choices: List<Pair<T, String>>,
    onSelect: (T?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Row(
            Modifier.fillMaxWidth().clickable { expanded = true }.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, Modifier.weight(0.8f), style = MaterialTheme.typography.bodyMedium)
            Text(
                choices.firstOrNull { it.first == selected }?.second
                    ?: stringResource(RStrings.all),
                Modifier.weight(1.2f), style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.End,
                maxLines = 3, overflow = TextOverflow.Ellipsis,
            )
            Icon(
                Icons.Rounded.ExpandMore,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(RStrings.all)) },
                onClick = { onSelect(null); expanded = false })
            choices.forEach { (value, text) ->
                DropdownMenuItem(
                    text = { Text(text) },
                    onClick = { onSelect(value); expanded = false })
            }
        }
    }
}
