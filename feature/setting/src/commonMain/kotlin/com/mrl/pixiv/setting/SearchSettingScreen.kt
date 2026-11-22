package com.mrl.pixiv.setting

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Collections
import androidx.compose.material.icons.rounded.FilterAlt
import androidx.compose.material.icons.rounded.ViewModule
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mrl.pixiv.common.compose.ui.SearchContentFilterControls
import com.mrl.pixiv.common.data.search.SearchAiType
import com.mrl.pixiv.common.data.search.SearchSort
import com.mrl.pixiv.common.data.search.SearchTarget
import com.mrl.pixiv.common.data.setting.SearchFilterPreferences
import com.mrl.pixiv.common.data.setting.SearchResultDisplayMode
import com.mrl.pixiv.common.data.setting.SearchSettings
import com.mrl.pixiv.common.repository.SettingRepository
import com.mrl.pixiv.common.repository.requireUserPreferenceFlow
import com.mrl.pixiv.common.router.NavigationManager
import com.mrl.pixiv.common.router.currentNavigationManager
import com.mrl.pixiv.common.util.RStrings
import com.mrl.pixiv.setting.components.SettingChoiceRow
import com.mrl.pixiv.setting.components.SettingSectionTitle
import com.mrl.pixiv.setting.components.SettingSwitchRow
import com.mrl.pixiv.strings.ai_generate
import com.mrl.pixiv.strings.date_asc
import com.mrl.pixiv.strings.date_desc
import com.mrl.pixiv.strings.default_search_sort
import com.mrl.pixiv.strings.default_search_target
import com.mrl.pixiv.strings.popular_desc
import com.mrl.pixiv.strings.popular_female
import com.mrl.pixiv.strings.popular_male
import com.mrl.pixiv.strings.search_result_display_mode
import com.mrl.pixiv.strings.search_result_display_mode_infinite
import com.mrl.pixiv.strings.search_result_display_mode_paged
import com.mrl.pixiv.strings.search_series_only
import com.mrl.pixiv.strings.search_setting
import com.mrl.pixiv.strings.search_show_r18
import com.mrl.pixiv.strings.settings_section_content_filters
import com.mrl.pixiv.strings.settings_section_result_display
import com.mrl.pixiv.strings.settings_section_search_defaults
import com.mrl.pixiv.strings.tags_exact_match
import com.mrl.pixiv.strings.tags_partially_match
import com.mrl.pixiv.strings.title_and_description
import org.jetbrains.compose.resources.stringResource

private val searchTargetOptions = listOf(
    SearchTarget.PARTIAL_MATCH_FOR_TAGS,
    SearchTarget.EXACT_MATCH_FOR_TAGS,
    SearchTarget.TITLE_AND_CAPTION,
)

@Composable
fun SearchSettingScreen(
    modifier: Modifier = Modifier,
    navigationManager: NavigationManager = currentNavigationManager(),
) {
    val userPreference by requireUserPreferenceFlow.collectAsStateWithLifecycle()
    val searchSettings = userPreference.searchSettings

    SearchSettingContent(
        searchSettings = searchSettings,
        defaultShowR18 = userPreference.isR18Enabled,
        onFilterDefaultsChange = SettingRepository::updateSearchFilterDefaults,
        onDisplayModeChange = { mode ->
            SettingRepository.setSearchSettings(searchSettings.copy(searchResultDisplayMode = mode))
        },
        onBack = navigationManager::popBackStack,
        modifier = modifier,
    )
}

