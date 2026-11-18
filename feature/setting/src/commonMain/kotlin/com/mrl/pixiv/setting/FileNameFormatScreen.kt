package com.mrl.pixiv.setting

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mrl.pixiv.common.data.setting.UserPreference
import com.mrl.pixiv.common.repository.SettingRepository
import com.mrl.pixiv.common.router.NavigationManager
import com.mrl.pixiv.common.router.currentNavigationManager
import com.mrl.pixiv.common.util.RStrings
import com.mrl.pixiv.setting.components.SettingPageColumn
import com.mrl.pixiv.setting.components.SettingSwitchRow
import com.mrl.pixiv.strings.download_settings_title
import com.mrl.pixiv.strings.download_single_folder_by_user_desc
import com.mrl.pixiv.strings.download_single_folder_by_user_title
import com.mrl.pixiv.strings.file_name_format_title
import com.mrl.pixiv.strings.legend_illust_id
import com.mrl.pixiv.strings.legend_index
import com.mrl.pixiv.strings.legend_meaning
import com.mrl.pixiv.strings.legend_template
import com.mrl.pixiv.strings.legend_title
import com.mrl.pixiv.strings.legend_user_id
import com.mrl.pixiv.strings.legend_user_name
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FileNameFormatScreen(
    modifier: Modifier = Modifier,
    navigationManager: NavigationManager = currentNavigationManager()
) {
    val userPreference by SettingRepository.userPreferenceFlow.collectAsStateWithLifecycle()
    val format = rememberTextFieldState(userPreference.fileNameFormat)

    FileNameFormatContent(
        format = format,
        downloadSubFolderByUser = userPreference.downloadSubFolderByUser,
        onFolderByUserChange = SettingRepository::setDownloadSubFolderByUser,
        onBack = navigationManager::popBackStack,
        onReset = {
            format.edit { replace(0, length, UserPreference.DEFAULT_FILE_NAME_FORMAT) }
        },
        onSave = {
            SettingRepository.setFileNameFormat(format.text.toString())
            navigationManager.popBackStack()
        },
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun FileNameFormatContent(
    format: TextFieldState,
    downloadSubFolderByUser: Boolean,
    onFolderByUserChange: (Boolean) -> Unit,
    onBack: () -> Unit,
    onReset: () -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(RStrings.download_settings_title),
                        style = LocalTextStyle.current.copy(
                            hyphens = Hyphens.Auto,
                            lineBreak = LineBreak.Heading,
                        ),
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        shapes = IconButtonDefaults.shapes(),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = null
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = onReset
                    ) {
                        Icon(imageVector = Icons.Rounded.Refresh, contentDescription = null)
                    }
                    IconButton(
                        onClick = onSave
                    ) {
                        Icon(imageVector = Icons.Rounded.Save, contentDescription = null)
                    }
                }
            )
        }
    ) { innerPadding ->
        SettingPageColumn(innerPadding) {
            SettingSwitchRow(
                title = stringResource(RStrings.download_single_folder_by_user_title),
                icon = Icons.Rounded.Folder,
                checked = downloadSubFolderByUser,
                onCheckedChange = onFolderByUserChange,
                supportingText = stringResource(RStrings.download_single_folder_by_user_desc),
            )
            FileNameFormatEditor(format)
            FileNameFormatLegend()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun FileNameFormatEditor(format: TextFieldState) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                state = format,
                label = { Text(text = stringResource(RStrings.file_name_format_title)) },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val tokens = listOf("title", "_", "index", "illust_id", "user_id", "user_name")
                tokens.forEach { key ->
                    FilterChip(
                        selected = false,
                        onClick = {
                            format.edit {
                                val cursor = selection.start
                                val tag = if (key == "_") "_" else "{$key}"
                                replace(cursor, selection.end, tag)
                            }
                        },
                        label = { Text(text = key) },
                    )
                }
            }
        }
    }
}

@Composable
private fun FileNameFormatLegend() {
    val legends = listOf(
        UserPreference.TEMPLATE_ILLUST_ID to RStrings.legend_illust_id,
        UserPreference.TEMPLATE_TITLE to RStrings.legend_title,
        UserPreference.TEMPLATE_USER_ID to RStrings.legend_user_id,
        UserPreference.TEMPLATE_USER_NAME to RStrings.legend_user_name,
        UserPreference.TEMPLATE_INDEX to RStrings.legend_index,
    )
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
            FileNameLegendRow(
                key = stringResource(RStrings.legend_template),
                meaning = stringResource(RStrings.legend_meaning),
                header = true,
            )
            legends.forEach { (key, res) ->
                HorizontalDivider()
                FileNameLegendRow(key = key, meaning = stringResource(res))
            }
        }
    }
}

@Composable
private fun FileNameLegendRow(key: String, meaning: String, header: Boolean = false) {
    val style = if (header) MaterialTheme.typography.labelLarge else MaterialTheme.typography.bodyMedium
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(text = key, modifier = Modifier.weight(1f), style = style)
        Text(text = meaning, modifier = Modifier.weight(1f), style = style)
    }
}
