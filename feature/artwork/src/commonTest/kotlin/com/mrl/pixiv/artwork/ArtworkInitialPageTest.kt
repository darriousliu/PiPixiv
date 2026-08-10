package com.mrl.pixiv.artwork

import com.mrl.pixiv.common.data.Type
import kotlin.test.Test
import kotlin.test.assertEquals

class ArtworkInitialPageTest {
    @Test
    fun existingArtworkRoutesKeepTheirInitialPages() {
        assertEquals(
            0,
            resolveArtworkInitialPage(
                initialType = Type.Illust,
                initialNovel = false,
            ),
        )
        assertEquals(
            2,
            resolveArtworkInitialPage(
                initialType = Type.Manga,
                initialNovel = false,
            ),
        )
    }

    @Test
    fun userNovelsRouteStartsOnTheNovelPage() {
        assertEquals(
            1,
            resolveArtworkInitialPage(
                initialType = Type.Illust,
                initialNovel = true,
            ),
        )
    }
}
