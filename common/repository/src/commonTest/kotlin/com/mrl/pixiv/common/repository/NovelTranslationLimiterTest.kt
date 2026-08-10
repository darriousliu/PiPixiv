package com.mrl.pixiv.common.repository

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NovelTranslationLimiterTest {
    @Test
    fun concurrentRequestsNeverExceedConfiguredLimit() = runTest {
        val limiter = NovelTranslationLimiter()
        val release = CompletableDeferred<Unit>()
        var activeRequests = 0
        var peakRequests = 0

        val requests = List(6) {
            async {
                limiter.withPermit(maxConcurrentRequests = 2) {
                    activeRequests += 1
                    peakRequests = maxOf(peakRequests, activeRequests)
                    try {
                        release.await()
                    } finally {
                        activeRequests -= 1
                    }
                }
            }
        }

        runCurrent()
        assertEquals(2, peakRequests)
        release.complete(Unit)
        requests.awaitAll()
        assertEquals(0, activeRequests)
    }

    @Test
    fun waitingRequestsAcquirePermitsInFifoOrder() = runTest {
        val limiter = NovelTranslationLimiter()
        val holderStarted = CompletableDeferred<Unit>()
        val releaseHolder = CompletableDeferred<Unit>()
        val order = mutableListOf<Int>()

        val holder = async {
            limiter.withPermit(maxConcurrentRequests = 1) {
                holderStarted.complete(Unit)
                releaseHolder.await()
            }
        }
        holderStarted.await()

        val queued = (1..3).map { request ->
            async {
                limiter.withPermit(maxConcurrentRequests = 1) {
                    order += request
                }
            }
        }
        runCurrent()
        assertTrue(order.isEmpty())

        releaseHolder.complete(Unit)
        holder.await()
        queued.awaitAll()
        assertEquals(listOf(1, 2, 3), order)
    }

    @Test
    fun cancellationReleasesActivePermitAndRemovesWaitingRequest() = runTest {
        val limiter = NovelTranslationLimiter()
        val activeStarted = CompletableDeferred<Unit>()
        var cancelledWaiterStarted = false
        var successorStarted = false

        val active = async {
            limiter.withPermit(maxConcurrentRequests = 1) {
                activeStarted.complete(Unit)
                awaitCancellation()
            }
        }
        activeStarted.await()

        val cancelledWaiter = async {
            limiter.withPermit(maxConcurrentRequests = 1) {
                cancelledWaiterStarted = true
            }
        }
        val successor = async {
            limiter.withPermit(maxConcurrentRequests = 1) {
                successorStarted = true
            }
        }
        runCurrent()

        cancelledWaiter.cancelAndJoin()
        active.cancelAndJoin()
        successor.await()

        assertFalse(cancelledWaiterStarted)
        assertTrue(successorStarted)
    }

    @Test
    fun failureReleasesPermitForNextRequest() = runTest {
        val limiter = NovelTranslationLimiter()

        assertFailsWith<IllegalStateException> {
            limiter.withPermit(maxConcurrentRequests = 1) {
                error("provider failed")
            }
        }

        val result = limiter.withPermit(maxConcurrentRequests = 1) {
            "continued"
        }
        assertEquals("continued", result)
    }

    @Test
    fun activeLowerLimitConstrainsLaterHigherLimitRequest() = runTest {
        val limiter = NovelTranslationLimiter()
        val lowerStarted = CompletableDeferred<Unit>()
        val releaseLower = CompletableDeferred<Unit>()
        var higherStarted = false

        val lower = async {
            limiter.withPermit(maxConcurrentRequests = 1) {
                lowerStarted.complete(Unit)
                releaseLower.await()
            }
        }
        lowerStarted.await()
        val higher = async {
            limiter.withPermit(maxConcurrentRequests = 8) {
                higherStarted = true
            }
        }

        runCurrent()
        assertFalse(higherStarted)
        releaseLower.complete(Unit)
        lower.await()
        higher.await()
        assertTrue(higherStarted)
    }

    @Test
    fun lowerLimitQueueHeadCannotBeBypassedByHigherLimitRequest() = runTest {
        val limiter = NovelTranslationLimiter()
        val activeStarted = List(2) { CompletableDeferred<Unit>() }
        val releaseActive = CompletableDeferred<Unit>()
        val order = mutableListOf<String>()

        val active = activeStarted.mapIndexed { index, started ->
            async {
                limiter.withPermit(maxConcurrentRequests = 8) {
                    started.complete(Unit)
                    releaseActive.await()
                    order += "active-$index"
                }
            }
        }
        activeStarted.forEach { it.await() }

        val lowerHead = async {
            limiter.withPermit(maxConcurrentRequests = 1) {
                order += "lower"
            }
        }
        val higherFollower = async {
            limiter.withPermit(maxConcurrentRequests = 8) {
                order += "higher"
            }
        }
        runCurrent()
        assertTrue(order.isEmpty())

        releaseActive.complete(Unit)
        active.awaitAll()
        lowerHead.await()
        higherFollower.await()

        assertEquals("lower", order[2])
        assertEquals("higher", order[3])
    }
}
