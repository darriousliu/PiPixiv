package com.mrl.pixiv.setting.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.mrl.pixiv.common.compose.rememberThrottleClick
import com.mrl.pixiv.common.compose.ui.ContentEmptyState
import com.mrl.pixiv.common.compose.ui.image.LoadingImage
import com.mrl.pixiv.common.router.NavigationManager
import com.mrl.pixiv.common.router.currentNavigationManager
import com.mrl.pixiv.common.util.RStrings
import com.mrl.pixiv.common.util.ToastUtil
import com.mrl.pixiv.common.viewmodel.SideEffect
import com.mrl.pixiv.common.viewmodel.asState
import com.mrl.pixiv.strings.account_current
import com.mrl.pixiv.strings.account_management
import com.mrl.pixiv.strings.account_needs_relogin
import com.mrl.pixiv.strings.add_account
import com.mrl.pixiv.strings.back
import com.mrl.pixiv.strings.more
import com.mrl.pixiv.strings.remove_account
import com.mrl.pixiv.strings.remove_account_confirmation
import com.mrl.pixiv.strings.unknown_error
import kotlinx.coroutines.CancellationException
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AccountManagementScreen(
    modifier: Modifier = Modifier,
    navigationManager: NavigationManager = currentNavigationManager(),
    viewModel: AccountManagementViewModel = koinViewModel(),
) {
    val topEntryId = navigationManager.topEntryId
    val accountEntryId = navigationManager.entryId ?: topEntryId
    LaunchedEffect(viewModel) {
        viewModel.sideEffect.collect { effect ->
            if (effect is SideEffect.Error && effect.throwable !is CancellationException) {
                ToastUtil.safeShortToast(RStrings.unknown_error)
            }
        }
    }
    AccountManagementContent(
        state = viewModel.asState(),
        onSwitch = { userId ->
            if (accountEntryId != null && navigationManager.topEntryId == accountEntryId) {
                viewModel.dispatch(AccountManagementAction.Switch(userId))
            }
        },
        onRemove = { userId, deleteLocalData ->
            if (accountEntryId != null && navigationManager.topEntryId == accountEntryId) {
                viewModel.dispatch(AccountManagementAction.Remove(userId, deleteLocalData))
            }
        },
        onAdd = rememberThrottleClick {
            if (!viewModel.uiState.value.busy) navigationManager.navigateToAddAccountScreen()
        },
        onBack = navigationManager::popBackStack,
        topEntryId = topEntryId,
        modifier = modifier,
    )
}

