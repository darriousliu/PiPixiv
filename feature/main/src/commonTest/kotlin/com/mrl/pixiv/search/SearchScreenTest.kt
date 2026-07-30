package com.mrl.pixiv.search

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SearchScreenTest {
    @Test
    fun clearIconIsOnlyShownWhenInputIsNotEmpty() {
        assertFalse(shouldShowSearchInputClearIcon(""))
        assertTrue(shouldShowSearchInputClearIcon("keyword"))
        assertTrue(shouldShowSearchInputClearIcon(" "))
    }
}
