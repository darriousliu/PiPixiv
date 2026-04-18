package com.mrl.pixiv.collection

import androidx.compose.runtime.Stable
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import com.mrl.pixiv.common.data.Novel
import com.mrl.pixiv.common.data.Restrict
import com.mrl.pixiv.common.data.user.UserBookmarksQuery
import com.mrl.pixiv.common.repository.PixivRepository
import com.mrl.pixiv.common.repository.paging.CollectionIllustPagingSource
import com.mrl.pixiv.common.repository.paging.CollectionNovelPagingSource
import com.mrl.pixiv.common.repository.util.queryParams
import com.mrl.pixiv.common.util.AppUtil
import com.mrl.pixiv.common.util.RStrings
import com.mrl.pixiv.common.viewmodel.BaseMviViewModel
import com.mrl.pixiv.common.viewmodel.ViewIntent
import com.mrl.pixiv.common.viewmodel.state
import com.mrl.pixiv.strings.all
import com.mrl.pixiv.strings.non_translate_uncategorized
import com.mrl.pixiv.strings.uncategorized
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.koin.android.annotation.KoinViewModel

@Stable
data class CollectionState(
    val restrict: Restrict = Restrict.PUBLIC,
    val filterTag: String? = null,
    val novelRestrict: Restrict = Restrict.PUBLIC,
    val novelFilterTag: String? = null,
    val userBookmarksNovels: ImmutableList<Novel> = persistentListOf(),
    val userBookmarkTagsIllust: ImmutableList<RestrictBookmarkTag> = persistentListOf(),
    val privateBookmarkTagsIllust: ImmutableList<RestrictBookmarkTag> = persistentListOf(),
    val userBookmarkTagsNovel: ImmutableList<RestrictBookmarkTag> = persistentListOf(),
    val privateBookmarkTagsNovel: ImmutableList<RestrictBookmarkTag> = persistentListOf(),
    val illustMaxBookmarkId: Long? = null,
    val novelMaxBookmarkId: Long? = null,
    val isJumpingPage: Boolean = false,
    val jumpPageError: JumpPageError? = null,
)

enum class JumpPageError {
    INVALID_INPUT,
    OUT_OF_RANGE,
}

@Stable
data class RestrictBookmarkTag(
    val isPublic: Boolean,
    val count: Long? = null,
    val displayName: String,
    val name: String? = null,
)

sealed class CollectionAction : ViewIntent {
    data class LoadUserBookmarksTagsIllust(val restrict: Restrict) : CollectionAction()
    data class LoadUserBookmarksTagsNovel(val restrict: Restrict) : CollectionAction()
    data class JumpToPageIllust(val page: Int) : CollectionAction()
    data class JumpToPageNovel(val page: Int) : CollectionAction()
    data object ClearJumpPageError : CollectionAction()
}

