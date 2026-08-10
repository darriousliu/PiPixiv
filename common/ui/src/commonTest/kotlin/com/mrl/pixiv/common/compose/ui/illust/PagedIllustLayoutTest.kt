package com.mrl.pixiv.common.compose.ui.illust

import kotlin.test.Test
import kotlin.test.assertEquals

class PagedIllustLayoutTest {

    @Test
    fun portraitArtworkKeepsOriginalAspectRatio() {
        assertEquals(0.5f, calculateIllustAspectRatio(width = 1000, height = 2000))
    }

    @Test
    fun landscapeArtworkKeepsOriginalAspectRatio() {
        assertEquals(2f, calculateIllustAspectRatio(width = 2000, height = 1000))
    }

    @Test
    fun squareArtworkKeepsSquareAspectRatio() {
        assertEquals(1f, calculateIllustAspectRatio(width = 1000, height = 1000))
    }
}
