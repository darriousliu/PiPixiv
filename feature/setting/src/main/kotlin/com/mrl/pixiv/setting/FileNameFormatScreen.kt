package com.mrl.pixiv.setting

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mrl.pixiv.common.data.setting.UserPreference
import com.mrl.pixiv.common.repository.SettingRepository
import com.mrl.pixiv.common.router.NavigationManager
import com.mrl.pixiv.common.util.RString
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FileNameFormatScreen(
    modifier: Modifier = Modifier,
    navigationManager: NavigationManager = koinInject()
) {
    val userPreference by SettingRepository.userPreferenceFlow.collectAsStateWithLifecycle()
    val format = rememberTextFieldState(userPreference.fileNameFormat)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(RString.file_name_format_title)) },
                navigationIcon = {
                    IconButton(
                        onClick = navigationManager::popBackStack,
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
                        onClick = {
                            format.edit {
                                replace(0, length, UserPreference.DEFAULT_FILE_NAME_FORMAT)
                            }
                        }
                    ) {
                        Icon(imageVector = Icons.Rounded.Refresh, contentDescription = null)
                    }
                    IconButton(
                        onClick = {
                            SettingRepository.setFileNameFormat(format.text.toString())
                            navigationManager.popBackStack()
                        }
                    ) {
                        Icon(imageVector = Icons.Rounded.Save, contentDescription = null)
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            OutlinedTextField(
                state = format,
                label = { Text(text = stringResource(RString.file_name_format_title)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )

            val chips = listOf(
                "title", "_", "index", "illust_id", "user_id", "user_name"
            )

            FlowRow(
                modifier = Modifier.padding(horizontal = 16.dp),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
            ) {
                chips.forEach { key ->
                    FilterChip(
                        selected = false,
                        onClick = {
                            format.edit {
                                val cursor = selection.start
                                val tag = if (key == "_") "_" else "{$key}"
                                replace(cursor, selection.end, tag)
                            }
                        },
                        label = { Text(text = key) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.padding(16.dp)) {
                Text(text = stringResource(RString.legend_template), modifier = Modifier.weight(1f))
                Text(text = stringResource(RString.legend_meaning), modifier = Modifier.weight(1f))
            }
            val legends = listOf(
                "{illust_id}" to RString.legend_illust_id,
                "{title}" to RString.legend_title,
                "{user_id}" to RString.legend_user_id,
                "{user_name}" to RString.legend_user_name,
                "{index}" to RString.legend_index,
            )

            legends.forEach { (key, res) ->
                HorizontalDivider()
                ListItem(
                    headlineContent = {
                        Row {
                            Text(text = key, modifier = Modifier.weight(1f))
                            Text(text = stringResource(res), modifier = Modifier.weight(1f))
                        }
                    }
                )
            }
        }
    }
}
