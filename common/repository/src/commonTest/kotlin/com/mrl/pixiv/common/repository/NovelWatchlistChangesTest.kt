package com.mrl.pixiv.common.repository

import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class NovelWatchlistChangesTest {
    @Test
    fun publishesChangedSeriesIdToActiveWatchlists() = runTest {
        val received = async(start = CoroutineStart.UNDISPATCHED) {
            NovelWatchlistChanges.changes.first()
        }

        NovelWatchlistChanges.notifyChanged(42)

        assertEquals(42, received.await())
    }
}
