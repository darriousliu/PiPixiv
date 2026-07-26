package com.mrl.pixiv.novel

import com.mrl.pixiv.common.repository.NovelReadingProgress
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class NovelMarkerPageTest {
    private val spans = listOf(
        NovelSpanData.Text("page one"),
        NovelSpanData.NewPage,
        NovelSpanData.Text("page two"),
        NovelSpanData.NewPage,
        NovelSpanData.Text("page three"),
    )

    @Test
    fun pageNumberCountsNewPageTokensBeforeCurrentParagraph() {
        assertEquals(listOf(1, 1, 2, 2, 3), markerPagesForSpans(spans))
        assertEquals(1, markerPageForParagraph(spans, 0))
        assertEquals(1, markerPageForParagraph(spans, 1))
        assertEquals(2, markerPageForParagraph(spans, 2))
        assertEquals(3, markerPageForParagraph(spans, 4))
    }

    @Test
    fun markerPageResolvesToFirstParagraphAfterBoundary() {
        assertEquals(0, paragraphIndexForMarkerPage(spans, 1))
        assertEquals(2, paragraphIndexForMarkerPage(spans, 2))
        assertEquals(4, paragraphIndexForMarkerPage(spans, 3))
    }

    @Test
    fun outOfRangePagesAreClampedToContent() {
        assertEquals(1, markerPageForParagraph(spans, -1))
        assertEquals(3, markerPageForParagraph(spans, Int.MAX_VALUE))
        assertEquals(0, paragraphIndexForMarkerPage(spans, 0))
        assertEquals(spans.lastIndex, paragraphIndexForMarkerPage(spans, Int.MAX_VALUE))
    }

    @Test
    fun markerIsSavedOrOverwrittenUnlessCurrentPageIsAlreadyMarked() {
        assertEquals(
            NovelMarkerMutation.Save(page = 1),
            resolveNovelMarkerMutation(savedPage = null, currentPage = 0),
        )
        assertEquals(
            NovelMarkerMutation.Save(page = 3),
            resolveNovelMarkerMutation(savedPage = 2, currentPage = 3),
        )
        assertEquals(
            NovelMarkerMutation.Delete,
            resolveNovelMarkerMutation(savedPage = 3, currentPage = 3),
        )
    }

    @Test
    fun markerEntryOverridesProgressOnlyForInitialNovel() {
        assertEquals(
            4,
            initialMarkerPageForNovel(
                initialNovelId = 100,
                loadedNovelId = 100,
                requestedMarkerPage = 4,
            ),
        )
        assertNull(
            initialMarkerPageForNovel(
                initialNovelId = 100,
                loadedNovelId = 100,
                requestedMarkerPage = null,
            )
        )
        assertNull(
            initialMarkerPageForNovel(
                initialNovelId = 100,
                loadedNovelId = 101,
                requestedMarkerPage = 4,
            )
        )
    }

    @Test
    fun markerResponseUpdatesOnlyTheChapterThatStartedTheRequest() {
        assertTrue(
            shouldApplyNovelMarkerUpdate(
                currentNovelId = 100,
                markerNovelId = 100,
            )
        )
        assertTrue(
            !shouldApplyNovelMarkerUpdate(
                currentNovelId = 101,
                markerNovelId = 100,
            )
        )
        assertTrue(
            !shouldApplyNovelMarkerUpdate(
                currentNovelId = null,
                markerNovelId = 100,
            )
        )
    }

    @Test
    fun markerProgressDoesNotLeakIntoAdjacentChapter() {
        val session = NovelProgressSession()
        val markerProgress = NovelReadingProgress(
            paragraphIndex = 8,
            charIndex = 0,
            paragraphHash = 800,
        )
        val adjacentChapterProgress = NovelReadingProgress(
            paragraphIndex = 2,
            charIndex = 12,
            paragraphHash = 200,
        )

        session.update(novelId = 100, progress = markerProgress)

        assertEquals(markerProgress, session.get(novelId = 100))
        assertNull(session.get(novelId = 101))

        session.update(novelId = 101, progress = adjacentChapterProgress)
        assertEquals(adjacentChapterProgress, session.get(novelId = 101))
        assertNull(session.get(novelId = 100))
    }

    @Test
    fun collectionAndReadingMarkerStatesRemainIndependent() {
        val collectedNovel = NovelState(
            isBookmarked = true,
            markerPage = null,
        )
        val markedNovel = collectedNovel.copy(markerPage = 2)
        val markerRemoved = markedNovel.copy(markerPage = null)

        assertTrue(markedNovel.isBookmarked)
        assertEquals(2, markedNovel.markerPage)
        assertTrue(markerRemoved.isBookmarked)
        assertNull(markerRemoved.markerPage)
    }
}
