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
                AiTextStreamEvent.Completed("A"),
            ),
            remainingChunks = listOf(second, third),
            totalChunks = 3,
        ).toList()

        assertEquals("A\nB\nC", progress.last().text)
        assertTrue(progress.last().isComplete)
        assertEquals(
            listOf("A", "A\nB", "A\nB\nC"),
            progress.map { it.text }.distinct(),
        )
    }

    @Test
    fun cancellationCancelsPendingChunkRequests() = runTest {
        var firstStreamCancelled = false
        val second = backgroundScope.async<String> {
            awaitCancellation()
        }
        val third = backgroundScope.async<String> {
            awaitCancellation()
        }

        combineTranslatedChunks(
            firstChunk = flow {
                try {
                    emit(AiTextStreamEvent.Delta("A"))
                    awaitCancellation()
                } finally {
                    firstStreamCancelled = true
                }
            },
            remainingChunks = listOf(second, third),
            totalChunks = 3,
        ).take(1).toList()

        assertTrue(firstStreamCancelled)
        assertTrue(second.isCancelled)
        assertTrue(third.isCancelled)
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
