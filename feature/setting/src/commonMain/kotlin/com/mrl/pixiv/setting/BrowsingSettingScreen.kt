package com.mrl.pixiv.setting

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.FilterAlt
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Tag
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.ViewModule
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mrl.pixiv.common.data.setting.BrowsingSettings
import com.mrl.pixiv.common.data.setting.PreviewImageQuality
import com.mrl.pixiv.common.data.setting.SearchResultIllustLayout
import com.mrl.pixiv.common.repository.SettingRepository
import com.mrl.pixiv.common.repository.requireUserPreferenceFlow
import com.mrl.pixiv.common.router.NavigationManager
import com.mrl.pixiv.common.router.currentNavigationManager
import com.mrl.pixiv.common.util.RStrings
import com.mrl.pixiv.setting.components.SettingChoiceRow
import com.mrl.pixiv.setting.components.SettingRow
import com.mrl.pixiv.setting.components.SettingSectionTitle
import com.mrl.pixiv.setting.components.SettingSwitchRow
import com.mrl.pixiv.strings.auto_hide_preview_controls
import com.mrl.pixiv.strings.auto_hide_preview_controls_desc
import com.mrl.pixiv.strings.browsing_setting
import com.mrl.pixiv.strings.filter_long_novel_tags
import com.mrl.pixiv.strings.filter_long_novel_tags_desc
import com.mrl.pixiv.strings.max_novel_tag_length
import com.mrl.pixiv.strings.max_novel_tag_length_desc
import com.mrl.pixiv.strings.max_novel_tag_segments
import com.mrl.pixiv.strings.max_novel_tag_segments_desc
import com.mrl.pixiv.strings.preview_image_quality
import com.mrl.pixiv.strings.preview_image_quality_high
import com.mrl.pixiv.strings.preview_image_quality_medium
import com.mrl.pixiv.strings.preview_image_quality_original
import com.mrl.pixiv.strings.search_result_illust_layout
import com.mrl.pixiv.strings.search_result_illust_layout_original_aspect_ratio
import com.mrl.pixiv.strings.search_result_illust_layout_square
import com.mrl.pixiv.strings.settings_section_image_preview
import com.mrl.pixiv.strings.settings_section_novel_tags
import com.mrl.pixiv.strings.settings_section_work_grid
import com.mrl.pixiv.strings.span_count_adaptive
import com.mrl.pixiv.strings.span_count_landscape
import com.mrl.pixiv.strings.span_count_portrait
import com.mrl.pixiv.strings.tap_image_to_open_full_resolution_preview
import com.mrl.pixiv.strings.tap_image_to_open_full_resolution_preview_desc
import org.jetbrains.compose.resources.stringResource

private val SpanCountOptions = listOf(2, 3, 4, -1)

@Composable
fun BrowsingSettingScreen(
    modifier: Modifier = Modifier,
    navigationManager: NavigationManager = currentNavigationManager(),
) {
    val userPreference by requireUserPreferenceFlow.collectAsStateWithLifecycle()

    BrowsingSettingContent(
        browsingSettings = userPreference.browsingSettings,
        spanCountPortrait = userPreference.spanCountPortrait,
        spanCountLandscape = userPreference.spanCountLandscape,
        onSpanCountPortraitChange = SettingRepository::setSpanCountPortrait,
        onSpanCountLandscapeChange = SettingRepository::setSpanCountLandscape,
        onBrowsingSettingsChange = SettingRepository::setBrowsingSettings,
        onBack = navigationManager::popBackStack,
        modifier = modifier,
    )
}

