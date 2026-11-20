package com.mrl.pixiv.common.mmkv

import co.touchlab.kermit.Logger
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
    override fun toString() = "AccountSession"
}

@Serializable
private data class StoredAccounts(
    val sessions: List<AccountSession> = emptyList(),
    val activeUserId: Long = 0L,
)

object AccountStore : MMKVApp {
    val storage = AccountSessionStorage(this)

    val sessions get() = storage.sessions
    val activeUserId get() = storage.activeUserId
    val current get() = storage.current

    fun find(userId: Long) = storage.find(userId)

    fun saveAndActivate(session: AccountSession) = storage.saveAndActivate(session)

    fun removeCurrent() = storage.removeCurrent()

    fun update(userId: Long, transform: (AccountSession) -> AccountSession): AccountSession? =
        storage.update(userId, transform)
}

class AccountSessionStorage(private val owner: MMKVOwner) {
    private val accounts = owner.mmkvSerializable(StoredAccounts())
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

    private fun write(state: StoredAccounts) = accounts.write(owner, ACCOUNT_STORE_KEY, state)

    val sessions get() = withAccountStorageLock { read().sessions }

    val activeUserId get() = withAccountStorageLock { read().activeUserId }

    val current get() = withAccountStorageLock {
        val state = read()
        state.sessions.firstOrNull { it.userId == state.activeUserId }
    }

    fun find(userId: Long) = sessions.firstOrNull { it.userId == userId }

    fun saveAndActivate(session: AccountSession) = withAccountStorageLock {
        require(session.userId > 0)
        val others = read().sessions.filterNot { it.userId == session.userId }
        write(StoredAccounts(others + session, session.userId))
    }

    fun addMigratedAccount(session: AccountSession) = withAccountStorageLock {
        require(session.userId > 0)
        val state = read()
        if (state.sessions.none { it.userId == session.userId }) {
            val activeUserId = state.activeUserId.takeIf { it > 0 } ?: session.userId
            write(StoredAccounts(state.sessions + session, activeUserId))
        }
    }

    fun removeCurrent() = withAccountStorageLock {
        val state = read()
        if (state.activeUserId > 0) {
            write(StoredAccounts(state.sessions.filterNot { it.userId == state.activeUserId }))
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
            if (current.refreshToken != previous.refreshToken || current.accessToken != previous.accessToken) {
                current
            } else {
                current.copy(
                    refreshToken = refreshed.refreshToken,
                    accessToken = refreshed.accessToken,
                    accessTokenExpiresAt = refreshed.accessTokenExpiresAt,
                )
            }
        }
    }
}

private const val ACCOUNT_STORE_KEY = "accountStoreState"
private const val CORRUPT_ACCOUNT_STORE_KEY = "$ACCOUNT_STORE_KEY.corrupt"
private const val RECOVER_AFTER_LOGIN_KEY = "recoverAfterLogin"
