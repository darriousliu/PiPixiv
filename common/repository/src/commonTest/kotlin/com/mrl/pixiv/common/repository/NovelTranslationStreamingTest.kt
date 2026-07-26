package com.mrl.pixiv.common.repository

import com.mrl.pixiv.common.ai.provider.AiTextStreamEvent
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class NovelTranslationStreamingTest {
    @Test
    fun appendsConcurrentChunksOnlyInSourceOrder() = runTest {
        val second = async {
            delay(50)
            "B"
        }
        val third = async {
            delay(1)
            "C"
        }

        val progress = combineTranslatedChunks(
            firstChunk = flowOf(
                AiTextStreamEvent.Delta("A"),
                AiTextStreamEvent.Delta("1"),
                AiTextStreamEvent.Completed("A1"),
            ),
            remainingChunks = listOf(second, third),
            totalChunks = 3,
        ).toList()

        assertEquals("A1\nB\nC", progress.last().text)
        assertTrue(progress.last().isComplete)
        progress.forEach { update ->
            assertTrue("A1\nB\nC".startsWith(update.text))
        }
    }

    @Test
    fun cancellationCancelsPendingChunkRequests() = runTest {
        val pending = backgroundScope.async<String> {
            awaitCancellation()
        }

        combineTranslatedChunks(
            firstChunk = flow {
                emit(AiTextStreamEvent.Delta("A"))
                awaitCancellation()
            },
            remainingChunks = listOf(pending),
            totalChunks = 2,
        ).take(1).toList()

        assertTrue(pending.isCancelled)
    }

    @Test
    fun incompleteFirstStreamFailsAndCancelsPendingChunks() = runTest {
        val pending = backgroundScope.async<String> {
            awaitCancellation()
        }

        assertFailsWith<IllegalStateException> {
            combineTranslatedChunks(
                firstChunk = flowOf(AiTextStreamEvent.Delta("partial")),
                remainingChunks = listOf(pending),
                totalChunks = 2,
            ).toList()
        }
        assertTrue(pending.isCancelled)
    }
}