@Composable
internal fun BrowsingSettingContent(
    browsingSettings: BrowsingSettings,
    spanCountPortrait: Int,
    spanCountLandscape: Int,
    onSpanCountPortraitChange: (Int) -> Unit,
    onSpanCountLandscapeChange: (Int) -> Unit,
    onBrowsingSettingsChange: (BrowsingSettings) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(RStrings.browsing_setting),
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
    ) { innerPadding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(innerPadding).imePadding(),
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
                SettingSectionTitle(stringResource(RStrings.settings_section_work_grid))
                SpanCountSetting(
                    title = stringResource(RStrings.span_count_portrait),
                    currentSpanCount = spanCountPortrait,
                    onSpanCountChange = onSpanCountPortraitChange,
                )
                SpanCountSetting(
                    title = stringResource(RStrings.span_count_landscape),
                    currentSpanCount = spanCountLandscape,
                    onSpanCountChange = onSpanCountLandscapeChange,
                )
                SettingChoiceRow(
                    title = stringResource(RStrings.search_result_illust_layout),
                    icon = Icons.Rounded.ViewModule,
                    selected = browsingSettings.searchResultIllustLayout,
                    options = SearchResultIllustLayout.entries,
                    onSelectionChange = { layout ->
                        onBrowsingSettingsChange(browsingSettings.copy(searchResultIllustLayout = layout))
                    },
                    optionLabel = { it.label() },
                )

                SettingSectionTitle(stringResource(RStrings.settings_section_image_preview))
                SettingChoiceRow(
                    title = stringResource(RStrings.preview_image_quality),
                    icon = Icons.Rounded.Image,
                    selected = browsingSettings.previewImageQuality,
                    options = PreviewImageQuality.entries,
                    onSelectionChange = { quality ->
                        onBrowsingSettingsChange(browsingSettings.copy(previewImageQuality = quality))
                    },
                    optionLabel = { it.label() },
                )
                SettingSwitchRow(
                    title = stringResource(RStrings.auto_hide_preview_controls),
                    icon = Icons.Rounded.VisibilityOff,
                    checked = browsingSettings.autoHidePreviewControls,
                    supportingText = stringResource(RStrings.auto_hide_preview_controls_desc),
                    onCheckedChange = { checked ->
                        onBrowsingSettingsChange(browsingSettings.copy(autoHidePreviewControls = checked))
                    },
                )
                SettingSwitchRow(
                    title = stringResource(RStrings.tap_image_to_open_full_resolution_preview),
                    icon = Icons.Rounded.TouchApp,
                    checked = browsingSettings.tapImageToOpenFullResolutionPreview,
                    supportingText = stringResource(RStrings.tap_image_to_open_full_resolution_preview_desc),
                    onCheckedChange = { checked ->
                        onBrowsingSettingsChange(
                            browsingSettings.copy(tapImageToOpenFullResolutionPreview = checked)
                        )
                    },
                )

                SettingSectionTitle(stringResource(RStrings.settings_section_novel_tags))
                SettingSwitchRow(
                    title = stringResource(RStrings.filter_long_novel_tags),
                    icon = Icons.Rounded.FilterAlt,
                    checked = browsingSettings.filterLongNovelTags,
                    supportingText = stringResource(RStrings.filter_long_novel_tags_desc),
                    onCheckedChange = { checked ->
                        onBrowsingSettingsChange(browsingSettings.copy(filterLongNovelTags = checked))
                    },
                )
                if (browsingSettings.filterLongNovelTags) {
                    NovelTagLimitSetting(
                        title = stringResource(RStrings.max_novel_tag_length),
                        description = stringResource(RStrings.max_novel_tag_length_desc),
                        value = browsingSettings.maxNovelTagLength,
                        onValueChange = { value ->
                            onBrowsingSettingsChange(browsingSettings.copy(maxNovelTagLength = value))
                        },
                    )
                    NovelTagLimitSetting(
                        title = stringResource(RStrings.max_novel_tag_segments),
                        description = stringResource(RStrings.max_novel_tag_segments_desc),
                        value = browsingSettings.maxNovelTagSegments,
                        onValueChange = { value ->
                            onBrowsingSettingsChange(browsingSettings.copy(maxNovelTagSegments = value))
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun SpanCountSetting(
    title: String,
    currentSpanCount: Int,
    onSpanCountChange: (Int) -> Unit,
) {
    SettingChoiceRow(
        title = title,
        icon = Icons.Rounded.ViewModule,
        selected = currentSpanCount,
        options = SpanCountOptions,
        onSelectionChange = onSpanCountChange,
        optionLabel = { count ->
            when (count) {
                2, 3, 4 -> count.toString()
                else -> stringResource(RStrings.span_count_adaptive)
            }
        },
    )
}

@Composable
private fun NovelTagLimitSetting(
    title: String,
    description: String,
    value: Int,
    onValueChange: (Int) -> Unit,
) {
    var input by remember(value) { mutableStateOf(value.toString()) }
    val validRange = BrowsingSettings.MIN_NOVEL_TAG_LIMIT..BrowsingSettings.MAX_NOVEL_TAG_LIMIT
    val parsedValue = input.toIntOrNull()

    SettingRow(
        title = title,
        icon = Icons.Rounded.Tag,
        supportingContent = { Text(description) },
        trailingContent = {
            OutlinedTextField(
                modifier = Modifier.width(104.dp),
                value = input,
                onValueChange = { newValue ->
                    val digits = newValue.filter(Char::isDigit).take(3)
                    input = digits
                    digits.toIntOrNull()
                        ?.takeIf { it in validRange }
                        ?.let(onValueChange)
                },
                singleLine = true,
                isError = parsedValue == null || parsedValue !in validRange,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
        },
    )
}

@Composable
private fun SearchResultIllustLayout.label(): String = when (this) {
    SearchResultIllustLayout.SQUARE ->
        stringResource(RStrings.search_result_illust_layout_square)

    SearchResultIllustLayout.ORIGINAL_ASPECT_RATIO ->
        stringResource(RStrings.search_result_illust_layout_original_aspect_ratio)
}

@Composable
private fun PreviewImageQuality.label(): String = when (this) {
    PreviewImageQuality.MEDIUM -> stringResource(RStrings.preview_image_quality_medium)
    PreviewImageQuality.HIGH -> stringResource(RStrings.preview_image_quality_high)
    PreviewImageQuality.ORIGINAL -> stringResource(RStrings.preview_image_quality_original)
}
