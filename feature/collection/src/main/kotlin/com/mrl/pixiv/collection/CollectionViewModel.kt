package com.mrl.pixiv.collection

import androidx.compose.runtime.Stable
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import com.mrl.pixiv.common.data.Novel
import com.mrl.pixiv.common.data.Restrict
import com.mrl.pixiv.common.data.user.UserBookmarksIllustQuery
import com.mrl.pixiv.common.repository.PixivRepository
import com.mrl.pixiv.common.repository.paging.CollectionIllustPagingSource
import com.mrl.pixiv.common.util.AppUtil
import com.mrl.pixiv.common.util.RString
import com.mrl.pixiv.common.viewmodel.BaseMviViewModel
import com.mrl.pixiv.common.viewmodel.ViewIntent
import com.mrl.pixiv.common.viewmodel.state
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import org.koin.android.annotation.KoinViewModel

@Stable
data class CollectionState(
    val restrict: Restrict = Restrict.PUBLIC,
    val filterTag: String? = null,
    val userBookmarksNovels: ImmutableList<Novel> = persistentListOf(),
    val userBookmarkTagsIllust: ImmutableList<RestrictBookmarkTag> = persistentListOf(),
    val privateBookmarkTagsIllust: ImmutableList<RestrictBookmarkTag> = persistentListOf(),
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
}

@KoinViewModel
class CollectionViewModel(
    private val uid: Long,
) : BaseMviViewModel<CollectionState, CollectionAction>(
    initialState = CollectionState(),
) {
    val userBookmarksIllusts = Pager(PagingConfig(pageSize = 20)) {
        CollectionIllustPagingSource(
            uid, UserBookmarksIllustQuery(
                restrict = state.restrict,
                userId = uid,
                tag = state.filterTag
            )
        )
    }.flow.cachedIn(viewModelScope)

    override suspend fun handleIntent(intent: CollectionAction) {
        when (intent) {
            is CollectionAction.LoadUserBookmarksTagsIllust -> loadUserBookmarkTagsIllust(intent.restrict)
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

    private fun generateInitialTags(isPublic: Boolean): List<RestrictBookmarkTag> {
        return listOf(
            RestrictBookmarkTag(
                isPublic = isPublic,
                count = null,
                displayName = AppUtil.getString(RString.all),
                name = null,
            ),
            RestrictBookmarkTag(
                isPublic = isPublic,
                count = null,
                displayName = AppUtil.getString(RString.uncategorized),
                name = AppUtil.getString(RString.non_translate_uncategorized)
            )
        )
    }
}