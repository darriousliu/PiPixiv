package com.mrl.pixiv.common.mmkv

import kotlinx.serialization.Serializable

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
    private val storage = AccountSessionStorage(this)

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

    private fun read() = accounts.readStrict(owner, "accountStoreState")

    private fun write(state: StoredAccounts) = accounts.write(owner, "accountStoreState", state)

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
}
