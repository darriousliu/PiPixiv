package com.mrl.pixiv.setting.applog

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import co.touchlab.kermit.Severity
import com.mrl.pixiv.common.analytics.FLAVOR
import com.mrl.pixiv.common.compose.ui.ContentEmptyState
import com.mrl.pixiv.common.logs.AppLogEntry
import com.mrl.pixiv.common.logs.AppLogHeader
import com.mrl.pixiv.common.logs.AppLogSnapshot
import com.mrl.pixiv.common.logs.AppLogStore
import com.mrl.pixiv.common.logs.formatCopiedLogs
import com.mrl.pixiv.common.router.NavigationManager
import com.mrl.pixiv.common.router.currentNavigationManager
import com.mrl.pixiv.common.util.AppUtil
import com.mrl.pixiv.common.util.DeviceInfo
import com.mrl.pixiv.common.util.RStrings
import com.mrl.pixiv.common.util.ShareUtil
import com.mrl.pixiv.common.util.ToastUtil
import com.mrl.pixiv.common.util.copyToClipboard
import com.mrl.pixiv.common.util.isDesktop
import com.mrl.pixiv.common.util.platform
import com.mrl.pixiv.setting.components.SettingSectionTitle
import com.mrl.pixiv.strings.all
import com.mrl.pixiv.strings.app_logs
import com.mrl.pixiv.strings.app_logs_clear
import com.mrl.pixiv.strings.app_logs_clear_confirmation
import com.mrl.pixiv.strings.app_logs_collapse
import com.mrl.pixiv.strings.app_logs_copied
import com.mrl.pixiv.strings.app_logs_copy_all
import com.mrl.pixiv.strings.app_logs_current
import com.mrl.pixiv.strings.app_logs_empty
import com.mrl.pixiv.strings.app_logs_errors
import com.mrl.pixiv.strings.app_logs_expand
import com.mrl.pixiv.strings.app_logs_export_failed
import com.mrl.pixiv.strings.app_logs_local_notice
import com.mrl.pixiv.strings.app_logs_omitted
import com.mrl.pixiv.strings.app_logs_previous
import com.mrl.pixiv.strings.app_logs_unknown_device
import com.mrl.pixiv.strings.app_logs_warnings
import com.mrl.pixiv.strings.back
import com.mrl.pixiv.strings.cancel
import com.mrl.pixiv.strings.clear
import com.mrl.pixiv.strings.copy_to_clipboard
import com.mrl.pixiv.strings.more
import com.mrl.pixiv.strings.share
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource

@Composable
fun AppLogScreen(
    modifier: Modifier = Modifier,
    navigationManager: NavigationManager = currentNavigationManager(),
) {
    val logs = AppLogStore.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    fun exportLogs(entries: List<AppLogEntry>, share: Boolean) {
        scope.launch { exportAppLogs(entries, share) }
    }

    AppLogContent(
        snapshot = logs.value,
        onBack = navigationManager::popBackStack,
        onCopyAll = {
            val snapshot = logs.value
            exportLogs(snapshot.previous + snapshot.current, share = false)
        },
        onShare = {
            val snapshot = logs.value
            exportLogs(snapshot.previous + snapshot.current, share = true)
        },
        onClear = { AppLogStore.clear() },
        onCopyEntry = { exportLogs(listOf(it), share = false) },
        modifier = modifier,
    )
}

