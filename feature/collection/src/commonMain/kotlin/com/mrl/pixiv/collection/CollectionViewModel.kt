package com.mrl.pixiv.collection

import androidx.compose.runtime.Stable
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import com.mrl.pixiv.common.data.Novel
import com.mrl.pixiv.common.data.Restrict
import com.mrl.pixiv.common.data.user.UserBookmarksQuery
import com.mrl.pixiv.common.repository.util.queryParams
import kotlinx.coroutines.CancellationException
import com.mrl.pixiv.common.repository.isSelf
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
    val tagPages: Map<CollectionTagKey, CollectionTagPage> = emptyMap(),
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

data class CollectionTagKey(val novel: Boolean, val restrict: Restrict)
data class CollectionTagPage(
    val nextUrl: String? = null,
    val loading: Boolean = false,
    val failed: Boolean = false,
    val loaded: Boolean = false,
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
                restrict = if (uid.isSelf) restrict else Restrict.PUBLIC,
                filterTag = filterTag
            )
        }
    }

    fun updateNovelFilterTag(restrict: Restrict, filterTag: String?) {
        updateState {
            copy(
                novelRestrict = if (uid.isSelf) restrict else Restrict.PUBLIC,
                novelFilterTag = filterTag
            )
        }
    }

    private fun loadUserBookmarkTagsIllust(restrict: Restrict) = loadTags(false, restrict, false)
    private fun loadUserBookmarkTagsNovel(restrict: Restrict) = loadTags(true, restrict, false)

    fun loadMoreTags(novel: Boolean, restrict: Restrict) {
        val page = state.tagPages[CollectionTagKey(novel, restrict)]
        loadTags(novel, restrict, page?.loaded == true)
    }

    private fun loadTags(novel: Boolean, restrict: Restrict, append: Boolean) {
        if (!uid.isSelf && restrict != Restrict.PUBLIC) return
        val key = CollectionTagKey(novel, restrict)
        val page = state.tagPages[key] ?: CollectionTagPage()
        if (page.loading || (append && page.nextUrl == null)) return
        updateState { copy(tagPages = tagPages + (key to page.copy(loading = true, failed = false))) }
        launchIO {
            try {
                val response = if (append) {
                    val params = requireNotNull(page.nextUrl).queryParams +
                        mapOf("user_id" to uid.toString(), "restrict" to restrict.value)
                    if (novel) PixivRepository.loadMoreUserBookmarkTagsNovel(params)
                    else PixivRepository.loadMoreUserBookmarkTagsIllust(params)
                } else {
                    if (novel) PixivRepository.getUserBookmarkTagsNovel(uid, restrict.value)
                    else PixivRepository.getUserBookmarkTagsIllust(uid, restrict.value)
                }
                val isPublic = restrict == Restrict.PUBLIC
                val added = response.bookmarkTags.map {
                    RestrictBookmarkTag(isPublic, it.count, it.name, it.name)
                }
                updateState {
                    val previous = when {
                        novel && isPublic -> userBookmarkTagsNovel
                        novel -> privateBookmarkTagsNovel
                        isPublic -> userBookmarkTagsIllust
                        else -> privateBookmarkTagsIllust
                    }
                    val tags = ((if (append) previous else generateInitialTags(isPublic)) + added)
                        .distinctBy { it.name }.toImmutableList()
                    val next = response.nextUrl?.takeIf { it.isNotBlank() && (!append || it != page.nextUrl) }
                    val updated = copy(tagPages = tagPages + (key to CollectionTagPage(nextUrl = next, loaded = true)))
                    when {
                        novel && isPublic -> updated.copy(userBookmarkTagsNovel = tags)
                        novel -> updated.copy(privateBookmarkTagsNovel = tags)
                        isPublic -> updated.copy(userBookmarkTagsIllust = tags)
                        else -> updated.copy(privateBookmarkTagsIllust = tags)
                    }
                }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                updateState { copy(tagPages = tagPages + (key to page.copy(failed = true))) }
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