@KoinViewModel
class CollectionViewModel(
    private val uid: Long,
) : BaseMviViewModel<CollectionState, CollectionAction>(
    initialState = CollectionState(),
) {
    private val _illustRefreshTrigger = MutableStateFlow(0)
    val illustRefreshTrigger = _illustRefreshTrigger.asStateFlow()

    private val _novelRefreshTrigger = MutableStateFlow(0)
    val novelRefreshTrigger = _novelRefreshTrigger.asStateFlow()

    val userBookmarksIllusts = Pager(PagingConfig(pageSize = 30)) {
        CollectionIllustPagingSource(
            uid, UserBookmarksQuery(
                restrict = state.restrict,
                userId = uid,
                tag = state.filterTag,
                maxBookmarkId = state.illustMaxBookmarkId,
            )
        )
    }.flow.cachedIn(viewModelScope)

    val userBookmarksNovels = Pager(PagingConfig(pageSize = 30)) {
        CollectionNovelPagingSource(
            UserBookmarksQuery(
                restrict = state.novelRestrict,
                userId = uid,
                tag = state.novelFilterTag,
                maxBookmarkId = state.novelMaxBookmarkId,
            )
        )
    }.flow.cachedIn(viewModelScope)

    override suspend fun handleIntent(intent: CollectionAction) {
        when (intent) {
            is CollectionAction.LoadUserBookmarksTagsIllust -> loadUserBookmarkTagsIllust(intent.restrict)
            is CollectionAction.LoadUserBookmarksTagsNovel -> loadUserBookmarkTagsNovel(intent.restrict)
            is CollectionAction.JumpToPageIllust -> jumpToPageIllust(intent.page)
            is CollectionAction.JumpToPageNovel -> jumpToPageNovel(intent.page)
            is CollectionAction.ClearJumpPageError -> updateState { copy(jumpPageError = null) }
        }
    }

    fun updateFilterTag(restrict: Restrict, filterTag: String?) {
        updateState {
            copy(
                restrict = restrict,
                filterTag = filterTag,
                illustMaxBookmarkId = null,
            )
        }
    }

    fun updateNovelFilterTag(restrict: Restrict, filterTag: String?) {
        updateState {
            copy(
                novelRestrict = restrict,
                novelFilterTag = filterTag,
                novelMaxBookmarkId = null,
            )
        }
    }

    private suspend fun jumpToPageIllust(targetPage: Int) {
        if (targetPage <= 1) {
            updateState { copy(illustMaxBookmarkId = null, isJumpingPage = false, jumpPageError = null) }
            _illustRefreshTrigger.value++
            return
        }
        updateState { copy(isJumpingPage = true, jumpPageError = null) }
        try {
            var currentQuery = UserBookmarksQuery(
                restrict = state.restrict,
                userId = uid,
                tag = state.filterTag,
            )
            var reachedEnd = false
            repeat(targetPage - 1) {
                val resp = PixivRepository.getUserBookmarksIllust(
                    currentQuery.restrict,
                    uid,
                    currentQuery.tag,
                    currentQuery.maxBookmarkId,
                )
                val nextParams = resp.nextUrl?.queryParams
                if (nextParams != null) {
                    currentQuery = UserBookmarksQuery(
                        restrict = nextParams["restrict"]?.let { Restrict.fromValue(it) }
                            ?: Restrict.PUBLIC,
                        tag = nextParams["tag"],
                        userId = nextParams["user_id"]?.toLongOrNull() ?: uid,
                        maxBookmarkId = nextParams["max_bookmark_id"]?.toLongOrNull(),
                    )
                } else {
                    reachedEnd = true
                    return@repeat
                }
            }
            if (reachedEnd) {
                updateState { copy(isJumpingPage = false, jumpPageError = JumpPageError.OUT_OF_RANGE) }
            } else {
                updateState {
                    copy(
                        illustMaxBookmarkId = currentQuery.maxBookmarkId,
                        isJumpingPage = false,
                        jumpPageError = null,
                    )
                }
                _illustRefreshTrigger.value++
            }
        } catch (e: Exception) {
            updateState { copy(isJumpingPage = false, jumpPageError = JumpPageError.OUT_OF_RANGE) }
        }
    }

    private suspend fun jumpToPageNovel(targetPage: Int) {
        if (targetPage <= 1) {
            updateState { copy(novelMaxBookmarkId = null, isJumpingPage = false, jumpPageError = null) }
            _novelRefreshTrigger.value++
            return
        }
        updateState { copy(isJumpingPage = true, jumpPageError = null) }
        try {
            var currentQuery = UserBookmarksQuery(
                restrict = state.novelRestrict,
                userId = uid,
                tag = state.novelFilterTag,
            )
            var reachedEnd = false
            repeat(targetPage - 1) {
                val resp = PixivRepository.getUserBookmarksNovels(
                    currentQuery.restrict,
                    uid,
                    currentQuery.tag,
                    currentQuery.maxBookmarkId,
                )
                val nextParams = resp.nextUrl?.queryParams
                if (nextParams != null) {
                    currentQuery = UserBookmarksQuery(
                        restrict = nextParams["restrict"]?.let { Restrict.fromValue(it) }
                            ?: Restrict.PUBLIC,
                        tag = nextParams["tag"],
                        userId = nextParams["user_id"]?.toLongOrNull() ?: uid,
                        maxBookmarkId = nextParams["max_bookmark_id"]?.toLongOrNull(),
                    )
                } else {
                    reachedEnd = true
                    return@repeat
                }
            }
            if (reachedEnd) {
                updateState { copy(isJumpingPage = false, jumpPageError = JumpPageError.OUT_OF_RANGE) }
            } else {
                updateState {
                    copy(
                        novelMaxBookmarkId = currentQuery.maxBookmarkId,
                        isJumpingPage = false,
                        jumpPageError = null,
                    )
                }
                _novelRefreshTrigger.value++
            }
        } catch (e: Exception) {
            updateState { copy(isJumpingPage = false, jumpPageError = JumpPageError.OUT_OF_RANGE) }
        }
    }

    private fun loadUserBookmarkTagsIllust(restrict: Restrict) {
        launchIO {
            val resp = PixivRepository.getUserBookmarkTagsIllust(
                userId = uid,
                restrict = restrict.value
            )
            val isPublic = restrict == Restrict.PUBLIC
            updateState {
                if (isPublic) {
                    copy(
                        userBookmarkTagsIllust = (generateInitialTags(true) +
                                resp.bookmarkTags.map {
                                    RestrictBookmarkTag(
                                        isPublic = true,
                                        count = it.count,
                                        displayName = it.name,
                                        name = it.name
                                    )
                                }).toImmutableList()
                    )
                } else {
                    copy(
                        privateBookmarkTagsIllust = (generateInitialTags(false) +
                                resp.bookmarkTags.map {
                                    RestrictBookmarkTag(
                                        isPublic = false,
                                        count = it.count,
                                        displayName = it.name,
                                        name = it.name
                                    )
                                }).toImmutableList()
                    )
                }
            }
        }
    }

    private fun loadUserBookmarkTagsNovel(restrict: Restrict) {
        launchIO {
            val resp = PixivRepository.getUserBookmarkTagsNovel(
                userId = uid,
                restrict = restrict.value
            )
            val isPublic = restrict == Restrict.PUBLIC
            updateState {
                if (isPublic) {
                    copy(
                        userBookmarkTagsNovel = (generateInitialTags(true) +
                                resp.bookmarkTags.map {
                                    RestrictBookmarkTag(
                                        isPublic = true,
                                        count = it.count,
                                        displayName = it.name,
                                        name = it.name
                                    )
                                }).toImmutableList()
                    )
                } else {
                    copy(
                        privateBookmarkTagsNovel = (generateInitialTags(false) +
                                resp.bookmarkTags.map {
                                    RestrictBookmarkTag(
                                        isPublic = false,
                                        count = it.count,
                                        displayName = it.name,
                                        name = it.name
                                    )
                                }).toImmutableList()
                    )
                }
            }
        }
    }

    private fun generateInitialTags(isPublic: Boolean): List<RestrictBookmarkTag> {
        return listOf(
            RestrictBookmarkTag(
                isPublic = isPublic,
                count = null,
                displayName = AppUtil.getString(RStrings.all),
                name = null,
            ),
            RestrictBookmarkTag(
                isPublic = isPublic,
                count = null,
                displayName = AppUtil.getString(RStrings.uncategorized),
                name = AppUtil.getString(RStrings.non_translate_uncategorized)
            )
        )
    }
}