suspend fun exportAppLogs(entries: List<AppLogEntry>, share: Boolean = true) {
    if (entries.isEmpty()) return
    try {
        val device = DeviceInfo.appLogDetails()
        val header = AppLogHeader(
            appVersion = AppUtil.versionName,
            appBuild = AppUtil.versionCode.toString(),
            flavor = FLAVOR,
            platform = device.platform,
            osVersion = device.osVersion,
            deviceModel = device.model ?: getString(RStrings.app_logs_unknown_device),
        )
        val omitted = getString(RStrings.app_logs_omitted)
        val text = withContext(Dispatchers.Default) {
            val shareable = entries.map { entry ->
                if (entry.tag == "StartupMigration" && entry.message.startsWith("MigrationTiming ")) {
                    entry.copy(message = entry.message.replace(migrationAvailableSpace, "\"availableBytes\":null"))
                } else {
                    entry
                }
            }
            formatCopiedLogs(header, shareable) { count -> omitted.replace("%1\$d", count.toString()) }
        }
        if (share) ShareUtil.shareText(text) else copyToClipboard(text)
        if (!share || platform.isDesktop()) {
            ToastUtil.safeShortToast(RStrings.app_logs_copied)
        }
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Exception) {
        ToastUtil.safeShortToast(RStrings.app_logs_export_failed)
    }
}

private val migrationAvailableSpace = Regex(""""availableBytes":\d+""")

private enum class AppLogFilter(val label: StringResource, val minimum: Severity) {
    All(RStrings.all, Severity.Info),
    Warnings(RStrings.app_logs_warnings, Severity.Warn),
    Errors(RStrings.app_logs_errors, Severity.Error),
}

@Composable
internal fun AppLogContent(
    snapshot: AppLogSnapshot,
    onBack: () -> Unit,
    onCopyAll: () -> Unit,
    onShare: () -> Unit,
    onClear: () -> Unit,
    onCopyEntry: (AppLogEntry) -> Unit,
    modifier: Modifier = Modifier,
) {
    var filterName by rememberSaveable { mutableStateOf(AppLogFilter.All.name) }
    val filter = AppLogFilter.valueOf(filterName)
    var confirmClear by rememberSaveable { mutableStateOf(false) }
    val hasLogs = snapshot.current.isNotEmpty() || snapshot.previous.isNotEmpty()
    val current = remember(snapshot.current, filter) {
        snapshot.current.asReversed().filter { it.level >= filter.minimum }
    }
    val previous = remember(snapshot.previous, filter) {
        snapshot.previous.asReversed().filter { it.level >= filter.minimum }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            BoxWithConstraints {
                val compactActions = maxWidth < 360.dp ||
                    (maxWidth < 480.dp && LocalDensity.current.fontScale >= 1.25f)
                TopAppBar(
                    title = {
                        Text(stringResource(RStrings.app_logs), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack, shapes = IconButtonDefaults.shapes()) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(RStrings.back))
                        }
                    },
                    actions = {
                        AppLogActions(
                            compact = compactActions,
                            enabled = hasLogs,
                            onCopyAll = onCopyAll,
                            onShare = onShare,
                            onClear = { confirmClear = true },
                        )
                    },
                )
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(innerPadding).consumeWindowInsets(innerPadding),
            contentAlignment = Alignment.TopCenter,
        ) {
            LazyColumn(
                modifier = Modifier.widthIn(max = 640.dp).fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                item(key = "privacy", contentType = "notice") {
                    Text(
                        text = stringResource(RStrings.app_logs_local_notice),
                        modifier = Modifier.padding(horizontal = 4.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                item(key = "filters", contentType = "filters") {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AppLogFilter.entries.forEach { option ->
                            FilterChip(
                                selected = filter == option,
                                onClick = { filterName = option.name },
                                label = { Text(stringResource(option.label)) },
                            )
                        }
                    }
                }
                if (current.isEmpty() && previous.isEmpty()) {
                    item(key = "empty", contentType = "empty") {
                        ContentEmptyState(message = stringResource(RStrings.app_logs_empty))
                    }
                } else {
                    appLogGroup(current, "current", RStrings.app_logs_current, onCopyEntry)
                    appLogGroup(previous, "previous", RStrings.app_logs_previous, onCopyEntry)
                }
            }
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text(stringResource(RStrings.app_logs_clear)) },
            text = { Text(stringResource(RStrings.app_logs_clear_confirmation)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmClear = false
                    onClear()
                }) { Text(stringResource(RStrings.clear)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) { Text(stringResource(RStrings.cancel)) }
            },
        )
    }
}

