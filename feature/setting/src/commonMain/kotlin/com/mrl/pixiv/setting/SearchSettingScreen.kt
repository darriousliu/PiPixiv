package com.mrl.pixiv.setting

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.FilterAlt
import androidx.compose.material.icons.rounded.ViewModule
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mrl.pixiv.common.data.search.SearchAiType
import com.mrl.pixiv.common.data.search.SearchSort
import com.mrl.pixiv.common.data.search.SearchTarget
import com.mrl.pixiv.common.data.setting.SearchResultDisplayMode
import com.mrl.pixiv.common.repository.SettingRepository
import com.mrl.pixiv.common.repository.requireUserPreferenceFlow
import com.mrl.pixiv.common.router.NavigationManager
import com.mrl.pixiv.common.util.RStrings
import com.mrl.pixiv.common.util.throttleClick
import com.mrl.pixiv.setting.components.DropDownSelector
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
import com.mrl.pixiv.strings.search_setting
import com.mrl.pixiv.strings.tags_exact_match
import com.mrl.pixiv.strings.tags_partially_match
import com.mrl.pixiv.strings.title_and_description
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

@Composable
fun SearchSettingScreen(
    modifier: Modifier = Modifier,
    navigationManager: NavigationManager = koinInject(),
) {
    val userPreference by requireUserPreferenceFlow.collectAsStateWithLifecycle()
    val searchSettings = userPreference.searchSettings

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(text = stringResource(RStrings.search_setting))
                },
                navigationIcon = {
                    IconButton(
                        onClick = navigationManager::popBackStack,
                        shapes = IconButtonDefaults.shapes(),
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(paddingValues)
                .imePadding()
                .padding(horizontal = 8.dp)
        ) {
            DefaultSearchTargetSetting(
                selectedTarget = searchSettings.defaultSearchTarget,
                onTargetChange = { target ->
                    SettingRepository.setSearchSettings(
                        searchSettings.copy(defaultSearchTarget = target)
                    )
                }
            )
            DefaultSearchSortSetting(
                selectedSort = searchSettings.defaultSearchSort,
                onSortChange = { sort ->
                    SettingRepository.setSearchSettings(
                        searchSettings.copy(defaultSearchSort = sort)
                    )
                }
            )
            ListItem(
                headlineContent = {
                    Text(text = stringResource(RStrings.ai_generate))
                },
                modifier = Modifier.throttleClick(indication = ripple()) {
                    SettingRepository.setSearchSettings(
                        searchSettings.copy(
                            defaultSearchAiType = searchSettings.defaultSearchAiType.toggled()
                        )
                    )
                },
                leadingContent = {
                    Icon(Icons.Rounded.AutoAwesome, contentDescription = null)
                },
                trailingContent = {
                    Switch(
                        checked = searchSettings.defaultSearchAiType == SearchAiType.SHOW_AI,
                        onCheckedChange = { checked ->
                            SettingRepository.setSearchSettings(
                                searchSettings.copy(
                                    defaultSearchAiType = if (checked) {
                                        SearchAiType.SHOW_AI
                                    } else {
                                        SearchAiType.HIDE_AI
                                    }
                                )
                            )
                        }
                    )
                }
            )
            SearchResultDisplayModeSetting(
                selectedMode = searchSettings.searchResultDisplayMode,
                onModeChange = { mode ->
                    SettingRepository.setSearchSettings(
                        searchSettings.copy(searchResultDisplayMode = mode)
                    )
                }
            )
        }
    }
}

@Composable
private fun DefaultSearchTargetSetting(
    selectedTarget: SearchTarget,
    onTargetChange: (SearchTarget) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val targets = remember {
        listOf(
            SearchTarget.PARTIAL_MATCH_FOR_TAGS,
            SearchTarget.EXACT_MATCH_FOR_TAGS,
            SearchTarget.TITLE_AND_CAPTION,
        )
    }

    ListItem(
        headlineContent = { Text(text = stringResource(RStrings.default_search_target)) },
        modifier = modifier,
        leadingContent = {
            Icon(Icons.Rounded.FilterAlt, contentDescription = null)
        },
        trailingContent = {
            DropDownSelector(
                modifier = Modifier.throttleClick { expanded = !expanded },
                expanded = expanded,
                onDismissRequest = { expanded = false },
                current = selectedTarget.label(),
            ) {
                targets.forEach { target ->
                    DropdownMenuItem(
                        text = {
                            Text(text = target.label())
                        },
                        trailingIcon = {
                            if (target == selectedTarget) {
                                Icon(Icons.Rounded.Check, contentDescription = null)
                            }
                        },
                        onClick = {
                            onTargetChange(target)
                            expanded = false
                        }
                    )
                }
            }
        }
    )
}

