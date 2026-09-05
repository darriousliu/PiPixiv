package com.mrl.pixiv.search

import com.mrl.pixiv.common.router.DestinationsDeepLink
import com.mrl.pixiv.common.router.PixivLinkTarget
import kotlin.test.Test
import kotlin.test.assertEquals

class DestinationsDeepLinkTest {
    @Test
    fun findsPixivLinksInsideSharedTextInSourceOrder() {
        val text = """
            Artwork https://www.pixiv.net/en/artworks/12345,
            novel https://www.pixiv.net/novel/show.php?id=67890
            user https://pixiv.net/users/24680.
        """.trimIndent()

        assertEquals(
            listOf(
                PixivLinkTarget.Illust(12345),
                PixivLinkTarget.Novel(67890),
                PixivLinkTarget.User(24680),
            ),
            DestinationsDeepLink.findLinks(text),
        )
    }

    @Test
    fun ignoresInvalidHostsAndIds() {
        val text = """
            https://www.pixiv.net.evil.example/artworks/123
            https://www.pixiv.net/artworks/0
            https://example.com/users/456
        """.trimIndent()

        assertEquals(emptyList(), DestinationsDeepLink.findLinks(text))
    }

    @Test
    fun removesDuplicateTargets() {
        val text = """
            https://www.pixiv.net/artworks/123
            https://pixiv.me/artworks/123
        """.trimIndent()

        assertEquals(
            listOf(PixivLinkTarget.Illust(123)),
            DestinationsDeepLink.findLinks(text),
        )
    }
}