private data class AppLogAction(
    val label: StringResource,
    val icon: ImageVector,
    val onClick: () -> Unit,
)

@Composable
private fun AppLogActions(
    compact: Boolean,
    enabled: Boolean,
    onCopyAll: () -> Unit,
    onShare: () -> Unit,
    onClear: () -> Unit,
) {
    val actions = listOf(
        AppLogAction(RStrings.app_logs_copy_all, Icons.Rounded.ContentCopy, onCopyAll),
        AppLogAction(RStrings.share, Icons.Rounded.Share, onShare),
        AppLogAction(RStrings.app_logs_clear, Icons.Rounded.Delete, onClear),
    )
    if (compact) {
        var expanded by remember { mutableStateOf(false) }
        Box {
            IconButton(onClick = { expanded = true }, enabled = enabled, shapes = IconButtonDefaults.shapes()) {
                Icon(Icons.Rounded.MoreVert, stringResource(RStrings.more))
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                for (action in actions) {
                    DropdownMenuItem(
                        text = { Text(stringResource(action.label)) },
                        leadingIcon = { Icon(action.icon, contentDescription = null) },
                        onClick = {
                            expanded = false
                            action.onClick()
                        },
                    )
                }
            }
        }
    } else {
        for (action in actions) {
            IconButton(onClick = action.onClick, enabled = enabled, shapes = IconButtonDefaults.shapes()) {
                Icon(action.icon, stringResource(action.label))
            }
        }
    }
}

private fun LazyListScope.appLogGroup(
    entries: List<AppLogEntry>,
    group: String,
    title: StringResource,
    onCopyEntry: (AppLogEntry) -> Unit,
) {
    if (entries.isEmpty()) return
    item(key = group, contentType = "group") { SettingSectionTitle(stringResource(title)) }
    items(entries, key = { "$group-${it.id}" }, contentType = { "entry" }) { entry ->
        var expanded by rememberSaveable { mutableStateOf(false) }
        AppLogRow(
            entry = entry,
            expanded = expanded,
            onToggle = { expanded = !expanded },
            onCopy = { onCopyEntry(entry) },
        )
    }
}

@Composable
internal fun AppLogRow(
    entry: AppLogEntry,
    expanded: Boolean,
    onToggle: () -> Unit,
    onCopy: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val hasStackTrace = !entry.stackTrace.isNullOrEmpty()
    val levelColor = when (entry.level) {
        Severity.Assert, Severity.Error -> MaterialTheme.colorScheme.error
        Severity.Warn -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.primary
    }
    val toggleLabel = if (hasStackTrace) {
        stringResource(if (expanded) RStrings.app_logs_collapse else RStrings.app_logs_expand)
    } else {
        null
    }
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = MaterialTheme.shapes.large,
    ) {
        Column(
            modifier = Modifier.combinedClickable(
                onClick = { if (hasStackTrace) onToggle() },
                onClickLabel = toggleLabel,
                onLongClick = onCopy,
                onLongClickLabel = stringResource(RStrings.copy_to_clipboard),
            ).padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                FlowRow(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(entry.level.name.take(1), style = MaterialTheme.typography.labelMedium, color = levelColor)
                    Text(
                        text = entry.timestamp.replace(displayTimestampFormat, "$1 $2 $3"),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(entry.tag, style = MaterialTheme.typography.labelMedium)
                }
                if (hasStackTrace) {
                    Icon(
                        if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                        contentDescription = null,
                        modifier = Modifier.padding(start = 4.dp).size(20.dp),
                    )
                }
            }
            Text(entry.message, style = MaterialTheme.typography.bodyMedium)
            if (expanded && hasStackTrace) {
                Text(
                    text = entry.stackTrace.orEmpty(),
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private val displayTimestampFormat = Regex("""^(\d{4}-\d{2}-\d{2})T(\d{2}:\d{2}:\d{2})(?:\.\d+)?(Z|[+-].+)$""")
