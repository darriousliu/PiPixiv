package com.mrl.pixiv.community

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class NotificationTextTest {
    @Test
    fun convertsNotificationMarkupWithFormatting() {
        val text = notificationTextToAnnotatedString(
            "<b><a href=\"pixiv://users/42\">Alice &amp; Bob</a></b>关注了你。" +
                    "<br>作品：<i><u>&lt;星空&gt;</u></i> &#128150;" +
                    "<script>alert('ignored')</script>"
        )

        assertEquals("Alice & Bob关注了你。\n作品：<星空> 💖", text.text)
        assertTrue(text.spanStyles.any { range ->
            range.item.fontWeight == FontWeight.Bold && range.start == 0 && range.end == 11
        })
        val titleStart = text.text.indexOf("<星空>")
        assertTrue(text.spanStyles.any { range ->
            range.item.fontStyle == FontStyle.Italic &&
                    range.start == titleStart && range.end == titleStart + 4
        })
        assertTrue(text.spanStyles.any { range ->
            range.item.textDecoration == TextDecoration.Underline &&
                    range.start == titleStart && range.end == titleStart + 4
        })
        assertEquals(
            "pixiv://users/42",
            assertIs<LinkAnnotation.Url>(text.getLinkAnnotations(0, text.length).single().item).url,
        )
    }

    @Test
    fun preservesPlainTextNotifications() {
        val text = "画师 Alice 关注了你。"

        assertEquals(AnnotatedString(text), notificationTextToAnnotatedString(text))
    }

    @Test
    fun emptyMarkupRemainsBlankForTheNotificationFallback() {
        assertTrue(notificationTextToAnnotatedString("<b> </b><br>&nbsp;").isBlank())
    }
}
