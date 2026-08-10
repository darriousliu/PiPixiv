package com.mrl.pixiv.novel

import com.mrl.pixiv.common.repository.NovelTranslationStreamProgress
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

class NovelTranslationRenderTest {
    @Test
    fun cacheCommitRunsOnlyForCompletedCurrentTranslation() = runTest {
        val saved = mutableListOf<String>()

        val completed = commitCompletedNovelTranslation(
            completedText = "translated",
            isCurrent = true,
            saveTranslation = saved::add,
        )

        assertEquals("translated", completed)
        assertEquals(listOf("translated"), saved)

        assertFailsWith<IllegalArgumentException> {
            commitCompletedNovelTranslation(
                completedText = null,
                isCurrent = true,
                saveTranslation = saved::add,
            )
        }
        assertFailsWith<IllegalStateException> {
            commitCompletedNovelTranslation(
                completedText = "obsolete",
                isCurrent = false,
                saveTranslation = saved::add,
            )
        }
        assertEquals(listOf("translated"), saved)
    }

    @Test
    fun translationPresentationMovesFromWaitingToStreamingToCompletedBody() {
        val originalSpans = persistentListOf<NovelSpanData>(
            NovelSpanData.Text("original body"),
        )
        val originalParagraphs = persistentListOf("original body")
        val previewSpans = persistentListOf<NovelSpanData>(
            NovelSpanData.Text("translated prefix"),
        )
        val previewParagraphs = persistentListOf("translated prefix")
        val original = NovelState(
            novelText = "original body",
            paragraphs = originalParagraphs,
            paragraphSpans = originalSpans,
        )

        val waiting = original.withTranslationWaiting()

        assertTrue(waiting.translationPresentation is NovelTranslationPresentation.Waiting)
        assertTrue(waiting.isTranslating)
        assertSame(originalParagraphs, waiting.paragraphs)
        assertSame(originalSpans, waiting.paragraphSpans)

        val streaming = waiting.withStreamingTranslation(
            translatedSpans = previewSpans,
            completedChunks = 1,
            totalChunks = 3,
        )

        assertEquals("original body", streaming.novelText)
        assertSame(originalParagraphs, streaming.paragraphs)
        assertSame(originalSpans, streaming.paragraphSpans)
        val presentation =
            streaming.translationPresentation as NovelTranslationPresentation.Streaming
        assertSame(previewSpans, presentation.spans)
        assertEquals(1, presentation.completedChunks)
        assertEquals(3, presentation.totalChunks)
        assertTrue(streaming.isTranslating)

        val completed = streaming.withCompletedTranslation(
            translatedText = "translated body",
            translatedParagraphs = previewParagraphs,
            translatedSpans = previewSpans,
            translatedTitle = "translated title",
            translatedCaption = "translated caption",
        )

        assertEquals("translated body", completed.novelText)
        assertSame(previewParagraphs, completed.paragraphs)
        assertSame(previewSpans, completed.paragraphSpans)
        assertTrue(completed.translationPresentation is NovelTranslationPresentation.Idle)
        assertFalse(completed.isTranslating)
        assertTrue(completed.isTranslated)
        assertEquals("translated title", completed.translatedTitle)
        assertEquals("translated caption", completed.translatedCaption)
    }

    @Test
    fun translatedMetadataFollowsOriginalTextToggleAndFallsBackIndependently() {
        assertEquals(
            "translated title",
            resolveNovelMetadataText(
                original = "original title",
                translated = "translated title",
                isTranslated = true,
                isShowingOriginalText = false,
            ),
        )
        assertEquals(
            "original title",
            resolveNovelMetadataText(
                original = "original title",
                translated = "translated title",
                isTranslated = true,
                isShowingOriginalText = true,
            ),
        )
        assertEquals(
            "original caption",
            resolveNovelMetadataText(
                original = "original caption",
                translated = "",
                isTranslated = true,
                isShowingOriginalText = false,
            ),
        )
        assertEquals(
            "original title",
            resolveNovelMetadataText(
                original = "original title",
                translated = "translated title",
                isTranslated = false,
                isShowingOriginalText = false,
            ),
        )
    }

    @Test
    fun streamingSpansKeepTheEntirePrefixBeyondLegacyPreviewLimit() {
        val prefix = "prefix-start-" + "x".repeat(1_300) + "-prefix-end"

        val spans = buildNovelTranslationSpans(
            text = prefix,
            novelTextResp = null,
        )

        assertEquals(persistentListOf(NovelSpanData.Text(prefix)), spans)
    }

