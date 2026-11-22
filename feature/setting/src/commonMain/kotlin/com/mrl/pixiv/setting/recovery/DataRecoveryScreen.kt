package com.mrl.pixiv.setting.recovery

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import co.touchlab.kermit.Logger
import com.mrl.pixiv.common.mmkv.AccountStore
import com.mrl.pixiv.common.repository.recovery.ItemStatus
import com.mrl.pixiv.common.repository.recovery.MigrationItem
import com.mrl.pixiv.common.repository.recovery.MigrationReason
import com.mrl.pixiv.common.repository.recovery.isResolved
import com.mrl.pixiv.common.util.RStrings
import com.mrl.pixiv.common.util.ToastUtil
import com.mrl.pixiv.common.viewmodel.SideEffect
import com.mrl.pixiv.common.viewmodel.asState
import com.mrl.pixiv.setting.components.SettingPageColumn
import com.mrl.pixiv.strings.back
import com.mrl.pixiv.strings.cancel
import com.mrl.pixiv.strings.confirm
import com.mrl.pixiv.strings.history
import com.mrl.pixiv.strings.read_later
import com.mrl.pixiv.strings.reading_progress
import com.mrl.pixiv.strings.recovery_abandon
import com.mrl.pixiv.strings.recovery_abandon_description
import com.mrl.pixiv.strings.recovery_export
import com.mrl.pixiv.strings.recovery_export_description
import com.mrl.pixiv.strings.recovery_finish
import com.mrl.pixiv.strings.recovery_import
import com.mrl.pixiv.strings.recovery_import_description
import com.mrl.pixiv.strings.recovery_item_legacy_blocks
import com.mrl.pixiv.strings.recovery_item_search_tags
import com.mrl.pixiv.strings.recovery_item_series_progress
import com.mrl.pixiv.strings.recovery_item_session
import com.mrl.pixiv.strings.recovery_item_shared_data
import com.mrl.pixiv.strings.recovery_item_translations
import com.mrl.pixiv.strings.recovery_operation_failed
import com.mrl.pixiv.strings.recovery_reason_corrupted
import com.mrl.pixiv.strings.recovery_reason_io
import com.mrl.pixiv.strings.recovery_reason_storage
import com.mrl.pixiv.strings.recovery_skip
import com.mrl.pixiv.strings.recovery_status_abandoned
import com.mrl.pixiv.strings.recovery_status_pending
import com.mrl.pixiv.strings.recovery_status_skipped
import com.mrl.pixiv.strings.recovery_target_description
import com.mrl.pixiv.strings.recovery_target_title
import com.mrl.pixiv.strings.recovery_title
import com.mrl.pixiv.strings.retry
import com.mrl.pixiv.strings.setting
import com.mrl.pixiv.strings.status_completed
import com.mrl.pixiv.strings.status_failed
import com.mrl.pixiv.strings.status_running
import com.mrl.pixiv.strings.unknown_error
import io.github.vinceglb.filekit.dialogs.FileKitDialogSettings
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.dialogs.compose.rememberFileSaverLauncher
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun DataRecoveryScreen(
    afterLogin: Boolean,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel = koinViewModel<DataRecoveryViewModel>()
    val state = viewModel.asState()
    val userId = remember { AccountStore.activeUserId.takeIf { it > 0 } }
    var showAbandon by rememberSaveable { mutableStateOf(false) }
    var showImportItems by rememberSaveable { mutableStateOf(false) }
    var importItemName by rememberSaveable { mutableStateOf<String?>(null) }
    val exportLauncher = rememberFileSaverLauncher(dialogSettings = FileKitDialogSettings.createDefault()) { file ->
        if (file != null) viewModel.dispatch(RecoveryIntent.Export(file))
    }
    val importLauncher = rememberFilePickerLauncher(
        type = FileKitType.File(extensions = listOf("zip")),
    ) { file ->
        val item = importItemName?.let(MigrationItem::valueOf)
        importItemName = null
        if (file != null && item != null && userId != null) {
            viewModel.dispatch(RecoveryIntent.Import(file, item, userId))
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.sideEffect.collect { effect ->
            when (effect) {
                is RecoveryMessage -> ToastUtil.safeShortToast(effect.text)
                is SideEffect.Error -> {
                    Logger.w(tag = "DataRecovery") { "Recovery operation failed (${effect.throwable::class.simpleName})" }
                    ToastUtil.safeShortToast(RStrings.recovery_operation_failed)
                }
            }
        }
    }

    DataRecoveryContent(
        state = state,
        afterLogin = afterLogin,
        canImport = userId != null,
        onRetry = { viewModel.dispatch(RecoveryIntent.Retry(it, userId)) },
        onExport = { exportLauncher.launch(suggestedName = "pixiv_legacy_data", defaultExtension = "zip") },
        onImport = { showImportItems = true },
        onAbandon = { showAbandon = true },
        onBack = onBack,
        modifier = modifier,
    )

    state.retryTarget?.let { target ->
        RecoveryTargetDialog(
            userId = target.userId,
            onConfirm = { viewModel.dispatch(RecoveryIntent.ConfirmRetry(target)) },
            onDismiss = { viewModel.dispatch(RecoveryIntent.DismissRetry) },
        )
    }
    if (showAbandon) {
        RecoveryAbandonDialog(
            onConfirm = {
                showAbandon = false
                viewModel.dispatch(RecoveryIntent.Abandon)
            },
            onDismiss = { showAbandon = false },
        )
    }
    if (showImportItems && userId != null) {
        ImportItemDialog(
            userId = userId,
            onSelect = {
                showImportItems = false
                importItemName = it.name
                importLauncher.launch()
            },
            onDismiss = { showImportItems = false },
        )
    }
}

@Composable
internal fun DataRecoveryContent(
    state: DataRecoveryState,
    afterLogin: Boolean,
    canImport: Boolean,
    onRetry: (MigrationItem) -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit,
    onAbandon: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier) {
        val stackActions = maxWidth / LocalDensity.current.fontScale < 280.dp
        Scaffold(
            topBar = {
                Column {
                    RecoveryTopBar(
                        afterLogin = afterLogin,
                        stackActions = stackActions,
                        hasPending = state.hasPending,
                        enabled = !state.isBusy,
                        onBack = onBack,
                    )
                    if (state.isBusy && state.activeItem == null) {
                        LinearProgressIndicator(Modifier.fillMaxWidth())
                    }
                }
            },
        ) { innerPadding ->
            SettingPageColumn(innerPadding) {
                val orderedItems = state.items.entries.sortedBy { it.value.isResolved }
                orderedItems.forEachIndexed { index, (item, status) ->
                    if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    RecoveryItemRow(
                        item = item,
                        status = status,
                        isRunning = state.activeItem == item,
                        stackActions = stackActions,
                        canRetry = !state.isBusy,
                        onRetry = { onRetry(item) },
                    )
                }
                RecoveryActions(
                    canImport = canImport,
                    canAbandon = state.hasPending,
                    enabled = !state.isBusy,
                    onExport = onExport,
                    onImport = onImport,
                    onAbandon = onAbandon,
                    modifier = Modifier.padding(top = 16.dp),
                )
            }
        }
    }
}

