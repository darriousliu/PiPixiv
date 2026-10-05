package com.mrl.pixiv.collection

import com.mrl.pixiv.common.data.Restrict
import com.mrl.pixiv.common.data.collection.CollectionSearchOrder
import com.mrl.pixiv.common.data.collection.CollectionSearchQuery
import com.mrl.pixiv.common.data.collection.CollectionWorkType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class CollectionSearchStateTest {
    @Test
    fun editingDraftKeepsLoadedQueryUntilAppliedThenChangesPagingKey() {
        val loaded = CollectionSearchQuery(type = CollectionWorkType.NOVEL)
        val state = CollectionSearchState(sync = CollectionSyncState.READY, query = loaded,
            draft = loaded.copy(restrict = Restrict.PRIVATE, bookmarkTag = " reading ",
                workTag = " fantasy ", period = "2026-10", order = CollectionSearchOrder.OLDEST))
        assertEquals(loaded, state.query)
        val applied = applyCollectionSearchDraft(state)
        assertNotEquals(loaded, applied.query)
        assertEquals("reading", applied.query.bookmarkTag)
        assertEquals("fantasy", applied.query.workTag)
        assertEquals("2026-10", applied.query.period)
        assertEquals(Restrict.PRIVATE, applied.query.restrict)
        assertEquals(CollectionWorkType.NOVEL, applied.query.type)
        assertEquals(CollectionSearchOrder.OLDEST, applied.query.order)
    }
}
