package com.mrl.pixiv.common.data.notification

import com.mrl.pixiv.common.data.discovery.CommunityUsersResponse
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class NotificationModelsTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun unknownNotificationTypeKeepsContentUnreadStateAndGroup() {
        val response = json.decodeFromString<NotificationsResponse>("""{
            "notifications": [{
                "id": 9007199254740993,
                "type": 9876,
                "created_datetime": "2026-10-05T12:30:00+09:00",
                "is_read": false,
                "target_url": "pixiv://users/42",
                "content": {"text": "An artist followed you", "left_image": null, "future": true},
                "view_more": {"title": "All followers", "unread_exists": true}
            }],
            "next_url": "https://app-api.pixiv.net/v1/notification/list?offset=30"
        }""")
        val notification = response.notifications.single()
        assertEquals(9007199254740993L, notification.id)
        assertEquals(9876, notification.type)
        assertFalse(notification.isRead)
        assertNull(notification.content.leftImage)
        assertEquals("An artist followed you", notification.content.text)
        assertTrue(notification.viewMore!!.unreadExists)
        assertTrue(response.nextUrl!!.endsWith("offset=30"))
    }

    @Test
    fun relatedAuthorsCanOmitPaginationAndOptionalPreviewCollections() {
        val response = json.decodeFromString<CommunityUsersResponse>("""{
            "user_previews": [{"user": {"id": 42, "name": "Artist"}, "novels": []}]
        }""")
        assertEquals(42L, response.userPreviews.single().user.id)
        assertTrue(response.userPreviews.single().illusts.isEmpty())
        assertNull(response.nextUrl)
    }

    @Test
    fun unreadSummaryUsesServerFlag() {
        val summary = json.decodeFromString<UnreadNotificationsResponse>("""{
            "has_unread_notifications": true,
            "notify_user_ids": [42, 99]
        }""")
        assertTrue(summary.hasUnreadNotifications)
        assertEquals(listOf(42L, 99L), summary.notifyUserIds)
    }
}