@Composable
private fun RecoveryTopBar(
    afterLogin: Boolean,
    stackActions: Boolean,
    hasPending: Boolean,
    enabled: Boolean,
    onBack: () -> Unit,
) {
    val title: @Composable () -> Unit = {
        val style = if (stackActions) MaterialTheme.typography.titleLarge else LocalTextStyle.current
        Text(stringResource(RStrings.recovery_title), style = style.copy(hyphens = Hyphens.Auto))
    }
    val navigationIcon: @Composable () -> Unit = {
        if (!afterLogin) {
            IconButton(onClick = onBack, enabled = enabled, shapes = IconButtonDefaults.shapes()) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(RStrings.back))
            }
        }
    }
    val actions: @Composable RowScope.() -> Unit = {
        if (afterLogin) {
            TextButton(onClick = onBack, enabled = enabled) {
                Text(stringResource(if (hasPending) RStrings.recovery_skip else RStrings.recovery_finish))
            }
        }
    }
    if (stackActions) {
        MediumTopAppBar(title = title, navigationIcon = navigationIcon, actions = actions)
    } else {
        TopAppBar(title = title, navigationIcon = navigationIcon, actions = actions)
    }
}

@Composable
private fun RecoveryItemRow(
    item: MigrationItem,
    status: ItemStatus,
    isRunning: Boolean,
    stackActions: Boolean,
    canRetry: Boolean,
    onRetry: () -> Unit,
) {
    val retry: @Composable (Modifier) -> Unit = { modifier ->
        TextButton(onClick = onRetry, modifier = modifier, enabled = canRetry) {
            Text(stringResource(RStrings.retry))
        }
    }
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (stackActions) {
            RecoveryItemDescription(item, status, isRunning)
            if (!status.isResolved) retry(Modifier.align(Alignment.End))
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RecoveryItemDescription(item, status, isRunning, modifier = Modifier.weight(1f))
                if (!status.isResolved) retry(Modifier)
            }
        }
        if (isRunning) LinearProgressIndicator(Modifier.fillMaxWidth())
    }
}

