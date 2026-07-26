package com.mrl.pixiv.common.repository

import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class NovelMarkerChangesTest {
    @Test
    fun publishesChangedNovelIdToActiveMarkerLists() = runTest {
        val received = async(start = CoroutineStart.UNDISPATCHED) {
            NovelMarkerChanges.changes.first()
        }

        NovelMarkerChanges.notifyChanged(42)

        assertEquals(42, received.await())
    }
}
