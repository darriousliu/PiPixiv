package com.mrl.pixiv.common.mmkv

import co.touchlab.kermit.Logger
import com.mrl.pixiv.common.util.currentTimeMillis
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlin.text.CharacterCodingException

@Serializable
data class AccountSession(
    val userId: Long,
    val name: String,
    val account: String,
    val avatarUrl: String = "",
    val isPremium: Boolean = false,
    val refreshToken: String,
    val accessToken: String,
    val accessTokenExpiresAt: Long,
    val needsRelogin: Boolean = false,
    val lastUsedAt: Long = 0L,
) {
    fun hasUsableCredentials() = !needsRelogin && refreshToken.isNotEmpty() && accessToken.isNotEmpty()

    fun hasExpired() = accessTokenExpiresAt <= currentTimeMillis()

    override fun toString() = "AccountSession"
}

@Serializable
private data class StoredAccounts(
    val sessions: List<AccountSession> = emptyList(),
    val activeUserId: Long = 0L,
    val removedAccounts: Map<Long, RemovedAccountData> = emptyMap(),
)

@Serializable
private enum class RemovedAccountData { Retained, PendingDeletion, Deleted }

object AccountStore : MMKVApp {
    val storage = AccountSessionStorage(this)

    val sessions get() = storage.sessions
    val activeUserId get() = storage.activeUserId
    val current get() = storage.current
    val accountChanges get() = storage.accountChanges

    fun find(userId: Long) = storage.find(userId)

    fun saveAndActivate(session: AccountSession) = storage.saveAndActivate(session)

    fun removeCurrent() = storage.removeCurrent()

    fun update(userId: Long, transform: (AccountSession) -> AccountSession): AccountSession? =
        storage.update(userId, transform)
}

class AccountSessionStorage(private val owner: MMKVOwner) {
    private val accounts = owner.mmkvSerializable(StoredAccounts())
    // Observers re-read the store without retaining a second in-memory copy of the credentials.
    private val mutableAccountChanges = MutableStateFlow(0L)
    val accountChanges = mutableAccountChanges.asStateFlow()
    private val recoveryOnLogin = owner.mmkvBool()
    private val mutableRecoverAfterLogin = MutableStateFlow(false)
    val recoverAfterLogin = mutableRecoverAfterLogin.asStateFlow()

    fun reloadRecoverAfterLogin() = withAccountStorageLock {
        mutableRecoverAfterLogin.value = recoveryOnLogin.read(owner, RECOVER_AFTER_LOGIN_KEY)
    }

    fun setRecoverAfterLogin(value: Boolean) = withAccountStorageLock {
        if (recoveryOnLogin.read(owner, RECOVER_AFTER_LOGIN_KEY) != value) {
            recoveryOnLogin.write(owner, RECOVER_AFTER_LOGIN_KEY, value)
            owner.sync()
        }
        mutableRecoverAfterLogin.value = value
    }

    /** Clears the persisted recovery request and returns whether it was set. */
    fun consumeRecoverAfterLogin(): Boolean = withAccountStorageLock {
        val pending = recoveryOnLogin.read(owner, RECOVER_AFTER_LOGIN_KEY)
        if (pending) setRecoverAfterLogin(false)
        pending
    }

    private fun read(): StoredAccounts = readOrNull() ?: StoredAccounts()

    fun hasCorruptState() = withAccountStorageLock { readOrNull() == null }

    private fun readOrNull(): StoredAccounts? {
        val raw = owner.accessMMKV { getByteArray(ACCOUNT_STORE_KEY) }
        if (raw == null) {
            if (!owner.contains(ACCOUNT_STORE_KEY)) return StoredAccounts()
            setRecoverAfterLogin(true)
            return null
        }
        try {
            return accounts.decode(raw)
        } catch (_: CharacterCodingException) {
            preserveCorruptState(raw)
        } catch (_: SerializationException) {
            preserveCorruptState(raw)
        }
        return null
    }

    private fun preserveCorruptState(raw: ByteArray) {
        if (!owner.contains(CORRUPT_ACCOUNT_STORE_KEY)) {
            owner.mmkvBytes().write(owner, CORRUPT_ACCOUNT_STORE_KEY, raw)
            owner.sync()
            Logger.w(tag = "AccountStore") { "Preserved unreadable account session data" }
        }
        setRecoverAfterLogin(true)
    }

    private fun write(state: StoredAccounts) {
        accounts.write(owner, ACCOUNT_STORE_KEY, state)
        mutableAccountChanges.value += 1
    }

    val sessions get() = withAccountStorageLock { read().sessions }

    val activeUserId get() = withAccountStorageLock { read().activeUserId }

    val removedUserIds get() = withAccountStorageLock { read().removedAccounts.keys }

    val discardedLocalUserIds get() = withAccountStorageLock {
        read().removedAccounts.filterValues { it != RemovedAccountData.Retained }.keys
    }

