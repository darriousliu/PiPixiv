package com.mrl.pixiv.search.result.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.unit.dp
import com.mrl.pixiv.common.data.search.SearchNumberRange
import com.mrl.pixiv.common.data.search.SearchRatio
import com.mrl.pixiv.common.data.search.parseSearchNumberRange
import com.mrl.pixiv.common.util.RStrings
import com.mrl.pixiv.search.SearchState.SearchFilter
import com.mrl.pixiv.search.result.SearchOptionsState
import com.mrl.pixiv.strings.*
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
    val valid = remember { mutableStateMapOf<String, Boolean>() }
    LaunchedEffect(valid.values.toList()) { onValidityChanged(valid.values.all { it }) }
    Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(RStrings.discovery_advanced_filters), style = MaterialTheme.typography.titleMedium)
        Text(stringResource(RStrings.discovery_advanced_sort_note), style = MaterialTheme.typography.bodySmall)
        if (isNovel) {
            val advanced = filter.novelAdvanced
            SearchRangeEditor(stringResource(RStrings.discovery_character_count), advanced.textLength,
                { onChange(filter.copy(novelAdvanced = advanced.copy(textLength = it))) }, { valid["text"] = it })
            SearchRangeEditor(stringResource(RStrings.discovery_word_count), advanced.wordCount,
                { onChange(filter.copy(novelAdvanced = advanced.copy(wordCount = it))) }, { valid["words"] = it })
            options.options?.novel?.wordCountSupportedLanguages?.takeIf(String::isNotBlank)?.let {
                Text(stringResource(RStrings.discovery_word_count_languages, it), style = MaterialTheme.typography.bodySmall)
            }
            SearchRangeEditor(stringResource(RStrings.discovery_reading_minutes), advanced.readingTime,
                { onChange(filter.copy(novelAdvanced = advanced.copy(readingTime = it))) }, { valid["time"] = it })
            SearchToggle(stringResource(RStrings.discovery_original_only), advanced.originalOnly) {
                onChange(filter.copy(novelAdvanced = advanced.copy(originalOnly = it)))
            }
            SearchToggle(stringResource(RStrings.discovery_replaceable_only), advanced.replaceableOnly) {
                onChange(filter.copy(novelAdvanced = advanced.copy(replaceableOnly = it)))
            }
            val novelOptions = options.options?.novel
            SearchOptionMenu(stringResource(RStrings.discovery_language), advanced.language,
                novelOptions?.lang?.options.orEmpty().map { it.code to it.name }) {
                onChange(filter.copy(novelAdvanced = advanced.copy(language = it)))
            }
            SearchOptionMenu(stringResource(RStrings.discovery_genre), advanced.genre,
                novelOptions?.genre?.options.orEmpty().map { it.id to it.label }) {
                onChange(filter.copy(novelAdvanced = advanced.copy(genre = it)))
            }
        } else {
            val advanced = filter.illustAdvanced
            SearchRangeEditor(stringResource(RStrings.discovery_width_pixels), advanced.width,
                { onChange(filter.copy(illustAdvanced = advanced.copy(width = it))) }, { valid["width"] = it })
            SearchRangeEditor(stringResource(RStrings.discovery_height_pixels), advanced.height,
                { onChange(filter.copy(illustAdvanced = advanced.copy(height = it))) }, { valid["height"] = it })
            SearchOptionMenu(stringResource(RStrings.discovery_ratio), advanced.ratio, listOf(
                SearchRatio.PORTRAIT to stringResource(RStrings.discovery_portrait),
                SearchRatio.LANDSCAPE to stringResource(RStrings.discovery_landscape),
                SearchRatio.SQUARE to stringResource(RStrings.discovery_square),
            )) { onChange(filter.copy(illustAdvanced = advanced.copy(ratio = it))) }
            SearchOptionMenu(stringResource(RStrings.discovery_tool), advanced.tool,
                options.options?.illust?.tool?.options.orEmpty().map { it to it }) {
                onChange(filter.copy(illustAdvanced = advanced.copy(tool = it)))
            }
            SearchOptionMenu(stringResource(RStrings.discovery_language), advanced.language,
                options.options?.illust?.lang?.options.orEmpty().map { it.code to it.name }) {
                onChange(filter.copy(illustAdvanced = advanced.copy(language = it)))
            }
        }
        if (options.loading) CircularProgressIndicator()
        if (options.failed) {
            Text(stringResource(RStrings.discovery_search_options_failed), style = MaterialTheme.typography.bodySmall)
            TextButton(onClick = onRetry) { Text(stringResource(RStrings.discovery_retry)) }
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
    Text(title, style = MaterialTheme.typography.labelLarge)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(min, { min = it.take(10) }, Modifier.weight(1f), singleLine = true,
            label = { Text(stringResource(RStrings.discovery_minimum)) }, isError = parsed == null,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
        OutlinedTextField(max, { max = it.take(10) }, Modifier.weight(1f), singleLine = true,
            label = { Text(stringResource(RStrings.discovery_maximum)) }, isError = parsed == null,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
    }
    if (parsed == null) Text(stringResource(RStrings.discovery_invalid_range), color = MaterialTheme.colorScheme.error)
}

@Composable
private fun SearchToggle(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked, onChange)
        Text(label)
    }
}

@Composable
private fun <T> SearchOptionMenu(label: String, selected: T?, choices: List<Pair<T, String>>, onSelect: (T?) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        TextButton(onClick = { expanded = true }) {
            Text("$label: ${choices.firstOrNull { it.first == selected }?.second ?: stringResource(RStrings.all)}")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text(stringResource(RStrings.all)) }, onClick = { onSelect(null); expanded = false })
            choices.forEach { (value, text) ->
                DropdownMenuItem(text = { Text(text) }, onClick = { onSelect(value); expanded = false })
            }
        }
    }
}
