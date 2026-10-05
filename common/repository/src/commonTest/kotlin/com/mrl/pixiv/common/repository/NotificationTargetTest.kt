package com.mrl.pixiv.common.repository

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class NotificationTargetTest {
    @Test
    fun supportedNativeAndWebLinksOpenExistingDestinations() {
        assertEquals(NotificationTarget.User(42), resolveNotificationTarget("pixiv://users/42"))
        assertEquals(NotificationTarget.Illust(7), resolveNotificationTarget("pixiv://illusts/7"))
        assertEquals(NotificationTarget.Novel(9), resolveNotificationTarget("pixiv://novels/9"))
        assertEquals(NotificationTarget.Illust(7), resolveNotificationTarget("https://www.pixiv.net/en/artworks/7"))
        assertEquals(NotificationTarget.User(42), resolveNotificationTarget("https://www.pixiv.net/users/42"))
        assertEquals(NotificationTarget.Novel(9), resolveNotificationTarget("https://www.pixiv.net/novel/show.php?id=9"))
        assertEquals(NotificationTarget.Illust(7), resolveNotificationTarget("https://www.pixiv.net/member_illust.php?illust_id=7"))
    }

    @Test
    fun unknownOfficialWebLinksHaveBrowserFallback() {
        assertIs<NotificationTarget.Web>(resolveNotificationTarget("https://www.pixiv.net/info.php?id=100"))
    }

    @Test
    fun unsafeOrUnrelatedTargetsAreNotExecuted() {
        listOf(
            "javascript:alert(1)",
            "file:///private/data",
            "https://pixiv.net.evil.example/artworks/7",
            "https://evil.example/?target=https://www.pixiv.net/artworks/7",
            "https://other@www.pixiv.net/artworks/7",
            "pixiv://unknown/42",
            "pixiv://users/-42",
            "pixiv://users/999999999999999999999999999",
        ).forEach { assertNull(resolveNotificationTarget(it), it) }
    }
}
