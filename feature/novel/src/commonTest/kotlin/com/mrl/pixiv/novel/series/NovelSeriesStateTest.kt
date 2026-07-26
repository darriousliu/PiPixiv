package com.mrl.pixiv.novel.series

import com.mrl.pixiv.common.data.AiType
import com.mrl.pixiv.common.data.User
import com.mrl.pixiv.common.data.novel.NovelSeriesDetail
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NovelSeriesStateTest {

    @Test
    fun updatesWatchlistStateWithoutDroppingSeriesDetail() {
        val initial = NovelSeriesState(detail = seriesDetail(watchlistAdded = false))

        val followed = initial.withWatchlistAdded(true)
        val unfollowed = followed.withWatchlistAdded(false)

        assertTrue(followed.detail?.watchlistAdded == true)
        assertFalse(unfollowed.detail?.watchlistAdded ?: true)
        assertTrue(followed.detail?.title == initial.detail?.title)
    }

    private fun seriesDetail(watchlistAdded: Boolean) = NovelSeriesDetail(
        id = 7,
        title = "Series",
        caption = "Caption",
        isOriginal = true,
        isConcluded = false,
        contentCount = 3,
        totalCharacterCount = 300,
        user = User(id = 99, name = "Author"),
        displayText = "",
        novelAiType = AiType.NotAiGeneratedWork,
        watchlistAdded = watchlistAdded,
    )
}
