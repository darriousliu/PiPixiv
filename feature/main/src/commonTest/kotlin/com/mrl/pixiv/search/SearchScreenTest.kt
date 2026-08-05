package com.mrl.pixiv.search

import com.mrl.pixiv.common.router.PixivLinkTarget
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SearchScreenTest {
    @Test
    fun clearIconIsOnlyShownWhenInputIsNotEmpty() {
        assertFalse(shouldShowSearchInputClearIcon(""))
        assertTrue(shouldShowSearchInputClearIcon("keyword"))
        assertTrue(shouldShowSearchInputClearIcon(" "))
    }

    @Test
    fun clipboardTextIsHandledOnlyAfterItChanges() {
        val tracker = ClipboardTextChangeTracker()

        assertTrue(tracker.hasChanged("first"))
        assertFalse(tracker.hasChanged("first"))
        assertTrue(tracker.hasChanged("second"))
        assertFalse(tracker.hasChanged("second"))
    }

    @Test
    fun multipleLinksSubmittedFromSearchBoxShowSelectionDialog() {
        val text = """
            https://www.pixiv.net/artworks/123
            https://www.pixiv.net/novel/show.php?id=456
        """.trimIndent()

        assertEquals(
            PixivLinkSearchAction.ShowSelection(
                listOf(
                    PixivLinkTarget.Illust(123),
                    PixivLinkTarget.Novel(456),
                ),
            ),
            resolvePixivLinkSearchAction(text, alwaysShowSelection = false),
        )
    }

    @Test
    fun clipboardLinksAlwaysShowSelectionDialog() {
        assertEquals(
            PixivLinkSearchAction.ShowSelection(listOf(PixivLinkTarget.User(789))),
            resolvePixivLinkSearchAction(
                text = "https://www.pixiv.net/users/789",
                alwaysShowSelection = true,
            ),
        )
    }

    @Test
    fun singleLinkSubmittedFromSearchBoxStillOpensDirectly() {
        assertEquals(
            PixivLinkSearchAction.Open(PixivLinkTarget.Illust(123)),
            resolvePixivLinkSearchAction(
                text = "https://www.pixiv.net/artworks/123",
                alwaysShowSelection = false,
            ),
        )
    }
}
