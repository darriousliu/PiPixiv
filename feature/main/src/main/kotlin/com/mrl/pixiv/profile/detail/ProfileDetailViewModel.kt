package com.mrl.pixiv.profile.detail

import androidx.compose.runtime.Stable
import com.mrl.pixiv.common.data.Illust
import com.mrl.pixiv.common.data.Novel
import com.mrl.pixiv.common.data.Restrict
import com.mrl.pixiv.common.data.Type
import com.mrl.pixiv.common.data.user.IllustsWithNextUrl
import com.mrl.pixiv.common.data.user.UserDetailResp
import com.mrl.pixiv.common.data.user.UserIllustsResp
import com.mrl.pixiv.common.data.user.UserNovelsResp
import com.mrl.pixiv.common.repository.BlockingRepository
import com.mrl.pixiv.common.repository.PixivRepository
import com.mrl.pixiv.common.repository.requireUserInfoValue
import com.mrl.pixiv.common.repository.viewmodel.follow.FollowState
import com.mrl.pixiv.common.viewmodel.BaseMviViewModel
import com.mrl.pixiv.common.viewmodel.ViewIntent
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import org.koin.android.annotation.KoinViewModel

@Stable
data class ProfileDetailState(
    val userTotalWorks: Int = 0,
    val userIllusts: ImmutableList<Illust> = persistentListOf(),
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
    init {
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
            val userBookmarksNovels = resp[1] as UserNovelsResp
            val userBookmarksIllusts = resp[2] as IllustsWithNextUrl
            val userInfo = resp[3] as UserDetailResp
            updateState {
                copy(
                    userIllusts = userIllusts.illusts.toImmutableList(),
                    userBookmarksNovels = userBookmarksNovels.novels.toImmutableList(),
                    userBookmarksIllusts = userBookmarksIllusts.illusts.toImmutableList(),
                    userInfo = userInfo
                )
            }
        }
    }

    fun followUser(userId: Long, restrict: Restrict = Restrict.PUBLIC) {
        FollowState.followUser(userId, restrict)
    }

    fun blockUser(userId: Long) {
        launchIO {
            async { PixivRepository.postMuteSetting(addUserIds = listOf(userId)) }
            async { BlockingRepository.blockUser(userId) }
        }
    }

    fun removeBlockUser(userId: Long) {
        launchIO {
            async { PixivRepository.postMuteSetting(deleteUserIds = listOf(userId)) }
            async { BlockingRepository.removeBlockUser(userId) }
        }
    }
}