@Composable
internal fun AccountManagementContent(
    state: AccountManagementState,
    onSwitch: (Long) -> Unit,
    onRemove: (Long, Boolean) -> Unit,
    onAdd: () -> Unit,
    onBack: () -> Unit,
    topEntryId: String?,
    modifier: Modifier = Modifier,
) {
    var removeUserId by key(topEntryId) { rememberSaveable { mutableStateOf<Long?>(null) } }
    val toRemove = state.accounts.firstOrNull { it.userId == removeUserId }
    if (toRemove != null) {
        RemoveAccountDialog(
            title = stringResource(RStrings.remove_account_confirmation, toRemove.displayName),
            confirmLabel = stringResource(RStrings.remove_account),
            onDismissRequest = { removeUserId = null },
            onConfirm = { deleteLocalData ->
                if (removeUserId == toRemove.userId && !state.busy) {
                    removeUserId = null
                    onRemove(toRemove.userId, deleteLocalData)
                }
            },
        )
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            Box {
                TopAppBar(
                    title = { Text(stringResource(RStrings.account_management)) },
                    navigationIcon = {
                        IconButton(onClick = onBack, shapes = IconButtonDefaults.shapes()) {
                            Icon(
                                Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = stringResource(RStrings.back),
                            )
                        }
                    },
                )
                if (state.busy) {
                    LinearWavyProgressIndicator(Modifier.fillMaxWidth().align(Alignment.BottomCenter))
                }
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(innerPadding).consumeWindowInsets(innerPadding),
            contentAlignment = Alignment.TopCenter,
        ) {
            LazyColumn(
                modifier = Modifier.widthIn(max = 640.dp).fillMaxWidth(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                if (state.accounts.isEmpty()) {
                    item("empty_accounts", contentType = "empty") {
                        ContentEmptyState(
                            icon = Icons.Rounded.Person,
                            action = { AddAccountRow(enabled = !state.busy, onAdd = onAdd) },
                        )
                    }
                } else {
                    items(state.accounts, key = { it.userId }, contentType = { "account" }) { account ->
                        AccountRow(
                            account = account,
                            enabled = !state.busy,
                            onSelect = {
                                if (account.needsRelogin) onAdd()
                                else if (!account.isCurrent) onSwitch(account.userId)
                            },
                            onRemove = { removeUserId = account.userId },
                        )
                    }
                    item("add_account", contentType = "action") {
                        AddAccountRow(enabled = !state.busy, onAdd = onAdd, modifier = Modifier.padding(top = 8.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun AccountRow(
    account: ManagedAccount,
    enabled: Boolean,
    onSelect: () -> Unit,
    onRemove: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val colors = MaterialTheme.colorScheme
    ListItem(
        selected = account.isCurrent,
        onClick = onSelect,
        enabled = enabled,
        shapes = ListItemDefaults.shapes(shape = MaterialTheme.shapes.large),
        colors = ListItemDefaults.colors(
            containerColor = colors.surfaceContainerLow,
            selectedContainerColor = colors.secondaryContainer,
            selectedContentColor = colors.onSecondaryContainer,
            selectedSupportingContentColor = colors.onSecondaryContainer,
            selectedTrailingContentColor = colors.onSecondaryContainer,
        ),
        leadingContent = { AccountAvatar(account.avatarUrl, isCurrent = account.isCurrent) },
        supportingContent = {
            Column {
                if (account.account.isNotBlank()) {
                    Text("@${account.account}")
                }
                if (account.needsRelogin) {
                    Text(
                        stringResource(RStrings.account_needs_relogin),
                        color = if (enabled) colors.error else LocalContentColor.current,
                    )
                }
            }
        },
        trailingContent = {
            Box {
                IconButton(enabled = enabled, onClick = { expanded = true }) {
                    Icon(Icons.Rounded.MoreHoriz, contentDescription = stringResource(RStrings.more))
                }
                DropdownMenu(expanded = expanded && enabled, onDismissRequest = { expanded = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(RStrings.remove_account)) },
                        onClick = {
                            expanded = false
                            onRemove()
                        },
                    )
                }
            }
        },
    ) {
        Text(account.displayName)
    }
}

@Composable
private fun AccountAvatar(url: String, isCurrent: Boolean) {
    val colors = MaterialTheme.colorScheme
    Box(Modifier.size(40.dp)) {
        Surface(
            modifier = Modifier.matchParentSize(),
            shape = CircleShape,
            color = colors.surfaceContainerHighest,
        ) {
            if (url.isBlank()) {
                AccountAvatarPlaceholder()
            } else {
                LoadingImage(
                    model = url,
                    contentScale = ContentScale.Crop,
                    loadingContent = { AccountAvatarPlaceholder() },
                    errorContent = { AccountAvatarPlaceholder() },
                )
            }
        }
        if (isCurrent) {
            Surface(
                modifier = Modifier.align(Alignment.BottomEnd).size(18.dp),
                shape = CircleShape,
                color = colors.primary,
                contentColor = colors.onPrimary,
            ) {
                Icon(
                    Icons.Rounded.Check,
                    contentDescription = stringResource(RStrings.account_current),
                    modifier = Modifier.padding(2.dp),
                )
            }
        }
    }
}

@Composable
private fun AccountAvatarPlaceholder() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Icon(Icons.Rounded.Person, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun AddAccountRow(
    enabled: Boolean,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ListItem(
        onClick = onAdd,
        modifier = modifier,
        enabled = enabled,
        leadingContent = { Icon(Icons.Rounded.PersonAdd, contentDescription = null) },
        shapes = ListItemDefaults.shapes(shape = MaterialTheme.shapes.large),
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Text(stringResource(RStrings.add_account))
    }
}
