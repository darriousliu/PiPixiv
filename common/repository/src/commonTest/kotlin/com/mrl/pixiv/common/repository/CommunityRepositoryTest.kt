package com.mrl.pixiv.common.repository

import com.mrl.pixiv.common.data.notification.UnreadNotificationsResponse
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CommunityRepositoryTest {
    @Test
    fun accountSwitchDiscardsOutstandingUnreadResponse() = runTest {
        var accountId = 1L
        val result = CompletableDeferred<UnreadNotificationsResponse>()
        val store = NotificationUnreadStore({ accountId }) { result.await() }
        val job = launch { store.refresh() }
        runCurrent()
        accountId = 2L
        result.complete(UnreadNotificationsResponse(hasUnreadNotifications = true))
        job.join()
        assertNull(store.state.value.hasUnread)
        assertEquals(0L, store.state.value.accountId)
    }

    @Test
    fun cancelledUnreadRefreshPropagatesAndClearsLoading() = runTest {
        val store = NotificationUnreadStore({ 1L }) { throw CancellationException("Left screen") }
        assertFailsWith<CancellationException> { store.refresh() }
        assertFalse(store.state.value.isLoading)
        assertFalse(store.state.value.failed)
    }

    @Test
    fun failedUnreadRefreshDoesNotPretendTheInboxIsRead() = runTest {
        var fail = false
        val store = NotificationUnreadStore({ 1L }) {
            if (fail) error("Offline")
            UnreadNotificationsResponse(hasUnreadNotifications = true)
        }
        store.refresh()
        fail = true
        store.refresh()
        assertEquals(true, store.state.value.hasUnread)
        assertTrue(store.state.value.failed)
        assertFalse(store.state.value.isLoading)
    }

    @Test
    fun signedOutDoesNotFetchUnread() = runTest {
        var requested = false
        val store = NotificationUnreadStore({ 0L }) {
            requested = true
            UnreadNotificationsResponse()
        }
        store.refresh()
        assertFalse(requested)
        assertNull(store.state.value.hasUnread)
    }

    @Test
    fun paginationRejectsForeignHostsWrongPathsAndUserInfo() {
        listOf(
            "https://evil.example/v1/notification/list?offset=30",
            "https://app-api.pixiv.net/v1/user/follower?offset=30",
            "https://app-api.pixiv.net.evil.example/v1/notification/list?offset=30",
            "https://someone@app-api.pixiv.net/v1/notification/list?offset=30",
            "http://app-api.pixiv.net/v1/notification/list?offset=30",
        ).forEach { url ->
            assertFailsWith<IllegalArgumentException> { communityCursorQuery(url, "v1/notification/list") }
        }
        assertEquals(
            mapOf("offset" to "30", "cursor" to "a+b"),
            communityCursorQuery(
                "https://app-api.pixiv.net/v1/notification/list?offset=30&cursor=a%2Bb",
                "v1/notification/list",
            ),
        )
    }
}
