package com.mrl.pixiv.setting

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.FilterAlt
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Tag
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mrl.pixiv.common.data.setting.BrowsingSettings
import com.mrl.pixiv.common.data.setting.PreviewImageQuality
import com.mrl.pixiv.common.repository.SettingRepository
import com.mrl.pixiv.common.repository.requireUserPreferenceFlow
import com.mrl.pixiv.common.router.NavigationManager
import com.mrl.pixiv.common.util.RStrings
import com.mrl.pixiv.common.util.throttleClick
import com.mrl.pixiv.setting.components.DropDownSelector
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
import com.mrl.pixiv.strings.tap_image_to_open_full_resolution_preview
import com.mrl.pixiv.strings.tap_image_to_open_full_resolution_preview_desc
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

@Composable
fun BrowsingSettingScreen(
    modifier: Modifier = Modifier,
    navigationManager: NavigationManager = koinInject(),
) {
    val userPreference by requireUserPreferenceFlow.collectAsStateWithLifecycle()
    val browsingSettings = userPreference.browsingSettings

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(text = stringResource(RStrings.browsing_setting))
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
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(it)
                .imePadding()
                .padding(horizontal = 8.dp)
        ) {
            PreviewImageQualitySetting(
                selectedQuality = browsingSettings.previewImageQuality,
                onQualityChange = { quality ->
                    SettingRepository.setBrowsingSettings(
                        browsingSettings.copy(previewImageQuality = quality)
                    )
                }
            )
            ListItem(
                headlineContent = {
                    Text(text = stringResource(RStrings.auto_hide_preview_controls))
                },
                supportingContent = {
                    Text(text = stringResource(RStrings.auto_hide_preview_controls_desc))
                },
                modifier = Modifier
                    .height(IntrinsicSize.Min)
                    .throttleClick(indication = ripple()) {
                        SettingRepository.setBrowsingSettings(
                            browsingSettings.copy(
                                autoHidePreviewControls = !browsingSettings.autoHidePreviewControls
                            )
                        )
                    },
                leadingContent = {
                    Column(
                        modifier = Modifier.fillMaxHeight(),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Rounded.VisibilityOff, contentDescription = null)
                    }
                },
                trailingContent = {
                    Column(
                        modifier = Modifier.fillMaxHeight(),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Switch(
                            checked = browsingSettings.autoHidePreviewControls,
                            onCheckedChange = { checked ->
                                SettingRepository.setBrowsingSettings(
                                    browsingSettings.copy(autoHidePreviewControls = checked)
                                )
                            }
                        )
                    }
                }
            )
            ListItem(
                headlineContent = {
                    Text(text = stringResource(RStrings.tap_image_to_open_full_resolution_preview))
                },
                supportingContent = {
                    Text(text = stringResource(RStrings.tap_image_to_open_full_resolution_preview_desc))
                },
                modifier = Modifier
                    .height(IntrinsicSize.Min)
                    .throttleClick(indication = ripple()) {
                        SettingRepository.setBrowsingSettings(
                            browsingSettings.copy(
                                tapImageToOpenFullResolutionPreview =
                                    !browsingSettings.tapImageToOpenFullResolutionPreview
                            )
                        )
                    },
                leadingContent = {
                    Column(
                        modifier = Modifier.fillMaxHeight(),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Rounded.TouchApp, contentDescription = null)
                    }
                },
                trailingContent = {
                    Column(
                        modifier = Modifier.fillMaxHeight(),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Switch(
                            checked = browsingSettings.tapImageToOpenFullResolutionPreview,
                            onCheckedChange = { checked ->
                                SettingRepository.setBrowsingSettings(
                                    browsingSettings.copy(
                                        tapImageToOpenFullResolutionPreview = checked
                                    )
                                )
                            }
                        )
                    }
                }
            )
            ListItem(
                headlineContent = {
                    Text(text = stringResource(RStrings.filter_long_novel_tags))
                },
                supportingContent = {
                    Text(text = stringResource(RStrings.filter_long_novel_tags_desc))
                },
                modifier = Modifier
                    .height(IntrinsicSize.Min)
                    .throttleClick(indication = ripple()) {
                        SettingRepository.setBrowsingSettings(
                            browsingSettings.copy(
                                filterLongNovelTags = !browsingSettings.filterLongNovelTags
                            )
                        )
                    },
                leadingContent = {
                    Column(
                        modifier = Modifier.fillMaxHeight(),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Rounded.FilterAlt, contentDescription = null)
                    }
                },
                trailingContent = {
                    Column(
                        modifier = Modifier.fillMaxHeight(),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Switch(
                            checked = browsingSettings.filterLongNovelTags,
                            onCheckedChange = { checked ->
                                SettingRepository.setBrowsingSettings(
                                    browsingSettings.copy(filterLongNovelTags = checked)
                                )
                            }
                        )
                    }
                }
            )
            if (browsingSettings.filterLongNovelTags) {
                NovelTagLimitSetting(
                    title = stringResource(RStrings.max_novel_tag_length),
                    description = stringResource(RStrings.max_novel_tag_length_desc),
                    value = browsingSettings.maxNovelTagLength,
                    onValueChange = { value ->
                        SettingRepository.setBrowsingSettings(
                            browsingSettings.copy(maxNovelTagLength = value)
                        )
                    }
                )
                NovelTagLimitSetting(
                    title = stringResource(RStrings.max_novel_tag_segments),
                    description = stringResource(RStrings.max_novel_tag_segments_desc),
                    value = browsingSettings.maxNovelTagSegments,
                    onValueChange = { value ->
                        SettingRepository.setBrowsingSettings(
                            browsingSettings.copy(maxNovelTagSegments = value)
                        )
                    }
                )
            }
        }
    }
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

    ListItem(
        headlineContent = { Text(text = title) },
        supportingContent = { Text(text = description) },
        modifier = Modifier.height(IntrinsicSize.Min),
        leadingContent = {
            Column(
                modifier = Modifier.fillMaxHeight(),
                verticalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Rounded.Tag, contentDescription = null)
            }
        },
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
        }
    )
}

@Composable
private fun PreviewImageQualitySetting(
    selectedQuality: PreviewImageQuality,
    onQualityChange: (PreviewImageQuality) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val qualities = remember { PreviewImageQuality.entries }

    ListItem(
        headlineContent = { Text(text = stringResource(RStrings.preview_image_quality)) },
        modifier = modifier,
        leadingContent = { Icon(Icons.Rounded.Image, contentDescription = null) },
        trailingContent = {
            DropDownSelector(
                modifier = Modifier.throttleClick { expanded = !expanded },
                expanded = expanded,
                onDismissRequest = { expanded = false },
                current = selectedQuality.label(),
            ) {
                qualities.forEach { quality ->
                    DropdownMenuItem(
                        text = {
                            Text(text = quality.label())
                        },
                        trailingIcon = {
                            if (quality == selectedQuality) {
                                Icon(Icons.Rounded.Check, contentDescription = null)
                            }
                        },
                        onClick = {
                            onQualityChange(quality)
                            expanded = false
                        }
                    )
                }
            }
        }
    )
}

@Composable
private fun PreviewImageQuality.label(): String {
    return when (this) {
        PreviewImageQuality.MEDIUM -> stringResource(RStrings.preview_image_quality_medium)
        PreviewImageQuality.HIGH -> stringResource(RStrings.preview_image_quality_high)
        PreviewImageQuality.ORIGINAL -> stringResource(RStrings.preview_image_quality_original)
    }
}
