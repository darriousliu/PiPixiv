package com.mrl.pixiv.profile.detail

import com.mrl.pixiv.common.data.AiType
import com.mrl.pixiv.common.data.ImageUrls
import com.mrl.pixiv.common.data.Novel
import com.mrl.pixiv.common.data.Series
import com.mrl.pixiv.common.data.User
import com.mrl.pixiv.common.data.XRestrict
import com.mrl.pixiv.profile.detail.components.previewNovelWorks
import com.mrl.pixiv.profile.detail.components.shouldShowNovelWorks
import kotlinx.collections.immutable.persistentListOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ProfileNovelWorksTest {
    @Test
    fun emptyNovelWorksHideThePreview() {
        assertFalse(shouldShowNovelWorks(emptyList<Long>()))
    }

    @Test
    fun novelWorksPreviewShowsAtMostThreeItems() {
        assertTrue(shouldShowNovelWorks(listOf(1L)))
        assertEquals(
            listOf(1L, 2L, 3L),
            previewNovelWorks(listOf(1L, 2L, 3L, 4L)),
        )
    }

    @Test
    fun submittedWorksAreIndependentFromBookmarkedNovels() {
        val state = ProfileDetailState(
            userNovels = persistentListOf(novel(id = 10L)),
            userBookmarksNovels = persistentListOf(novel(id = 90L)),
        )

        val preview = previewNovelWorks(state.userNovels)

        assertEquals(listOf(10L), preview.map(Novel::id))
        assertEquals(listOf(90L), state.userBookmarksNovels.map(Novel::id))
    }

    private fun novel(id: Long) = Novel(
        id = id,
        title = "Novel $id",
        caption = "",
        restrict = 0,
        xRestrict = XRestrict.Normal,
        isOriginal = true,
        imageUrls = ImageUrls(),
        createDate = "",
        tags = emptyList(),
        pageCount = 1,
        textLength = 1,
        user = User(id = 1, name = "Author"),
        series = Series(id = 0, title = ""),
        isBookmarked = false,
        totalBookmarks = 0,
        totalView = 0,
        visible = true,
        isMuted = false,
        isMypixivOnly = false,
        isXRestricted = false,
        novelAiType = AiType.NotAiGeneratedWork,
    )
}
