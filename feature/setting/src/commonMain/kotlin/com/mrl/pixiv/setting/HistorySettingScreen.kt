package com.mrl.pixiv.setting

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.material.icons.automirrored.rounded.ViewList
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.History
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mrl.pixiv.common.data.setting.HistorySettings
import com.mrl.pixiv.common.repository.BrowsingHistoryRepository
import com.mrl.pixiv.common.repository.SettingRepository
import com.mrl.pixiv.common.repository.requireUserPreferenceFlow
import com.mrl.pixiv.common.router.NavigationManager
import com.mrl.pixiv.common.util.RStrings
import com.mrl.pixiv.common.util.ToastUtil
import com.mrl.pixiv.common.util.throttleClick
import com.mrl.pixiv.strings.clear_local_history
import com.mrl.pixiv.strings.clear_local_history_desc
import com.mrl.pixiv.strings.enable_cloud_history
import com.mrl.pixiv.strings.enable_cloud_history_desc
import com.mrl.pixiv.strings.enable_history
import com.mrl.pixiv.strings.enable_history_desc
import com.mrl.pixiv.strings.history_auto_clean
import com.mrl.pixiv.strings.history_auto_clean_desc
import com.mrl.pixiv.strings.history_max_entries
import com.mrl.pixiv.strings.history_max_entries_desc
import com.mrl.pixiv.strings.history_setting
import com.mrl.pixiv.strings.history_unlimited
import com.mrl.pixiv.strings.history_unlimited_desc
import com.mrl.pixiv.strings.local_history_cleared
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

@Composable
fun HistorySettingScreen(
    modifier: Modifier = Modifier,
    navigationManager: NavigationManager = koinInject(),
    browsingHistoryRepository: BrowsingHistoryRepository = koinInject(),
) {
    val userPreference by requireUserPreferenceFlow.collectAsStateWithLifecycle()
    val historySettings = userPreference.historySettings.normalized()
    val scope = rememberCoroutineScope()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(text = stringResource(RStrings.history_setting))
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
            HistorySwitchItem(
                title = stringResource(RStrings.enable_history),
                description = stringResource(RStrings.enable_history_desc),
                checked = historySettings.enabled,
                icon = { Icon(Icons.Rounded.History, contentDescription = null) },
                onCheckedChange = { checked ->
                    SettingRepository.setHistorySettings(historySettings.copy(enabled = checked))
                }
            )
            AnimatedVisibility(
                visible = historySettings.enabled,
                enter = slideInVertically { -it / 3 } + expandVertically(expandFrom = Alignment.Top) + fadeIn(),
                exit = slideOutVertically { -it / 3 } + shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut(),
            ) {
                Column {
                    HistorySwitchItem(
                        title = stringResource(RStrings.enable_cloud_history),
                        description = stringResource(RStrings.enable_cloud_history_desc),
                        checked = historySettings.cloudEnabled,
                        onCheckedChange = { checked ->
                            SettingRepository.setHistorySettings(historySettings.copy(cloudEnabled = checked))
                        }
                    )
                    HistorySwitchItem(
                        title = stringResource(RStrings.history_auto_clean),
                        description = stringResource(RStrings.history_auto_clean_desc),
                        checked = historySettings.autoClean,
                        onCheckedChange = { checked ->
                            SettingRepository.setHistorySettings(historySettings.copy(autoClean = checked))
                        }
                    )
                    HistorySwitchItem(
                        title = stringResource(RStrings.history_unlimited),
                        description = stringResource(RStrings.history_unlimited_desc),
                        checked = historySettings.unlimited,
                        onCheckedChange = { checked ->
                            SettingRepository.setHistorySettings(historySettings.copy(unlimited = checked))
                        }
                    )
                    AnimatedVisibility(
                        visible = !historySettings.unlimited,
                        enter = slideInVertically { -it / 3 } + expandVertically(expandFrom = Alignment.Top) + fadeIn(),
                        exit = slideOutVertically { -it / 3 } + shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut(),
                    ) {
                        HistoryLimitSetting(
                            selectedLimit = historySettings.maxEntries,
                            onLimitChange = { limit ->
                                SettingRepository.setHistorySettings(
                                    historySettings.copy(maxEntries = limit)
                                )
                            }
                        )
                    }
                    ListItem(
                        headlineContent = {
                            Text(text = stringResource(RStrings.clear_local_history))
                        },
                        supportingContent = {
                            Text(text = stringResource(RStrings.clear_local_history_desc))
                        },
                        modifier = Modifier
                            .height(IntrinsicSize.Min)
                            .throttleClick(indication = ripple()) {
                                scope.launch {
                                    browsingHistoryRepository.clearAllLocalHistory()
                                    ToastUtil.safeShortToast(RStrings.local_history_cleared)
                                }
                            },
                        leadingContent = {
                            Column(
                                modifier = Modifier.fillMaxHeight(),
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(Icons.Rounded.Delete, contentDescription = null)
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun HistorySwitchItem(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    icon: (@Composable () -> Unit)? = null,
) {
    ListItem(
        headlineContent = {
            Text(text = title)
        },
        supportingContent = {
            Text(text = description)
        },
        modifier = Modifier
            .height(IntrinsicSize.Min)
            .throttleClick(indication = ripple()) {
                onCheckedChange(!checked)
            },
        leadingContent = icon?.let {
            {
                Column(
                    modifier = Modifier.fillMaxHeight(),
                    verticalArrangement = Arrangement.Center
                ) {
                    it()
                }
            }
        },
        trailingContent = {
            Column(
                modifier = Modifier.fillMaxHeight(),
                verticalArrangement = Arrangement.Center
            ) {
                Switch(
                    checked = checked,
                    onCheckedChange = onCheckedChange,
                )
            }
        }
    )
}

@Composable
private fun HistoryLimitSetting(
    selectedLimit: Int,
    onLimitChange: (Int) -> Unit,
) {
    var input by remember(selectedLimit) { mutableStateOf(selectedLimit.toString()) }
    val validLimitRange = HistorySettings.MIN_ENTRIES..HistorySettings.MAX_ENTRIES
    val inputLimit = input.toIntOrNull()
    val isInputError = inputLimit == null || inputLimit !in validLimitRange

    ListItem(
        headlineContent = {
            Text(text = stringResource(RStrings.history_max_entries))
        },
        supportingContent = {
            Text(
                text = stringResource(
                    RStrings.history_max_entries_desc,
                    validLimitRange.first,
                    validLimitRange.last,
                )
            )
        },
        modifier = Modifier.height(IntrinsicSize.Min),
        leadingContent = {
            Column(
                modifier = Modifier.fillMaxHeight(),
                verticalArrangement = Arrangement.Center
            ) {
                Icon(Icons.AutoMirrored.Rounded.ViewList, contentDescription = null)
            }
        },
        trailingContent = {
            OutlinedTextField(
                modifier = Modifier.width(128.dp),
                value = input,
                onValueChange = { value ->
                    val digits = value
                        .filter { it.isDigit() }
                        .take(HistorySettings.MAX_ENTRIES.toString().length)
                    input = digits
                    digits.toIntOrNull()
                        ?.takeIf { it in validLimitRange }
                        ?.let(onLimitChange)
                },
                singleLine = true,
                isError = isInputError,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
        }
    )
}
