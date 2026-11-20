package com.mrl.pixiv.setting.account

import com.mrl.pixiv.common.mmkv.AccountSession
import com.mrl.pixiv.common.mmkv.AccountStore
import com.mrl.pixiv.common.mmkv.withAccountStorageLock
import com.mrl.pixiv.common.repository.AccountSessionManager
import com.mrl.pixiv.common.viewmodel.BaseMviViewModel
import com.mrl.pixiv.common.viewmodel.ViewIntent
import org.koin.android.annotation.KoinViewModel

data class ManagedAccount(
    val userId: Long,
    val name: String,
    val account: String,
    val avatarUrl: String,
    val isCurrent: Boolean = false,
    val needsRelogin: Boolean = false,
) {
    val displayName: String
        get() = name.ifBlank { account.ifBlank { userId.toString() } }
}

data class AccountManagementState(
    val accounts: List<ManagedAccount>,
    val busy: Boolean = false,
)

sealed interface AccountManagementAction : ViewIntent {
    data class Switch(val userId: Long) : AccountManagementAction
    data class Remove(val userId: Long, val deleteLocalData: Boolean) : AccountManagementAction
}

@KoinViewModel
class AccountManagementViewModel(
    private val sessionManager: AccountSessionManager,
) : BaseMviViewModel<AccountManagementState, AccountManagementAction>(
    initialState = AccountManagementState(accounts = readAccounts()),
) {
    init {
        launchUI {
            AccountStore.accountChanges.collect {
                updateState { copy(accounts = readAccounts()) }
            }
        }
    }

    override suspend fun handleIntent(intent: AccountManagementAction) {
        if (uiState.value.busy) return
        updateState { copy(busy = true) }
        try {
            when (intent) {
                is AccountManagementAction.Switch -> sessionManager.switchTo(intent.userId)
                is AccountManagementAction.Remove ->
                    sessionManager.remove(intent.userId, intent.deleteLocalData)
            }
        } finally {
            updateState { copy(busy = false) }
        }
    }
}

private fun readAccounts(): List<ManagedAccount> = withAccountStorageLock {
    val activeUserId = AccountStore.activeUserId
    AccountStore.sessions
        .sortedWith(
            compareByDescending<AccountSession> { it.userId == activeUserId }
                .thenByDescending { it.lastUsedAt },
        )
        .map { session ->
            ManagedAccount(
                userId = session.userId,
                name = session.name,
                account = session.account,
                avatarUrl = session.avatarUrl,
                isCurrent = session.userId == activeUserId,
                needsRelogin = !session.hasUsableCredentials(),
            )
        }
}
