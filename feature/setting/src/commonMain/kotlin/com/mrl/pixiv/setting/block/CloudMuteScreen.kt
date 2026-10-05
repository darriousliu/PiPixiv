package com.mrl.pixiv.setting.block

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mrl.pixiv.common.router.currentNavigationManager
import com.mrl.pixiv.common.util.RStrings
import com.mrl.pixiv.strings.*
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun CloudMuteScreen(
    modifier: Modifier = Modifier,
    viewModel: CloudMuteViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val navigation = currentNavigationManager()
    var addKind by rememberSaveable { mutableStateOf<CloudMuteInputKind?>(null) }
    var input by rememberSaveable { mutableStateOf("") }
    var removeUserId by rememberSaveable { mutableStateOf<Long?>(null) }
    var removeTag by rememberSaveable { mutableStateOf<String?>(null) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(RStrings.cloud_mute_title)) },
                navigationIcon = {
                    IconButton(onClick = navigation::popBackStack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(RStrings.cloud_mute_back))
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::refresh, enabled = !state.busy) {
                        Icon(Icons.Rounded.Refresh, contentDescription = stringResource(RStrings.cloud_mute_refresh))
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(stringResource(RStrings.cloud_mute_description), style = MaterialTheme.typography.bodyMedium)
            }
            if (state.busy) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
            if (state.failed) item {
                Column {
                    Text(
                        stringResource(if (state.savedRefreshFailed) RStrings.cloud_mute_saved_refresh_failed else RStrings.cloud_mute_failed),
                        color = MaterialTheme.colorScheme.error,
                    )
                    TextButton(onClick = viewModel::refresh, enabled = !state.busy) {
                        Text(stringResource(RStrings.cloud_mute_refresh))
                    }
                }
            }
            state.importedCount?.let { count ->
                item { Text(stringResource(RStrings.cloud_mute_imported, count)) }
            }
            state.data?.let { data ->
                item {
                    Text(stringResource(RStrings.cloud_mute_quota, data.mutedCount, data.muteLimitCount))
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { input = ""; addKind = CloudMuteInputKind.Tag },
                            enabled = !state.busy && data.mutedCount < data.muteLimitCount,
                            modifier = Modifier.weight(1f),
                        ) { Text(stringResource(RStrings.cloud_mute_add_tag)) }
                        Button(
                            onClick = { input = ""; addKind = CloudMuteInputKind.User },
                            enabled = !state.busy && data.mutedCount < data.muteLimitCount,
                            modifier = Modifier.weight(1f),
                        ) { Text(stringResource(RStrings.cloud_mute_add_user)) }
                    }
                }
                item {
                    OutlinedButton(onClick = viewModel::importToLocal, enabled = !state.busy) {
                        Text(stringResource(RStrings.cloud_mute_import))
                    }
                }
                item { HorizontalDivider() }
                item { Text(stringResource(RStrings.block_user), style = MaterialTheme.typography.titleMedium) }
                items(data.mutedUsers.distinctBy { it.user.id }, key = { "user:${it.user.id}" }) { muted ->
                    ListItem(
                        headlineContent = { Text(muted.user.name.ifBlank { muted.user.id.toString() }) },
                        supportingContent = { Text(muted.user.id.toString()) },
                        trailingContent = {
                            IconButton(onClick = { removeUserId = muted.user.id }, enabled = !state.busy) {
                                Icon(Icons.Rounded.Delete, contentDescription = stringResource(RStrings.cloud_mute_remove))
                            }
                        },
                    )
                }
                if (data.mutedUsers.isEmpty()) item { Text(stringResource(RStrings.no_blocked_items)) }
                item { Text(stringResource(RStrings.block_tags), style = MaterialTheme.typography.titleMedium) }
                items(data.mutedTags.distinctBy { it.tag.name }, key = { "tag:${it.tag.name}" }) { muted ->
                    ListItem(
                        headlineContent = { Text(muted.tag.name) },
                        trailingContent = {
                            IconButton(onClick = { removeTag = muted.tag.name }, enabled = !state.busy) {
                                Icon(Icons.Rounded.Delete, contentDescription = stringResource(RStrings.cloud_mute_remove))
                            }
                        },
                    )
                }
                if (data.mutedTags.isEmpty()) item { Text(stringResource(RStrings.no_blocked_items)) }
            }
        }
    }
    addKind?.let { kind ->
        AlertDialog(
            onDismissRequest = { addKind = null },
            title = { Text(stringResource(if (kind == CloudMuteInputKind.Tag) RStrings.cloud_mute_add_tag else RStrings.cloud_mute_add_user)) },
            text = {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    label = { Text(stringResource(if (kind == CloudMuteInputKind.Tag) RStrings.cloud_mute_tag else RStrings.cloud_mute_user_id)) },
                    supportingText = { Text(stringResource(RStrings.cloud_mute_input_hint)) },
                    keyboardOptions = KeyboardOptions(keyboardType = if (kind == CloudMuteInputKind.User) KeyboardType.Number else KeyboardType.Text),
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.add(kind, input); addKind = null },
                    enabled = !state.busy && canAddCloudMute(state.data, kind, input),
                ) { Text(stringResource(RStrings.confirm)) }
            },
            dismissButton = { TextButton(onClick = { addKind = null }) { Text(stringResource(RStrings.cancel)) } },
        )
    }
    if (removeUserId != null || removeTag != null) {
        AlertDialog(
            onDismissRequest = { removeUserId = null; removeTag = null },
            title = { Text(stringResource(RStrings.cloud_mute_remove)) },
            text = { Text(stringResource(RStrings.cloud_mute_remove_description, removeTag ?: removeUserId.toString())) },
            confirmButton = {
                TextButton(
                    enabled = !state.busy && state.data != null,
                    onClick = {
                        removeUserId?.let(viewModel::removeUser)
                        removeTag?.let(viewModel::removeTag)
                        removeUserId = null
                        removeTag = null
                    },
                ) { Text(stringResource(RStrings.confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { removeUserId = null; removeTag = null }) { Text(stringResource(RStrings.cancel)) }
            },
        )
    }
}
