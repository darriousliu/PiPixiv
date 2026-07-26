package com.mrl.pixiv.novel

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.font.FontWeight
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class NovelCaptionTest {
    @Test
    fun convertsSupportedHtmlAndEntities() {
        val caption = novelCaptionToAnnotatedString(
            html = "<strong>粗体 <a href=\"pixiv://novels/42\">链接</a></strong><br>Tom &amp; Jerry",
            linkColor = Color.Blue,
            onLinkClick = {},
        )

        assertEquals("粗体 链接\nTom & Jerry", caption.text)
        assertTrue(caption.spanStyles.any { range ->
            range.item.fontWeight == FontWeight.Bold && range.start == 0 && range.end == 5
        })
        val link = caption.getLinkAnnotations(0, caption.length).single()
        assertEquals(3, link.start)
        assertEquals(5, link.end)
        assertEquals(
            "pixiv://novels/42",
            assertIs<LinkAnnotation.Url>(link.item).url,
        )
    }

    @Test
    fun toleratesMalformedHtml() {
        val caption = novelCaptionToAnnotatedString(
            html = "<strong>未闭合<a href=\"pixiv://users/9\">链接",
            linkColor = Color.Blue,
            onLinkClick = {},
        )

        assertEquals("未闭合链接", caption.text)
    }

    @Test
    fun resolvesSupportedPixivLinks() {
        assertEquals(
            NovelCaptionLinkTarget.Illust(123),
            resolveNovelCaptionLink("pixiv://illusts/123"),
        )
        assertEquals(
            NovelCaptionLinkTarget.Novel(456),
            resolveNovelCaptionLink("PIXIV://NOVELS/456/"),
        )
        assertEquals(
            NovelCaptionLinkTarget.User(789),
            resolveNovelCaptionLink("pixiv://users/789"),
        )
    }

    @Test
    fun resolvesHttpLinksForExternalHandling() {
        val http = assertIs<NovelCaptionLinkTarget.External>(
            resolveNovelCaptionLink("http://example.com/path?q=1"),
        )
        val https = assertIs<NovelCaptionLinkTarget.External>(
            resolveNovelCaptionLink("https://example.com"),
        )

        assertEquals("http://example.com/path?q=1", http.url)
        assertEquals("https://example.com", https.url)
    }

    @Test
    fun rejectsUnknownOrMalformedLinks() {
        assertNull(resolveNovelCaptionLink("javascript://alert/1"))
        assertNull(resolveNovelCaptionLink("pixiv://artworks/1"))
        assertNull(resolveNovelCaptionLink("pixiv://novels/not-a-number"))
        assertNull(resolveNovelCaptionLink("pixiv://novels/0"))
        assertNull(resolveNovelCaptionLink("pixiv://novels/1/extra"))
        assertNull(resolveNovelCaptionLink("pixiv://novels/1?mode=unsafe"))
        assertNull(resolveNovelCaptionLink("https:///missing-host"))
        assertNull(resolveNovelCaptionLink("https://:443/missing-host"))
        assertNull(resolveNovelCaptionLink("https://example.com:70000"))
        assertNull(resolveNovelCaptionLink("https://example.com/\nunsafe"))
    }
}
