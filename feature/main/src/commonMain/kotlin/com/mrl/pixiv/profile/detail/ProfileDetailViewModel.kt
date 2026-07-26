package com.mrl.pixiv.profile.detail

import androidx.compose.runtime.Stable
import androidx.lifecycle.viewModelScope
import com.mrl.pixiv.common.data.Illust
import com.mrl.pixiv.common.data.Novel
import com.mrl.pixiv.common.data.Restrict
import com.mrl.pixiv.common.data.Type
import com.mrl.pixiv.common.data.user.IllustsWithNextUrl
import com.mrl.pixiv.common.data.user.UserDetailResp
import com.mrl.pixiv.common.data.user.UserIllustsResp
import com.mrl.pixiv.common.data.user.UserNovelsResp
import com.mrl.pixiv.common.repository.BlockingRepositoryV2
import com.mrl.pixiv.common.repository.PixivRepository
import com.mrl.pixiv.common.repository.SettingRepository
import com.mrl.pixiv.common.repository.hasDifferentNovelFilterSettings
import com.mrl.pixiv.common.repository.requireUserInfoValue
import com.mrl.pixiv.common.repository.util.filterBlockedTags
import com.mrl.pixiv.common.repository.viewmodel.follow.FollowState
import com.mrl.pixiv.common.viewmodel.BaseMviViewModel
import com.mrl.pixiv.common.viewmodel.ViewIntent
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel

@Stable
data class ProfileDetailState(
    val userTotalWorks: Int = 0,
    val userIllusts: ImmutableList<Illust> = persistentListOf(),
    val userMangas: ImmutableList<Illust> = persistentListOf(),
    val userBookmarksIllusts: ImmutableList<Illust> = persistentListOf(),
    val userBookmarksNovels: ImmutableList<Novel> = persistentListOf(),
    val userInfo: UserDetailResp = UserDetailResp(),
)

sealed class ProfileDetailAction : ViewIntent {
    data object LoadUserData : ProfileDetailAction()
}

@KoinViewModel
class ProfileDetailViewModel(
    private val uid: Long?,
) : BaseMviViewModel<ProfileDetailState, ProfileDetailAction>(
    initialState = ProfileDetailState(),
) {
    private val originalUserBookmarksNovels = MutableStateFlow<List<Novel>?>(null)

    init {
        observeNovelBookmarkFilterSettings()
        dispatch(ProfileDetailAction.LoadUserData)
    }

    override suspend fun handleIntent(intent: ProfileDetailAction) {
        when (intent) {
            is ProfileDetailAction.LoadUserData -> loadUserData()
        }
    }

    private fun loadUserData() {
        launchIO {
            val userId = uid ?: requireUserInfoValue.user.id
            val resp = awaitAll(
                async {
                    PixivRepository.getUserIllusts(
                        userId = userId,
                        type = Type.Illust.value,
                    )
                },
                async {
                    PixivRepository.getUserIllusts(
                        userId = userId,
                        type = Type.Manga.value,
                    )
                },
                async {
                    PixivRepository.getUserBookmarksNovels(
                        restrict = Restrict.PUBLIC,
                        userId = userId
                    )
                },
                async {
                    PixivRepository.getUserBookmarksIllust(
                        restrict = Restrict.PUBLIC,
                        userId = userId
                    )
                },
                async {
                    PixivRepository.getUserDetail(userId = userId)
                }
            )
            val userIllusts = resp[0] as UserIllustsResp
            val userMangas = resp[1] as UserIllustsResp
            val userBookmarksNovels = resp[2] as UserNovelsResp
            val userBookmarksIllusts = resp[3] as IllustsWithNextUrl
            val userInfo = resp[4] as UserDetailResp
            val initialFilteredNovels = userBookmarksNovels.novels.filterBlockedTags()
            updateState {
                copy(
                    userIllusts = userIllusts.illusts.toImmutableList(),
                    userMangas = userMangas.illusts.toImmutableList(),
                    userBookmarksNovels = initialFilteredNovels.toImmutableList(),
                    userBookmarksIllusts = userBookmarksIllusts.illusts.toImmutableList(),
                    userInfo = userInfo
                )
            }
            originalUserBookmarksNovels.value = userBookmarksNovels.novels
        }
    }

    private fun observeNovelBookmarkFilterSettings() {
        viewModelScope.launch {
            combine(
                originalUserBookmarksNovels.filterNotNull(),
                SettingRepository.userPreferenceFlow
                    .map { it.browsingSettings }
                    .distinctUntilChanged { previous, current ->
                        !previous.hasDifferentNovelFilterSettings(current)
                    },
            ) { novels, browsingSettings ->
                novels.filterBlockedTags(browsingSettings).toImmutableList()
            }.collect { filteredNovels ->
                updateState {
                    copy(userBookmarksNovels = filteredNovels)
                }
            }
        }
    }

    fun followUser(userId: Long, restrict: Restrict = Restrict.PUBLIC) {
        FollowState.followUser(userId, restrict)
    }

    fun blockUser(userId: Long) {
        BlockingRepositoryV2.blockUser(userId = userId, name = uiState.value.userInfo.user.name)
    }

    fun removeBlockUser(userId: Long) {
        BlockingRepositoryV2.removeBlockUser(userId)
    }
}
