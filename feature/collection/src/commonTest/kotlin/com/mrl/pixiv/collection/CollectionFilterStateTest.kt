package com.mrl.pixiv.collection

import com.mrl.pixiv.common.data.Restrict
import com.mrl.pixiv.common.data.collection.CollectionSearchOrder
import com.mrl.pixiv.common.data.collection.CollectionSearchQuery
import com.mrl.pixiv.common.data.collection.CollectionWorkType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CollectionFilterStateTest {
    @Test
    fun reopeningDismissedDraftUsesOnlyAppliedListConditions() {
        val applied = CollectionSearchQuery(bookmarkTag = "cats")
        val edited = beginCollectionFilterEdit(applied, true).copy(draft = applied.copy(workTag = "landscape"))
        assertEquals(applied, edited.query)
        assertEquals(applied, beginCollectionFilterEdit(edited.query, true).draft)
    }

    @Test
    fun basicTagsWorkBeforeSyncButAdvancedChangesCannotBeApplied() {
        CollectionSyncState.entries.filter { it != CollectionSyncState.READY }.forEach { status ->
            val basic = CollectionSearchState(sync = status, draft = CollectionSearchQuery(bookmarkTag = "cats"))
            assertTrue(canApplyCollectionDraft(basic))
            val advanced = basic.copy(draft = basic.draft.copy(workTag = "landscape"))
            assertFalse(canApplyCollectionDraft(advanced))
            assertEquals(advanced.query, applyCollectionSearchDraft(advanced).query)
        }
    }

    @Test
    fun resetStaysDraftUntilApplyAndReturnsToOrdinaryBookmarks() {
        val advanced = CollectionSearchQuery(type = CollectionWorkType.NOVEL, restrict = Restrict.PRIVATE,
            bookmarkTag = "reads", workTag = "fantasy", period = "2026-10", order = CollectionSearchOrder.OLDEST)
        val current = CollectionSearchState(sync = CollectionSyncState.FAILED, query = advanced, draft = advanced)
        val reset = resetCollectionFilterDraft(current)
        assertEquals(advanced, reset.query)
        assertFalse(reset.draft.requiresBookmarkSearch)
        assertTrue(canApplyCollectionDraft(reset))
        val applied = applyCollectionSearchDraft(reset)
        assertEquals(CollectionSearchQuery(type = CollectionWorkType.NOVEL), applied.query)
        assertEquals(Restrict.PUBLIC, applied.query.toUserBookmarksQuery(42L).restrict)
    }

    @Test
    fun eachContentTypeKeepsAnIndependentQueryAndQuickVisibilityPreservesOtherFilters() {
        val illust = CollectionSearchQuery(workTag = "cats", order = CollectionSearchOrder.OLDEST)
        val novel = CollectionSearchQuery(type = CollectionWorkType.NOVEL, bookmarkTag = "reads", period = "2026-10")
        val state = CollectionState().withCollectionFilter(illust, true).withCollectionFilter(novel, true)
        val updated = state.withCollectionFilter(state.illustQuery.copy(restrict = Restrict.PRIVATE), true)
        assertEquals(novel, updated.novelQuery)
        assertEquals("cats", updated.illustQuery.workTag)
        assertEquals(CollectionSearchOrder.OLDEST, updated.illustQuery.order)
        assertEquals(Restrict.PRIVATE, updated.restrict)
    }

    @Test
    fun otherUsersAlwaysUsePublicOrdinaryBookmarksAndKeepSelectedTag() {
        val state = CollectionState().withCollectionFilter(CollectionSearchQuery(restrict = Restrict.PRIVATE,
            bookmarkTag = " cats ", workTag = "landscape", period = "2026-10", order = CollectionSearchOrder.OLDEST), false)
        assertEquals(Restrict.PUBLIC, state.restrict)
        assertEquals("cats", state.filterTag)
        assertFalse(state.illustQuery.requiresBookmarkSearch)
        assertEquals(92L, state.illustQuery.toUserBookmarksQuery(92L).userId)
    }
}