    val pendingLocalDeletions get() = withAccountStorageLock {
        read().removedAccounts.filterValues { it == RemovedAccountData.PendingDeletion }.keys
    }

    fun completeLocalDeletion(userId: Long) = withAccountStorageLock {
        val state = read()
        if (state.removedAccounts[userId] == RemovedAccountData.PendingDeletion) {
            write(state.copy(removedAccounts = state.removedAccounts + (userId to RemovedAccountData.Deleted)))
        }
    }

    val current get() = withAccountStorageLock {
        val state = read()
        state.sessions.firstOrNull { it.userId == state.activeUserId }
    }

    fun find(userId: Long) = sessions.firstOrNull { it.userId == userId }

    fun saveAndActivate(session: AccountSession) = withAccountStorageLock {
        require(session.userId > 0)
        val state = read()
        check(state.removedAccounts[session.userId] != RemovedAccountData.PendingDeletion)
        val others = state.sessions.filterNot { it.userId == session.userId }
        write(state.copy(sessions = others + session, activeUserId = session.userId))
    }

    fun activate(userId: Long, lastUsedAt: Long) = withAccountStorageLock {
        val state = read()
        check(state.sessions.any { it.userId == userId }) { "Account session no longer exists" }
        write(state.copy(sessions = state.sessions.withLastUsedAt(userId, lastUsedAt), activeUserId = userId))
    }

    fun remove(
        userId: Long,
        replacementUserId: Long?,
        lastUsedAt: Long,
        discardLocalData: Boolean,
    ) = withAccountStorageLock {
        val state = read()
        val remaining = state.sessions.filterNot { it.userId == userId }
        if (remaining.size == state.sessions.size) return@withAccountStorageLock

        // Keep retired sessions out of legacy recovery, with independent progress for filesystem deletion.
        val localData = if (discardLocalData) RemovedAccountData.PendingDeletion
        else state.removedAccounts[userId] ?: RemovedAccountData.Retained
        val updated = state.copy(sessions = remaining, removedAccounts = state.removedAccounts + (userId to localData))
        if (state.activeUserId == userId) {
            val nextUserId = replacementUserId ?: 0L
            check(nextUserId == 0L || remaining.any { it.userId == nextUserId }) {
                "Replacement account session no longer exists"
            }
            write(updated.copy(
                sessions = remaining.withLastUsedAt(nextUserId, lastUsedAt),
                activeUserId = nextUserId,
            ))
        } else {
            write(updated)
        }
    }

    private fun List<AccountSession>.withLastUsedAt(userId: Long, lastUsedAt: Long) =
        map { if (it.userId == userId) it.copy(lastUsedAt = lastUsedAt) else it }

    fun addMigratedAccount(session: AccountSession) = withAccountStorageLock {
        require(session.userId > 0)
        val state = read()
        if (state.sessions.none { it.userId == session.userId }) {
            val activeUserId = state.activeUserId.takeIf { it > 0 } ?: session.userId
            write(state.copy(sessions = state.sessions + session, activeUserId = activeUserId))
        }
    }

    fun removeCurrent() = withAccountStorageLock {
        val state = read()
        if (state.activeUserId > 0) {
            write(state.copy(sessions = state.sessions.filterNot { it.userId == state.activeUserId }, activeUserId = 0L))
        }
    }

    fun update(userId: Long, transform: (AccountSession) -> AccountSession): AccountSession? =
        withAccountStorageLock {
            val state = read()
            val current = state.sessions.firstOrNull { it.userId == userId } ?: return@withAccountStorageLock null
            val updated = transform(current)
            check(updated.userId == userId)
            if (updated != current) {
                write(state.copy(sessions = state.sessions.map { if (it.userId == userId) updated else it }))
            }
            updated
        }

    fun saveRefreshedTokens(previous: AccountSession, refreshed: AccountSession): AccountSession? {
        check(refreshed.userId == previous.userId) { "The refreshed token belongs to a different account" }
        return update(previous.userId) { current ->
            if (current.hasSameCredentials(previous) && !current.needsRelogin) {
                current.copy(
                    refreshToken = refreshed.refreshToken,
                    accessToken = refreshed.accessToken,
                    accessTokenExpiresAt = refreshed.accessTokenExpiresAt,
                )
            } else {
                current
            }
        }
    }

    fun markNeedsRelogin(previous: AccountSession): AccountSession? = update(previous.userId) { current ->
        if (current.hasSameCredentials(previous)) current.copy(needsRelogin = true) else current
    }

    private fun AccountSession.hasSameCredentials(other: AccountSession) =
        refreshToken == other.refreshToken && accessToken == other.accessToken &&
            accessTokenExpiresAt == other.accessTokenExpiresAt
}

private const val ACCOUNT_STORE_KEY = "accountStoreState"
private const val CORRUPT_ACCOUNT_STORE_KEY = "$ACCOUNT_STORE_KEY.corrupt"
private const val RECOVER_AFTER_LOGIN_KEY = "recoverAfterLogin"