@Composable
private fun RecoveryItemDescription(
    item: MigrationItem,
    status: ItemStatus,
    isRunning: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        val bodyStyle = MaterialTheme.typography.bodyMedium.copy(hyphens = Hyphens.Auto)
        Text(
            text = stringResource(item.title),
            style = MaterialTheme.typography.titleMedium.copy(hyphens = Hyphens.Auto),
        )
        Text(
            text = stringResource(if (isRunning) RStrings.status_running else status.title),
            color = if (status is ItemStatus.Failed && !isRunning) MaterialTheme.colorScheme.error
            else MaterialTheme.colorScheme.onSurfaceVariant,
            style = bodyStyle,
        )
        val reason = when (status) {
            is ItemStatus.Failed -> status.reason
            is ItemStatus.Pending -> status.reason
            else -> null
        }
        if (reason != null && !isRunning) {
            Text(
                text = stringResource(reason.title),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = bodyStyle,
            )
        }
    }
}

@Composable
internal fun RecoveryActions(
    canImport: Boolean,
    canAbandon: Boolean,
    enabled: Boolean,
    onExport: () -> Unit,
    onImport: () -> Unit,
    onAbandon: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(RStrings.recovery_export_description),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
        Button(onClick = onExport, enabled = enabled, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(RStrings.recovery_export), textAlign = TextAlign.Center)
        }
        if (canImport) {
            OutlinedButton(onClick = onImport, enabled = enabled, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(RStrings.recovery_import), textAlign = TextAlign.Center)
            }
        }
        if (canAbandon) {
            TextButton(
                onClick = onAbandon,
                enabled = enabled,
                modifier = Modifier.align(Alignment.CenterHorizontally),
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
            ) {
                Text(stringResource(RStrings.recovery_abandon), textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
internal fun RecoveryTargetDialog(userId: Long, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    RecoveryConfirmation(
        title = RStrings.recovery_target_title,
        description = stringResource(RStrings.recovery_target_description, userId),
        confirmLabel = RStrings.confirm,
        onConfirm = onConfirm,
        onDismiss = onDismiss,
    )
}

@Composable
private fun RecoveryAbandonDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    RecoveryConfirmation(
        title = RStrings.recovery_abandon,
        description = stringResource(RStrings.recovery_abandon_description),
        confirmLabel = RStrings.recovery_abandon,
        onConfirm = onConfirm,
        onDismiss = onDismiss,
    )
}

@Composable
private fun RecoveryConfirmation(
    title: StringResource,
    description: String,
    confirmLabel: StringResource,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(title)) },
        text = { Text(description) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(stringResource(confirmLabel)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(RStrings.cancel)) } },
    )
}

@Composable
private fun ImportItemDialog(userId: Long, onSelect: (MigrationItem) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(RStrings.recovery_import)) },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(stringResource(RStrings.recovery_import_description, userId))
                for (item in MigrationItem.entries) {
                    if (item != MigrationItem.Session) {
                        TextButton(onClick = { onSelect(item) }, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(item.title))
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(RStrings.cancel)) } },
    )
}

private val MigrationItem.title: StringResource
    get() = when (this) {
        MigrationItem.Session -> RStrings.recovery_item_session
        MigrationItem.Settings -> RStrings.setting
        MigrationItem.SearchAndTags -> RStrings.recovery_item_search_tags
        MigrationItem.SeriesProgress -> RStrings.recovery_item_series_progress
        MigrationItem.GlobalDatabase -> RStrings.recovery_item_shared_data
        MigrationItem.BrowsingHistory -> RStrings.history
        MigrationItem.ReadingProgress -> RStrings.reading_progress
        MigrationItem.ReadLater -> RStrings.read_later
        MigrationItem.TranslationCache -> RStrings.recovery_item_translations
        MigrationItem.LegacyBlocking -> RStrings.recovery_item_legacy_blocks
    }

private val ItemStatus.title: StringResource
    get() = when (this) {
        ItemStatus.Done -> RStrings.status_completed
        ItemStatus.Skipped -> RStrings.recovery_status_skipped
        ItemStatus.Abandoned -> RStrings.recovery_status_abandoned
        is ItemStatus.Pending -> RStrings.recovery_status_pending
        is ItemStatus.Failed -> RStrings.status_failed
    }

private val MigrationReason.title: StringResource
    get() = when (this) {
        MigrationReason.StorageSpace -> RStrings.recovery_reason_storage
        MigrationReason.Corrupted -> RStrings.recovery_reason_corrupted
        MigrationReason.Io -> RStrings.recovery_reason_io
        MigrationReason.Unknown -> RStrings.unknown_error
    }