@Composable
private fun DefaultSearchSortSetting(
    selectedSort: SearchSort,
    onSortChange: (SearchSort) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val sorts = remember {
        listOf(
            SearchSort.DATE_DESC,
            SearchSort.DATE_ASC,
            SearchSort.POPULAR_DESC,
            SearchSort.POPULAR_MALE_DESC,
            SearchSort.POPULAR_FEMALE_DESC,
        )
    }

    ListItem(
        headlineContent = { Text(text = stringResource(RStrings.default_search_sort)) },
        modifier = modifier,
        leadingContent = {
            Icon(Icons.AutoMirrored.Rounded.Sort, contentDescription = null)
        },
        trailingContent = {
            DropDownSelector(
                modifier = Modifier.throttleClick { expanded = !expanded },
                expanded = expanded,
                onDismissRequest = { expanded = false },
                current = selectedSort.label(),
            ) {
                sorts.forEach { sort ->
                    DropdownMenuItem(
                        text = {
                            Text(text = sort.label())
                        },
                        trailingIcon = {
                            if (sort == selectedSort) {
                                Icon(Icons.Rounded.Check, contentDescription = null)
                            }
                        },
                        onClick = {
                            onSortChange(sort)
                            expanded = false
                        }
                    )
                }
            }
        }
    )
}

@Composable
private fun SearchSort.label(): String {
    return when (this) {
        SearchSort.DATE_DESC -> stringResource(RStrings.date_desc)
        SearchSort.DATE_ASC -> stringResource(RStrings.date_asc)
        SearchSort.POPULAR_DESC -> stringResource(RStrings.popular_desc)
        SearchSort.POPULAR_MALE_DESC -> stringResource(RStrings.popular_male)
        SearchSort.POPULAR_FEMALE_DESC -> stringResource(RStrings.popular_female)
    }
}

@Composable
private fun SearchTarget.label(): String {
    return when (this) {
        SearchTarget.PARTIAL_MATCH_FOR_TAGS ->
            stringResource(RStrings.tags_partially_match)

        SearchTarget.EXACT_MATCH_FOR_TAGS ->
            stringResource(RStrings.tags_exact_match)

        SearchTarget.TITLE_AND_CAPTION,
        SearchTarget.TEXT,
        SearchTarget.KEYWORD -> stringResource(RStrings.title_and_description)
    }
}

private fun SearchAiType.toggled(): SearchAiType {
    return when (this) {
        SearchAiType.SHOW_AI -> SearchAiType.HIDE_AI
        SearchAiType.HIDE_AI -> SearchAiType.SHOW_AI
    }
}

@Composable
private fun SearchResultDisplayModeSetting(
    selectedMode: SearchResultDisplayMode,
    onModeChange: (SearchResultDisplayMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val modes = remember { SearchResultDisplayMode.entries }

    ListItem(
        headlineContent = { Text(text = stringResource(RStrings.search_result_display_mode)) },
        modifier = modifier,
        leadingContent = { Icon(Icons.Rounded.ViewModule, contentDescription = null) },
        trailingContent = {
            DropDownSelector(
                modifier = Modifier.throttleClick { expanded = !expanded },
                expanded = expanded,
                onDismissRequest = { expanded = false },
                current = selectedMode.label(),
            ) {
                modes.forEach { mode ->
                    DropdownMenuItem(
                        text = {
                            Text(text = mode.label())
                        },
                        trailingIcon = {
                            if (mode == selectedMode) {
                                Icon(Icons.Rounded.Check, contentDescription = null)
                            }
                        },
                        onClick = {
                            onModeChange(mode)
                            expanded = false
                        }
                    )
                }
            }
        }
    )
}

@Composable
private fun SearchResultDisplayMode.label(): String {
    return when (this) {
        SearchResultDisplayMode.INFINITE_SCROLL ->
            stringResource(RStrings.search_result_display_mode_infinite)

        SearchResultDisplayMode.PAGED ->
            stringResource(RStrings.search_result_display_mode_paged)
    }
}