@Composable
internal fun SearchSettingContent(
    searchSettings: SearchSettings,
    defaultShowR18: Boolean,
    onFilterDefaultsChange: (SearchFilterPreferences.() -> SearchFilterPreferences) -> Unit,
    onDisplayModeChange: (SearchResultDisplayMode) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(RStrings.search_setting),
                        style = LocalTextStyle.current.copy(
                            hyphens = Hyphens.Auto,
                            lineBreak = LineBreak.Heading,
                        ),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, shapes = IconButtonDefaults.shapes()) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null)
                    }
                },
            )
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier.fillMaxWidth().padding(paddingValues).imePadding(),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 640.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(start = 16.dp, end = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                SettingSectionTitle(stringResource(RStrings.settings_section_search_defaults))
                SettingChoiceRow(
                    title = stringResource(RStrings.default_search_target),
                    icon = Icons.Rounded.FilterAlt,
                    options = searchTargetOptions,
                    selected = searchSettings.defaultSearchTarget,
                    onSelectionChange = { target ->
                        onFilterDefaultsChange { copy(searchTarget = target) }
                    },
                    optionLabel = { it.label() },
                )
                SettingChoiceRow(
                    title = stringResource(RStrings.default_search_sort),
                    icon = Icons.AutoMirrored.Rounded.Sort,
                    options = SearchSort.entries,
                    selected = searchSettings.defaultSearchSort,
                    onSelectionChange = { sort ->
                        onFilterDefaultsChange { copy(sort = sort) }
                    },
                    optionLabel = { it.label() },
                )
                SettingSwitchRow(
                    title = stringResource(RStrings.ai_generate),
                    icon = Icons.Rounded.AutoAwesome,
                    checked = searchSettings.defaultSearchAiType == SearchAiType.SHOW_AI,
                    onCheckedChange = { checked ->
                        onFilterDefaultsChange {
                            copy(searchAiType = if (checked) SearchAiType.SHOW_AI else SearchAiType.HIDE_AI)
                        }
                    },
                )
                SettingSectionTitle(stringResource(RStrings.settings_section_result_display))
                SettingChoiceRow(
                    title = stringResource(RStrings.search_result_display_mode),
                    icon = Icons.Rounded.ViewModule,
                    options = SearchResultDisplayMode.entries,
                    selected = searchSettings.searchResultDisplayMode,
                    onSelectionChange = onDisplayModeChange,
                    optionLabel = { it.label() },
                )
                SettingSectionTitle(stringResource(RStrings.settings_section_content_filters))
                SearchContentFilterControls(
                    filter = searchSettings.defaultContentFilter,
                    defaultShowR18 = defaultShowR18,
                    onChange = { contentFilter ->
                        onFilterDefaultsChange { copy(contentFilter = contentFilter) }
                    },
                    artworkTypeSection = { title, content ->
                        SearchSettingsArtworkTypeSection(title = title, content = content)
                    },
                    switchRow = { label, checked, onCheckedChange ->
                        SettingSwitchRow(
                            title = stringResource(label),
                            icon = when (label) {
                                RStrings.search_series_only -> Icons.AutoMirrored.Rounded.MenuBook
                                RStrings.search_show_r18 -> Icons.Rounded.Visibility
                                else -> Icons.Rounded.FilterAlt
                            },
                            checked = checked,
                            onCheckedChange = onCheckedChange,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun SearchSettingsArtworkTypeSection(
    title: String,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 8.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.Collections, contentDescription = null)
                Text(title, style = MaterialTheme.typography.bodyLarge)
            }
            content()
        }
    }
}

@Composable
private fun SearchSort.label(): String = when (this) {
    SearchSort.DATE_DESC -> stringResource(RStrings.date_desc)
    SearchSort.DATE_ASC -> stringResource(RStrings.date_asc)
    SearchSort.POPULAR_DESC -> stringResource(RStrings.popular_desc)
    SearchSort.POPULAR_MALE_DESC -> stringResource(RStrings.popular_male)
    SearchSort.POPULAR_FEMALE_DESC -> stringResource(RStrings.popular_female)
}

@Composable
private fun SearchTarget.label(): String = when (this) {
    SearchTarget.PARTIAL_MATCH_FOR_TAGS -> stringResource(RStrings.tags_partially_match)
    SearchTarget.EXACT_MATCH_FOR_TAGS -> stringResource(RStrings.tags_exact_match)

    SearchTarget.TITLE_AND_CAPTION,
    SearchTarget.TEXT,
    SearchTarget.KEYWORD -> stringResource(RStrings.title_and_description)
}

@Composable
private fun SearchResultDisplayMode.label(): String = when (this) {
    SearchResultDisplayMode.INFINITE_SCROLL -> stringResource(RStrings.search_result_display_mode_infinite)
    SearchResultDisplayMode.PAGED -> stringResource(RStrings.search_result_display_mode_paged)
}
