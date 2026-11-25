package com.mrl.pixiv.setting.appearance

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AppearanceInputTest {
    @Test
    fun swatchRowsStayBalancedWithoutAddingRows() {
        assertEquals(4, balancedColumns(count = 12, maxColumns = 5))
        assertEquals(4, balancedColumns(count = 11, maxColumns = 5))
        assertEquals(6, balancedColumns(count = 12, maxColumns = 6))
        assertEquals(3, balancedColumns(count = 12, maxColumns = 3))
        assertEquals(1, balancedColumns(count = 12, maxColumns = 0))
        assertEquals(2, balancedColumns(count = 2, maxColumns = 8))
    }

    @Test
    fun hexCodeAcceptsOptionalHashAndSurroundingSpaces() {
        assertEquals(0xFF2468AC, "#2468AC".parseColorHex())
        assertEquals(0xFF2468AC, "2468ac".parseColorHex())
        assertEquals(0xFF2468AC, " #2468ac ".parseColorHex())
        assertNull("#2468".parseColorHex())
        assertNull("##2468AC".parseColorHex())
        assertNull("#24G8AC".parseColorHex())
    }

    @Test
    fun partialHexCodeOnlyShowsErrorAfterLeavingTheField() {
        assertFalse("#2468".showsColorHexError(focused = true))
        assertTrue("#2468".showsColorHexError(focused = false))
        assertTrue("#24G8".showsColorHexError(focused = true))
        assertTrue("#2468ACE".showsColorHexError(focused = true))
        assertFalse("2468AC".showsColorHexError(focused = false))
    }
}
