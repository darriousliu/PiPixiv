package com.mrl.pixiv.collection

import com.mrl.pixiv.common.data.Restrict
import com.mrl.pixiv.common.data.collection.CollectionSearchOrder
import com.mrl.pixiv.common.data.collection.CollectionSearchQuery
import com.mrl.pixiv.common.data.collection.CollectionWorkType
import com.mrl.pixiv.common.data.user.UserBookmarksQuery

/** Tags and visibility remain usable while Pixiv builds its advanced search index. */
internal val CollectionSearchQuery.requiresBookmarkSearch: Boolean
    get() = workTag.isNotBlank() || period != null || order != CollectionSearchOrder.NEWEST

internal fun CollectionSearchQuery.normalizedForOwner(isOwner: Boolean): CollectionSearchQuery {
    val normalized = copy(bookmarkTag = bookmarkTag.trim(), workTag = workTag.trim())
    return if (isOwner) normalized else normalized.copy(
        restrict = Restrict.PUBLIC, workTag = "", period = null, order = CollectionSearchOrder.NEWEST,
    )
}

internal fun CollectionSearchQuery.toUserBookmarksQuery(uid: Long) = UserBookmarksQuery(
    userId = uid, restrict = restrict, tag = bookmarkTag.takeIf(String::isNotBlank),
)

internal fun CollectionState.withCollectionFilter(query: CollectionSearchQuery, isOwner: Boolean): CollectionState {
    val normalized = query.normalizedForOwner(isOwner)
    return when (normalized.type) {
        CollectionWorkType.ILLUST -> copy(illustQuery = normalized)
        CollectionWorkType.NOVEL -> copy(novelQuery = normalized)
    }
}

internal fun canApplyCollectionDraft(state: CollectionSearchState): Boolean =
    !state.draft.requiresBookmarkSearch || state.sync == CollectionSyncState.READY

internal fun beginCollectionFilterEdit(query: CollectionSearchQuery, isOwner: Boolean): CollectionSearchState {
    val normalized = query.normalizedForOwner(isOwner)
    return CollectionSearchState(query = normalized, draft = normalized)
}

internal fun resetCollectionFilterDraft(state: CollectionSearchState): CollectionSearchState = state.copy(
    draft = CollectionSearchQuery(type = state.query.type),
)
