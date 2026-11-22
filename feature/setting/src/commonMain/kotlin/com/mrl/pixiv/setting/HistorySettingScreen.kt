package com.mrl.pixiv.setting

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material.icons.automirrored.rounded.ViewList
import androidx.compose.material.icons.rounded.AllInclusive
import androidx.compose.material.icons.rounded.AutoDelete
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.History
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
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mrl.pixiv.common.compose.rememberThrottleClick
import com.mrl.pixiv.common.data.setting.HistorySettings
import com.mrl.pixiv.common.repository.BrowsingHistoryRepository
import com.mrl.pixiv.common.repository.SettingRepository
import com.mrl.pixiv.common.repository.requireUserPreferenceFlow
import com.mrl.pixiv.common.router.NavigationManager
import com.mrl.pixiv.common.router.currentNavigationManager
import com.mrl.pixiv.common.util.RStrings
import com.mrl.pixiv.common.util.ToastUtil
import com.mrl.pixiv.setting.components.SettingRow
import com.mrl.pixiv.setting.components.SettingSwitchRow
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
    navigationManager: NavigationManager = currentNavigationManager(),
    browsingHistoryRepository: BrowsingHistoryRepository = koinInject(),
) {
    val userPreference by requireUserPreferenceFlow.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    HistorySettingContent(
        historySettings = userPreference.historySettings.normalized(),
        onHistorySettingsChange = SettingRepository::setHistorySettings,
        onClearLocalHistory = {
            scope.launch {
                browsingHistoryRepository.clearAllLocalHistory()
                ToastUtil.safeShortToast(RStrings.local_history_cleared)
            }
        },
        onBack = navigationManager::popBackStack,
        modifier = modifier,
    )
}

@Composable
internal fun HistorySettingContent(
    historySettings: HistorySettings,
    onHistorySettingsChange: (HistorySettings) -> Unit,
    onClearLocalHistory: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(RStrings.history_setting),
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
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null)
                    }
                }
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
                SettingSwitchRow(
                    title = stringResource(RStrings.enable_history),
                    icon = Icons.Rounded.History,
                    checked = historySettings.enabled,
                    onCheckedChange = { checked ->
                        onHistorySettingsChange(historySettings.copy(enabled = checked))
                    },
                    supportingText = stringResource(RStrings.enable_history_desc),
                )
                AnimatedVisibility(
                    visible = historySettings.enabled,
                    enter = slideInVertically { -it / 3 } + expandVertically(expandFrom = Alignment.Top) + fadeIn(),
                    exit = slideOutVertically { -it / 3 } + shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut(),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        SettingSwitchRow(
                            title = stringResource(RStrings.enable_cloud_history),
                            icon = Icons.Rounded.Cloud,
                            checked = historySettings.cloudEnabled,
                            onCheckedChange = { checked ->
                                onHistorySettingsChange(historySettings.copy(cloudEnabled = checked))
                            },
                            supportingText = stringResource(RStrings.enable_cloud_history_desc),
                        )
                        SettingSwitchRow(
                            title = stringResource(RStrings.history_auto_clean),
                            icon = Icons.Rounded.AutoDelete,
                            checked = historySettings.autoClean,
                            onCheckedChange = { checked ->
                                onHistorySettingsChange(historySettings.copy(autoClean = checked))
                            },
                            supportingText = stringResource(RStrings.history_auto_clean_desc),
                        )
                        SettingSwitchRow(
                            title = stringResource(RStrings.history_unlimited),
                            icon = Icons.Rounded.AllInclusive,
                            checked = historySettings.unlimited,
                            onCheckedChange = { checked ->
                                onHistorySettingsChange(historySettings.copy(unlimited = checked))
                            },
                            supportingText = stringResource(RStrings.history_unlimited_desc),
                        )
                        AnimatedVisibility(
                            visible = !historySettings.unlimited,
                            enter = slideInVertically { -it / 3 } +
                                expandVertically(expandFrom = Alignment.Top) + fadeIn(),
                            exit = slideOutVertically { -it / 3 } +
                                shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut(),
                        ) {
                            HistoryLimitSetting(
                                selectedLimit = historySettings.maxEntries,
                                onLimitChange = { limit ->
                                    onHistorySettingsChange(historySettings.copy(maxEntries = limit))
                                },
                            )
                        }
                        SettingRow(
                            title = stringResource(RStrings.clear_local_history),
                            icon = Icons.Rounded.Delete,
                            onClick = rememberThrottleClick(onClick = onClearLocalHistory),
                            supportingContent = {
                                Text(text = stringResource(RStrings.clear_local_history_desc))
                            },
                        )
                    }
                }
            }
        }
    }
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
    val onInputChange: (String) -> Unit = { value ->
        val digits = value
            .filter { it.isDigit() }
            .take(HistorySettings.MAX_ENTRIES.toString().length)
        input = digits
        digits.toIntOrNull()
            ?.takeIf { it in validLimitRange }
            ?.let(onLimitChange)
    }

    val field = remember {
        movableContentOf<String, Boolean, (String) -> Unit> { value, isError, onValueChange ->
            OutlinedTextField(
                modifier = Modifier.width(128.dp),
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                isError = isError,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
        }
    }
    BoxWithConstraints {
        val stackField = maxWidth < 350.dp * LocalDensity.current.fontScale
        val inputField: @Composable () -> Unit = { field(input, isInputError, onInputChange) }
        SettingRow(
            title = stringResource(RStrings.history_max_entries),
            icon = Icons.AutoMirrored.Rounded.ViewList,
            supportingContent = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(
                            RStrings.history_max_entries_desc,
                            validLimitRange.first,
                            validLimitRange.last,
                        )
                    )
                    if (stackField) inputField()
                }
            },
            trailingContent = if (stackField) null else inputField,
        )
    }
}
