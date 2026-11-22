package com.mrl.pixiv.common.repository

import com.mrl.pixiv.common.data.auth.AuthTokenResp
import com.mrl.pixiv.common.datasource.local.AccountDatabaseProvider
import com.mrl.pixiv.common.mmkv.AccountStore
import com.mrl.pixiv.common.mmkv.closeAccountMMKV
import com.mrl.pixiv.common.mmkv.openAccountMMKV
import com.mrl.pixiv.common.mmkv.withAccountStorageLock
import com.mrl.pixiv.common.repository.viewmodel.bookmark.BookmarkState
import com.mrl.pixiv.common.repository.viewmodel.follow.FollowState
import com.mrl.pixiv.common.router.NavigationManager
import com.mrl.pixiv.common.util.currentTimeMillis
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Single

@Single
class AccountSessionManager(
    private val databases: AccountDatabaseProvider,
    private val navigationManager: NavigationManager,
    private val novelReadLater: NovelReadLaterRepository,
) {
    private val profileRefreshes = mutableMapOf<Long, Job>()

    suspend fun switchTo(userId: Long) = withContext(Dispatchers.Main.immediate) {
        databases.withAccountFilesLocked {
            if (AccountStore.activeUserId == userId) return@withAccountFilesLocked
            requireNotNull(AccountStore.find(userId)) { "Account session does not exist" }
            publishAccount(userId) {
                AccountStore.storage.activate(userId, currentTimeMillis())
            }
        }
    }

    suspend fun addOrUpdate(resp: AuthTokenResp, canActivate: () -> Boolean = { true }) {
        val session = resp.toAccountSession()
        withContext(Dispatchers.Main.immediate) {
            databases.withAccountFilesLocked {
                if (!canActivate()) throw CancellationException("The login screen was closed")
                finishLocalDeletion(session.userId)
                publishAccount(session.userId, afterLogin = true) {
                    if (!canActivate()) throw CancellationException("The login screen was closed")
                    withAccountStorageLock {
                        UserManager.updateUserInfo(session)
                        AccountStore.saveAndActivate(session.copy(lastUsedAt = currentTimeMillis()))
                    }
                }
            }
        }
    }

    suspend fun remove(userId: Long, deleteLocalData: Boolean) = withContext(Dispatchers.Main.immediate) {
        databases.withAccountFilesLocked {
            val sessions = AccountStore.sessions
            if (sessions.none { it.userId == userId }) {
                finishLocalDeletion(userId)
                return@withAccountFilesLocked
            }
            if (AccountStore.activeUserId == userId) {
                val remaining = sessions.filterNot { it.userId == userId }
                val replacement = remaining.filter { it.hasUsableCredentials() }.maxByOrNull { it.lastUsedAt }
                    ?: remaining.maxByOrNull { it.lastUsedAt }
                publishAccount(replacement?.userId) {
                    AccountStore.storage.remove(userId, replacement?.userId, currentTimeMillis(), deleteLocalData)
                }
            } else {
                AccountStore.storage.remove(userId, null, currentTimeMillis(), deleteLocalData)
            }

            withContext(NonCancellable) {
                profileRefreshes.remove(userId)?.cancelAndJoin()
                finishLocalDeletion(userId)
            }
        }
    }

    private suspend fun finishLocalDeletion(userId: Long) {
        if (userId !in AccountStore.storage.pendingLocalDeletions) return
        novelReadLater.cancelAccountWork(userId)
        databases.closeAndDelete(userId) { closeAccountMMKV(userId) }
        AccountStore.storage.completeLocalDeletion(userId)
    }

    private suspend fun publishAccount(
        userId: Long?,
        afterLogin: Boolean = false,
        updateSessions: () -> Unit,
    ) {
        val context = currentCoroutineContext()
        databases.publish(userId, beforePublish = {
            context.ensureActive()
            // Check that the incoming account's MMKV can be opened before committing the session.
            userId?.let(::openAccountMMKV)
            updateSessions()
        })
        reloadAccountPreferences()
        BookmarkState.clear()
        FollowState.clear()
        IllustCacheRepo.clear()

        val session = AccountStore.current
        if (session == null || !session.hasUsableCredentials()) {
            navigationManager.navigateToLoginOptionScreen()
        } else {
            navigationManager.loginToMainScreen(
                showDataRecovery = afterLogin && AccountStore.storage.consumeRecoverAfterLogin(),
            )
            profileRefreshes.remove(session.userId)?.cancel()
            profileRefreshes[session.userId] = UserManager.updateUserInfo(userId = session.userId)
        }
    }
}
