package com.mrl.pixiv.common.repository.viewmodel.follow

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class FollowRequestStateTest {
    @Test
    fun loadingStartsImmediatelyAndDeduplicatesOnlyTheSameUser() = runTest {
        val requests = FollowRequestState { request ->
            backgroundScope.launch(start = CoroutineStart.LAZY) { request() }
        }
        val response = CompletableDeferred<Unit>()
        var calls = 0
        val follow = requests.update(42, isFollowing = true) {
            calls++
            response.await()
        }

        assertTrue(requests.isUpdating(42))
        assertFalse(requests.isUpdating(43))
        assertEquals(0, calls)
        assertSame(follow, requests.update(42, isFollowing = false) { calls++ })
        requests.update(43, isFollowing = false) {
            calls++
            response.await()
        }

        runCurrent()
        assertEquals(2, calls)
        response.complete(Unit)
        runCurrent()

        assertFalse(requests.isUpdating(42))
        assertFalse(requests.isUpdating(43))
        assertEquals(true, requests.state[42])
        assertEquals(false, requests.state[43])
    }

    @Test
    fun failureKeepsTheRelationshipAndAllowsRetry() = runTest {
        val failure = IllegalStateException("Request failed")
        var reportedFailure: Throwable? = null
        val requests = FollowRequestState { request ->
            backgroundScope.launch(start = CoroutineStart.LAZY) {
                try {
                    request()
                } catch (error: IllegalStateException) {
                    reportedFailure = error
                }
            }
        }
        requests.state[42] = true
        requests.update(42, isFollowing = false) { throw failure }
        runCurrent()

        assertSame(failure, reportedFailure)
        assertEquals(true, requests.state[42])
        assertFalse(requests.isUpdating(42))

        requests.update(42, isFollowing = false) {}
        assertTrue(requests.isUpdating(42))
        runCurrent()
        assertEquals(false, requests.state[42])
        assertFalse(requests.isUpdating(42))
    }

    @Test
    fun cancellationBeforeAndAfterStartingReleasesLoading() = runTest {
        val requests = FollowRequestState { request ->
            backgroundScope.launch(start = CoroutineStart.LAZY) { request() }
        }
        requests.state[42] = false
        var calls = 0
        val queued = requests.update(42, isFollowing = true) { calls++ }
        queued.cancel()
        runCurrent()

        assertEquals(0, calls)
        assertFalse(requests.isUpdating(42))

        val running = requests.update(42, isFollowing = true) {
            calls++
            awaitCancellation()
        }
        runCurrent()
        assertEquals(1, calls)
        assertTrue(requests.isUpdating(42))
        running.cancel()
        runCurrent()

        assertFalse(requests.isUpdating(42))
        assertEquals(false, requests.state[42])
    }

    @Test
    fun immediatelyCompletedRequestDoesNotLeaveLoadingBehind() = runTest {
        val requests = FollowRequestState { request ->
            backgroundScope.launch(
                UnconfinedTestDispatcher(testScheduler),
                start = CoroutineStart.LAZY
            ) {
                request()
            }
        }

        val job = requests.update(42, isFollowing = true) {}

        assertTrue(job.isCompleted)
        assertFalse(requests.isUpdating(42))
        assertEquals(true, requests.state[42])
    }
}