    @Test
    fun streamingSpansPreserveImagesLinksAndPageBreaksInOrder() {
        val spans = buildNovelTranslationSpans(
            text = "A[pixivimage:123][[jumpuri:https://www.pixiv.net/novel/show.php?id=1]][newpage]B",
            novelTextResp = null,
        )

        assertEquals(
            listOf(
                NovelSpanData.Text("A"),
                NovelSpanData.PixivImage(
                    illustId = 123L,
                    targetIndex = 0,
                    token = "[pixivimage:123]",
                    imageUrl = null,
                ),
                NovelSpanData.JumpUri(
                    value = "https://www.pixiv.net/novel/show.php?id=1",
                    url = "https://www.pixiv.net/novel/show.php?id=1",
                ),
                NovelSpanData.NewPage,
                NovelSpanData.Text("B"),
            ),
            spans,
        )
    }

    @Test
    fun onlyNonStreamingCompletionRestoresSavedReadingProgress() {
        assertTrue(
            shouldRestoreProgressAfterTranslation(renderedStreamingBody = false)
        )
        assertFalse(
            shouldRestoreProgressAfterTranslation(renderedStreamingBody = true)
        )
    }

    @Test
    fun listAnchorRestoresOnlyWhenAnActiveTranslationEndsWithoutSuccess() {
        assertTrue(
            shouldRestoreNovelTranslationListAnchor(
                wasTranslating = true,
                isTranslating = false,
                isTranslated = false,
            )
        )
        assertFalse(
            shouldRestoreNovelTranslationListAnchor(
                wasTranslating = true,
                isTranslating = false,
                isTranslated = true,
            )
        )
        assertFalse(
            shouldRestoreNovelTranslationListAnchor(
                wasTranslating = false,
                isTranslating = false,
                isTranslated = false,
            )
        )
        assertFalse(
            shouldRestoreNovelTranslationListAnchor(
                wasTranslating = true,
                isTranslating = true,
                isTranslated = false,
            )
        )
    }

    @Test
    fun listAnchorUsesRestoredBodyBoundsInsteadOfPreviousStreamingLayout() {
        assertEquals(
            42,
            resolveNovelTranslationListAnchorItemIndex(
                requestedItemIndex = 42,
                paragraphStartItemIndex = 10,
                paragraphCount = 100,
            ),
        )
        assertEquals(
            110,
            resolveNovelTranslationListAnchorItemIndex(
                requestedItemIndex = 999,
                paragraphStartItemIndex = 10,
                paragraphCount = 100,
            ),
        )
        assertEquals(
            0,
            resolveNovelTranslationListAnchorItemIndex(
                requestedItemIndex = -1,
                paragraphStartItemIndex = 10,
                paragraphCount = 100,
            ),
        )
    }

    @Test
    fun staleNovelScopedUpdateCannotApplyToAnotherChapter() {
        assertTrue(
            shouldApplyNovelScopedUpdate(
                currentNovelId = 2L,
                requestedNovelId = 2L,
            )
        )
        assertFalse(
            shouldApplyNovelScopedUpdate(
                currentNovelId = 2L,
                requestedNovelId = 1L,
            )
        )
        assertFalse(
            shouldApplyNovelScopedUpdate(
                currentNovelId = null,
                requestedNovelId = 1L,
            )
        )
    }

    @Test
    fun firstDeltaAndChunkCompletionBypassThrottle() {
        val delta = progress(completedChunks = 0)
        val firstChunkCompleted = progress(completedChunks = 1)

        assertTrue(
            shouldRenderNovelTranslation(
                progress = delta,
                renderedFirstDelta = false,
                lastRenderedCompletedChunks = 0,
                renderIntervalElapsed = false,
            )
        )
        assertTrue(
            shouldRenderNovelTranslation(
                progress = firstChunkCompleted,
                renderedFirstDelta = true,
                lastRenderedCompletedChunks = 0,
                renderIntervalElapsed = false,
            )
        )
    }

    @Test
    fun intermediateDeltaWaitsForThrottleInterval() {
        val delta = progress(completedChunks = 0)

        assertFalse(
            shouldRenderNovelTranslation(
                progress = delta,
                renderedFirstDelta = true,
                lastRenderedCompletedChunks = 0,
                renderIntervalElapsed = false,
            )
        )
        assertTrue(
            shouldRenderNovelTranslation(
                progress = delta,
                renderedFirstDelta = true,
                lastRenderedCompletedChunks = 0,
                renderIntervalElapsed = true,
            )
        )
    }

    private fun progress(completedChunks: Int) = NovelTranslationStreamProgress(
        text = "translated",
        completedChunks = completedChunks,
        totalChunks = 2,
        isComplete = false,
    )
}
