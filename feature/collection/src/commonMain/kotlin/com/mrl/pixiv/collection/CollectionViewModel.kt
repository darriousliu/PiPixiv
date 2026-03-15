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
)

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
}

@KoinViewModel
class CollectionViewModel(
    private val uid: Long,
) : BaseMviViewModel<CollectionState, CollectionAction>(
    initialState = CollectionState(),
) {
    val userBookmarksIllusts = Pager(PagingConfig(pageSize = 20)) {
        CollectionIllustPagingSource(
            uid, UserBookmarksQuery(
                restrict = state.restrict,
                userId = uid,
                tag = state.filterTag
            )
        )
    }.flow.cachedIn(viewModelScope)

    val userBookmarksNovels = Pager(PagingConfig(pageSize = 30)) {
        CollectionNovelPagingSource(
            UserBookmarksQuery(
                restrict = state.novelRestrict,
                userId = uid,
                tag = state.novelFilterTag
            )
        )
    }.flow.cachedIn(viewModelScope)

    override suspend fun handleIntent(intent: CollectionAction) {
        when (intent) {
            is CollectionAction.LoadUserBookmarksTagsIllust -> loadUserBookmarkTagsIllust(intent.restrict)
            is CollectionAction.LoadUserBookmarksTagsNovel -> loadUserBookmarkTagsNovel(intent.restrict)
        }
    }

    fun updateFilterTag(restrict: Restrict, filterTag: String?) {
        updateState {
            copy(
                restrict = restrict,
                filterTag = filterTag
            )
        }
    }

    fun updateNovelFilterTag(restrict: Restrict, filterTag: String?) {
        updateState {
            copy(
                novelRestrict = restrict,
                novelFilterTag = filterTag
            )
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