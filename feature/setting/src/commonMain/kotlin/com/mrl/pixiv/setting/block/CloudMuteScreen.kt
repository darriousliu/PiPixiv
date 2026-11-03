package com.mrl.pixiv.setting.block

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Tag
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mrl.pixiv.common.compose.ui.ContentErrorState
import com.mrl.pixiv.common.compose.ui.image.UserAvatar
import com.mrl.pixiv.common.router.currentNavigationManager
import com.mrl.pixiv.common.util.RStrings
import com.mrl.pixiv.strings.block_tags
import com.mrl.pixiv.strings.block_user
import com.mrl.pixiv.strings.cancel
import com.mrl.pixiv.strings.cloud_mute_add_tag
import com.mrl.pixiv.strings.cloud_mute_add_user
import com.mrl.pixiv.strings.cloud_mute_back
import com.mrl.pixiv.strings.cloud_mute_description
import com.mrl.pixiv.strings.cloud_mute_import
import com.mrl.pixiv.strings.cloud_mute_imported
import com.mrl.pixiv.strings.cloud_mute_input_hint
import com.mrl.pixiv.strings.cloud_mute_quota
import com.mrl.pixiv.strings.cloud_mute_refresh
import com.mrl.pixiv.strings.cloud_mute_remove
import com.mrl.pixiv.strings.cloud_mute_remove_description
import com.mrl.pixiv.strings.cloud_mute_saved_refresh_failed
import com.mrl.pixiv.strings.cloud_mute_tag
import com.mrl.pixiv.strings.cloud_mute_title
import com.mrl.pixiv.strings.cloud_mute_user_id
import com.mrl.pixiv.strings.confirm
import com.mrl.pixiv.strings.no_blocked_items
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun CloudMuteScreen(
    modifier: Modifier = Modifier,
    viewModel: CloudMuteViewModel = koinViewModel(),
    embedded: Boolean = false,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val navigation = currentNavigationManager()
    var addKind by rememberSaveable { mutableStateOf<CloudMuteInputKind?>(null) }
    var input by rememberSaveable { mutableStateOf("") }
    var removeUserId by rememberSaveable { mutableStateOf<Long?>(null) }
    var removeTag by rememberSaveable { mutableStateOf<String?>(null) }

    CloudMutePage(
        state = state,
        modifier = modifier,
        onBack = navigation::popBackStack,
        embedded = embedded,
        onRefresh = viewModel::refresh,
        onAdd = { input = ""; addKind = it },
        onImport = viewModel::importToLocal,
        onRemoveUser = { removeUserId = it },
        onRemoveTag = { removeTag = it },
    )
    addKind?.let { kind ->
        AlertDialog(
            onDismissRequest = { addKind = null },
            title = { Text(stringResource(if (kind == CloudMuteInputKind.Tag) RStrings.cloud_mute_add_tag else RStrings.cloud_mute_add_user)) },
            text = {
                CloudMuteInputContent(kind, input, onValueChange = { input = it })
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.add(kind, input); addKind = null },
                    enabled = !state.busy && canAddCloudMute(state.data, kind, input),
                ) { Text(stringResource(RStrings.confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { addKind = null }) {
                    Text(
                        stringResource(
                            RStrings.cancel
                        )
                    )
                }
            },
        )
    }
    if (removeUserId != null || removeTag != null) {
        AlertDialog(
            onDismissRequest = { removeUserId = null; removeTag = null },
            title = { Text(stringResource(RStrings.cloud_mute_remove)) },
            text = {
                Text(
                    stringResource(
                        RStrings.cloud_mute_remove_description,
                        removeTag ?: removeUserId.toString()
                    )
                )
            },
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
                TextButton(onClick = { removeUserId = null; removeTag = null }) {
                    Text(
                        stringResource(RStrings.cancel)
                    )
                }
            },
        )
    }
}


@Composable
private fun CloudMutePage(
    state: CloudMuteState,
    modifier: Modifier = Modifier,
    embedded: Boolean = false,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onAdd: (CloudMuteInputKind) -> Unit,
    onImport: () -> Unit,
    onRemoveUser: (Long) -> Unit,
    onRemoveTag: (String) -> Unit,
) {
    if (embedded) {
        CloudMuteContent(state, modifier, onRefresh, onAdd, onImport, onRemoveUser, onRemoveTag)
    } else {
        Scaffold(
            modifier = modifier,
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(RStrings.cloud_mute_title)) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.AutoMirrored.Rounded.ArrowBack,
                                stringResource(RStrings.cloud_mute_back)
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = onRefresh, enabled = !state.busy) {
                            Icon(Icons.Rounded.Refresh, stringResource(RStrings.cloud_mute_refresh))
                        }
                    },
                )
            },
        ) { padding ->
            CloudMuteContent(
                state, Modifier.padding(padding), onRefresh, onAdd, onImport,
                onRemoveUser, onRemoveTag, showRefreshAction = false,
            )
        }
    }
}

