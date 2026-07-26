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
    fun streamingPreviewKeepsOriginalReadingBodyUntilCompletion() {
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

        val streaming = original.withTranslationRender(
            translatedText = "translated prefix",
            translatedParagraphs = previewParagraphs,
            translatedSpans = previewSpans,
            isComplete = false,
        )

        assertEquals("original body", streaming.novelText)
        assertSame(originalParagraphs, streaming.paragraphs)
        assertSame(originalSpans, streaming.paragraphSpans)
        assertSame(previewSpans, streaming.translationPreviewSpans)
        assertTrue(streaming.isTranslating)

        val completed = streaming.withTranslationRender(
            translatedText = "translated body",
            translatedParagraphs = previewParagraphs,
            translatedSpans = previewSpans,
            isComplete = true,
        )

        assertEquals("translated body", completed.novelText)
        assertSame(previewParagraphs, completed.paragraphs)
        assertSame(previewSpans, completed.paragraphSpans)
        assertTrue(completed.translationPreviewSpans.isEmpty())
        assertFalse(completed.isTranslating)
        assertTrue(completed.isTranslated)
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
