package com.mrl.pixiv.setting.appdata

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Upload
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mrl.pixiv.common.compose.rememberThrottleClick
import com.mrl.pixiv.common.router.NavigationManager
import com.mrl.pixiv.common.router.currentNavigationManager
import com.mrl.pixiv.common.util.RStrings
import com.mrl.pixiv.common.viewmodel.asState
import com.mrl.pixiv.setting.components.SettingPageColumn
import com.mrl.pixiv.setting.components.SettingRow
import com.mrl.pixiv.strings.cancel
import com.mrl.pixiv.strings.clear_cache
import com.mrl.pixiv.strings.confirm
import com.mrl.pixiv.strings.data_storage_title
import com.mrl.pixiv.strings.export_data
import com.mrl.pixiv.strings.history_import_user_mismatch_desc
import com.mrl.pixiv.strings.history_import_user_mismatch_title
import com.mrl.pixiv.strings.import_data
import com.mrl.pixiv.strings.migrate_data_desc
import com.mrl.pixiv.strings.migrate_data_title
import io.github.vinceglb.filekit.dialogs.FileKitDialogSettings
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.dialogs.compose.rememberFileSaverLauncher
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import kotlin.time.Clock

private data class HistoryImportDialogData(
    val requestId: Long,
    val currentUserId: Long,
    val importUserId: Long,
)

@Composable
fun AppDataScreen(
    modifier: Modifier = Modifier,
    navigationManager: NavigationManager = currentNavigationManager(),
    viewModel: AppDataViewModel = koinViewModel(),
) {
    val state = viewModel.asState()
    var dialogData by remember { mutableStateOf<HistoryImportDialogData?>(null) }

    LaunchedEffect(Unit) {
        viewModel.sideEffect.collect { effect ->
            if (effect is ConfirmHistoryImportEffect) {
                dialogData = HistoryImportDialogData(
                    requestId = effect.requestId,
                    currentUserId = effect.currentUserId,
                    importUserId = effect.importUserId,
                )
            }
        }
    }

    val exportLauncher = rememberFileSaverLauncher(
        dialogSettings = FileKitDialogSettings.createDefault()
    ) { file ->
        file?.let { viewModel.exportData(it) }
    }

    val importLauncher = rememberFilePickerLauncher(
        type = FileKitType.File(extensions = listOf("zip"))
    ) { file ->
        file?.let { viewModel.importData(it) }
    }

    AppDataContent(
        cacheSize = viewModel.cacheDirSize,
        onExport = {
            val fileName = "pixiv_data_backup_${
                Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                    .format(LocalDateTime.Formats.ISO)
            }"
            exportLauncher.launch(suggestedName = fileName, defaultExtension = "zip")
        },
        onImport = { importLauncher.launch() },
        onClearCache = viewModel::clearCache,
        onBack = navigationManager::popBackStack,
        modifier = modifier,
    ) {
        MigrationCard(
            state = state,
            migrateData = viewModel::migrateData,
            viewModel = viewModel,
        )
    }

    dialogData?.let { data ->
        AlertDialog(
            onDismissRequest = {
                viewModel.onHistoryImportConfirm(data.requestId, false)
                dialogData = null
            },
            title = {
                Text(text = stringResource(RStrings.history_import_user_mismatch_title))
            },
            text = {
                Text(
                    text = stringResource(
                        RStrings.history_import_user_mismatch_desc,
                        data.currentUserId,
                        data.importUserId
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.onHistoryImportConfirm(data.requestId, true)
                        dialogData = null
                    }
                ) {
                    Text(stringResource(RStrings.confirm))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        viewModel.onHistoryImportConfirm(data.requestId, false)
                        dialogData = null
                    }
                ) {
                    Text(stringResource(RStrings.cancel))
                }
            }
        )
    }
}

@Composable
internal fun AppDataContent(
    cacheSize: String,
    onExport: () -> Unit,
    onImport: () -> Unit,
    onClearCache: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    migrationContent: @Composable () -> Unit = {},
) {
    Scaffold(
        modifier = modifier,
        topBar = { AppDataTopAppBar(onBack = onBack) },
    ) { paddingValues ->
        SettingPageColumn(paddingValues) {
            migrationContent()
            SettingRow(
                title = stringResource(RStrings.export_data),
                icon = Icons.Rounded.Upload,
                onClick = onExport,
            )
            SettingRow(
                title = stringResource(RStrings.import_data),
                icon = Icons.Rounded.Download,
                onClick = onImport,
            )
            SettingRow(
                title = stringResource(RStrings.clear_cache, cacheSize),
                icon = Icons.Rounded.Delete,
                onClick = rememberThrottleClick(onClick = onClearCache),
            )
        }
    }
}

@Composable
internal fun AppDataMigrationNotice(oldImageCount: Int, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.Warning, contentDescription = null)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(RStrings.migrate_data_title),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = stringResource(RStrings.migrate_data_desc, oldImageCount),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
expect fun MigrationCard(
    state: AppDataState,
    migrateData: () -> Unit,
    viewModel: AppDataViewModel,
)

@Composable
internal fun AppDataTopAppBar(onBack: () -> Unit) {
    TopAppBar(
        title = {
            Text(text = stringResource(RStrings.data_storage_title))
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
}