@Composable
internal fun CloudMuteContent(
    state: CloudMuteState,
    modifier: Modifier = Modifier,
    onRefresh: () -> Unit = {},
    onAdd: (CloudMuteInputKind) -> Unit = {},
    onImport: () -> Unit = {},
    onRemoveUser: (Long) -> Unit = {},
    onRemoveTag: (String) -> Unit = {},
    showRefreshAction: Boolean = true,
) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier.widthIn(max = 720.dp).fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 8.dp,
                bottom = 32.dp
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (showRefreshAction) item(key = "refresh") {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onRefresh, enabled = !state.busy) {
                        Icon(Icons.Rounded.Refresh, contentDescription = null)
                        Spacer(Modifier.size(8.dp))
                        Text(stringResource(RStrings.cloud_mute_refresh))
                    }
                }
            }
            item(key = "description") {
                Text(
                    stringResource(RStrings.cloud_mute_description),
                    modifier = Modifier.padding(bottom = 12.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (state.busy) item(key = "loading") { LinearProgressIndicator(Modifier.fillMaxWidth()) }
            if (state.failed) item(key = "error") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (state.savedRefreshFailed) {
                        Text(
                            stringResource(RStrings.cloud_mute_saved_refresh_failed),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    ContentErrorState(
                        onRetry = { if (!state.busy) onRefresh() },
                        compact = state.data != null,
                    )
                }
            }
            state.importedCount?.let { count ->
                item(key = "imported") {
                    Text(
                        stringResource(RStrings.cloud_mute_imported, count),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            state.data?.let { data ->
                item(key = "quota") {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                        Column(
                            Modifier.fillMaxWidth().padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    Icons.Outlined.Cloud,
                                    null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    stringResource(
                                        RStrings.cloud_mute_quota,
                                        data.mutedCount,
                                        data.muteLimitCount
                                    ), style = MaterialTheme.typography.titleSmall
                                )
                            }
                            LinearProgressIndicator(
                                progress = {
                                    if (data.muteLimitCount > 0) (data.mutedCount.toFloat() / data.muteLimitCount).coerceIn(
                                        0f,
                                        1f
                                    ) else 0f
                                },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        HorizontalDivider(
                            Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
                        )
                        val canAdd = !state.busy && data.mutedCount < data.muteLimitCount
                        CloudMuteAddRow(
                            stringResource(RStrings.cloud_mute_add_tag),
                            canAdd
                        ) { onAdd(CloudMuteInputKind.Tag) }
                        CloudMuteAddRow(
                            stringResource(RStrings.cloud_mute_add_user),
                            canAdd
                        ) { onAdd(CloudMuteInputKind.User) }
                    }
                }
                item(key = "import") {
                    OutlinedButton(
                        onClick = onImport,
                        enabled = !state.busy,
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                    ) {
                        Icon(Icons.Outlined.Download, null, Modifier.size(18.dp))
                        Spacer(Modifier.size(8.dp))
                        Text(stringResource(RStrings.cloud_mute_import))
                    }
                }
                item(key = "users-title") { CloudMuteSectionTitle(stringResource(RStrings.block_user)) }
                items(
                    data.mutedUsers.distinctBy { it.user.id },
                    key = { "user:${it.user.id}" }) { muted ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow
                    ) {
                        ListItem(
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            leadingContent = {
                                if (muted.user.profileImageUrls.medium.isNotBlank()) {
                                    UserAvatar(
                                        muted.user.profileImageUrls.medium,
                                        Modifier.size(40.dp)
                                    )
                                } else {
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.surfaceContainerHigh
                                    ) {
                                        Icon(
                                            Icons.Outlined.Person,
                                            null,
                                            Modifier.padding(8.dp).size(24.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            },
                            headlineContent = {
                                Text(
                                    muted.user.name.ifBlank { muted.user.id.toString() },
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            supportingContent = {
                                Text(
                                    "ID: ${muted.user.id}",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            trailingContent = {
                                IconButton(
                                    onClick = { onRemoveUser(muted.user.id) },
                                    enabled = !state.busy
                                ) {
                                    Icon(
                                        Icons.Outlined.Delete,
                                        stringResource(RStrings.cloud_mute_remove)
                                    )
                                }
                            },
                        )
                    }
                }
                if (data.mutedUsers.isEmpty()) item(key = "users-empty") { CloudMuteEmptySection() }
                item(key = "tags-title") { CloudMuteSectionTitle(stringResource(RStrings.block_tags)) }
                items(
                    data.mutedTags.distinctBy { it.tag.name },
                    key = { "tag:${it.tag.name}" }) { muted ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow
                    ) {
                        ListItem(
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            leadingContent = {
                                Icon(
                                    Icons.Outlined.Tag,
                                    null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            headlineContent = {
                                Text(
                                    muted.tag.name,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            trailingContent = {
                                IconButton(
                                    onClick = { onRemoveTag(muted.tag.name) },
                                    enabled = !state.busy
                                ) {
                                    Icon(
                                        Icons.Outlined.Delete,
                                        stringResource(RStrings.cloud_mute_remove)
                                    )
                                }
                            },
                        )
                    }
                }
                if (data.mutedTags.isEmpty()) item(key = "tags-empty") { CloudMuteEmptySection() }
            }
        }
    }
}

@Composable
private fun CloudMuteAddRow(label: String, enabled: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val color =
            if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(
                alpha = 0.38f
            )
        Icon(Icons.Rounded.Add, null, tint = color)
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge, color = color)
        Icon(
            Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            null,
            Modifier.size(20.dp),
            tint = color
        )
    }
}

@Composable
private fun CloudMuteSectionTitle(title: String) {
    Text(
        title,
        Modifier.padding(start = 4.dp, top = 16.dp, bottom = 4.dp),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary
    )
}

@Composable
private fun CloudMuteEmptySection() {
    Text(
        stringResource(RStrings.no_blocked_items),
        Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 12.dp),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}


@Composable
private fun CloudMuteInputContent(
    kind: CloudMuteInputKind,
    input: String,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = input,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(stringResource(if (kind == CloudMuteInputKind.Tag) RStrings.cloud_mute_tag else RStrings.cloud_mute_user_id)) },
        supportingText = { Text(stringResource(RStrings.cloud_mute_input_hint)) },
        keyboardOptions = KeyboardOptions(keyboardType = if (kind == CloudMuteInputKind.User) KeyboardType.Number else KeyboardType.Text),
        singleLine = true,
    )
}
