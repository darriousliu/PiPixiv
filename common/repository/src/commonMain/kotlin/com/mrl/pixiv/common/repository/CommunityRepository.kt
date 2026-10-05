package com.mrl.pixiv.common.repository

import com.mrl.pixiv.common.data.discovery.CommunityUsersKind
import com.mrl.pixiv.common.data.discovery.CommunityUsersResponse
import com.mrl.pixiv.common.data.notification.NotificationsResponse
import com.mrl.pixiv.common.data.notification.UnreadNotificationsResponse
import com.mrl.pixiv.common.datasource.remote.createCommunityApi
import io.ktor.http.Url
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

object CommunityRepository {
    private val api by lazy { PixivRepository.apiKtorfit.createCommunityApi() }

    private val unreadStore = NotificationUnreadStore(
        currentAccountId = { requireUserInfoValue.user.id },
        fetch = { api.getUnreadNotifications() },
    )
    val unreadState = unreadStore.state

    suspend fun refreshUnread() = unreadStore.refresh()

    suspend fun getNotifications(notificationId: Long? = null): NotificationsResponse =
        if (notificationId == null) api.getNotifications() else api.getNotificationGroup(notificationId)

    suspend fun getNotificationsNext(
        notificationId: Long?,
        query: Map<String, String>,
    ): NotificationsResponse = if (notificationId == null) {
        api.getNotificationsNext(query)
    } else {
        api.getNotificationGroupNext(query + ("notification_id" to notificationId.toString()))
    }

    suspend fun getUsers(kind: CommunityUsersKind, seedUserId: Long?): CommunityUsersResponse =
        when (kind) {
            CommunityUsersKind.RECOMMENDED -> api.getRecommendedUsers()
            CommunityUsersKind.RELATED -> api.getRelatedUsers(requireNotNull(seedUserId).also {
                require(it > 0) { "A valid author is required" }
            })
            CommunityUsersKind.FOLLOWERS -> api.getMyFollowers()
        }

    suspend fun getUsersNext(kind: CommunityUsersKind, query: Map<String, String>): CommunityUsersResponse =
        when (kind) {
            CommunityUsersKind.RECOMMENDED -> api.getRecommendedUsersNext(query)
            CommunityUsersKind.FOLLOWERS -> api.getMyFollowersNext(query)
            // The related-user response has no next_url in the official contract.
            CommunityUsersKind.RELATED -> error("Related users do not support pagination")
        }
}

data class NotificationUnreadState(
    val accountId: Long = 0,
    val hasUnread: Boolean? = null,
    val isLoading: Boolean = false,
    val failed: Boolean = false,
)

/** Serializes foreground refreshes and never publishes another account's result. */
internal class NotificationUnreadStore(
    private val currentAccountId: () -> Long,
    private val fetch: suspend () -> UnreadNotificationsResponse,
) {
    private val mutex = Mutex()
    private val mutableState = MutableStateFlow(NotificationUnreadState())
    val state = mutableState.asStateFlow()

    suspend fun refresh() = mutex.withLock {
        val accountId = currentAccountId()
        if (accountId <= 0) {
            mutableState.value = NotificationUnreadState()
            return@withLock
        }
        val previous = state.value.takeIf { it.accountId == accountId }
            ?: NotificationUnreadState(accountId = accountId)
        mutableState.value = previous.copy(isLoading = true, failed = false)
        try {
            val response = fetch()
            if (currentAccountId() == accountId) {
                mutableState.value = NotificationUnreadState(accountId, response.hasUnreadNotifications)
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            if (currentAccountId() == accountId) {
                mutableState.value = previous.copy(failed = true)
            }
        } finally {
            if (currentAccountId() != accountId) {
                mutableState.value = NotificationUnreadState()
            } else if (state.value.isLoading) {
                mutableState.value = state.value.copy(isLoading = false)
            }
        }
    }
}

/** Use only the cursor query with a fixed API route, never an authenticated arbitrary URL. */
internal fun communityCursorQuery(nextUrl: String, expectedPath: String): Map<String, String> {
    val url = Url(nextUrl)
    require(url.protocol.name == "https" && url.host == "app-api.pixiv.net" &&
        url.port == 443 && url.user.isNullOrEmpty() && url.password.isNullOrEmpty() &&
        url.encodedPath.trim('/') == expectedPath.trim('/')
    ) { "Unexpected pagination URL" }
    return url.parameters.entries().associate { (key, values) -> key to values.first() }
}
