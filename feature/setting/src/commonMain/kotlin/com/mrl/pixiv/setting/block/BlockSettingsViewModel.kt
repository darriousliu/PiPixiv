package com.mrl.pixiv.setting.block

import androidx.compose.runtime.Stable
import com.mrl.pixiv.common.coroutine.withUIContext
import com.mrl.pixiv.common.data.mute.MutedTag
import com.mrl.pixiv.common.data.mute.MutedUser
import com.mrl.pixiv.common.repository.BlockingRepositoryV2
import com.mrl.pixiv.common.repository.PixivRepository
import com.mrl.pixiv.common.viewmodel.BaseMviViewModel
import com.mrl.pixiv.common.viewmodel.ViewIntent
import com.mrl.pixiv.common.viewmodel.state
import org.koin.android.annotation.KoinViewModel

@Stable
data class BlockSettingsState(
    val allMutedTags: List<MutedTag> = emptyList(),
    val allMutedUsers: List<MutedUser> = emptyList(),
    val toEditBlockTag: List<String> = emptyList(),
    val toEditBlockUser: List<Long> = emptyList(),
    val loading: Boolean = false,
)

@KoinViewModel
class BlockSettingsViewModel : BaseMviViewModel<BlockSettingsState, ViewIntent>(
    initialState = BlockSettingsState()
) {
    init {
        loadMuteList()
    }

    fun loadMuteList() {
        launchIO {
            updateState { copy(loading = true) }
            val resp = PixivRepository.getMuteList()
            BlockingRepositoryV2.blockUserList(resp.mutedUsers.map { it.user.id })
            updateState {
                copy(
                    allMutedTags = resp.mutedTags,
                    allMutedUsers = resp.mutedUsers,
                    loading = false,
                )
            }
        }
    }

    fun addMutedTag(tag: String) {
        updateState {
            copy(toEditBlockTag = toEditBlockTag + tag)
        }
    }

    fun removeMutedTag(tag: String) {
        updateState {
            copy(toEditBlockTag = toEditBlockTag - tag)
        }
    }

    fun addMutedUser(userId: Long) {
        updateState {
            copy(toEditBlockUser = toEditBlockUser + userId)
        }
    }

    fun removeMutedUser(userId: Long) {
        updateState {
            copy(toEditBlockUser = toEditBlockUser - userId)
        }
    }

    fun editMuteList(onSuccess: () -> Unit) {
        launchIO {
            PixivRepository.postMuteSetting(
                deleteTags = state.toEditBlockTag,
                deleteUserIds = state.toEditBlockUser,
            )
            BlockingRepositoryV2.removeBlockUserList(state.toEditBlockUser)
            withUIContext {
                onSuccess()
            }
        }
    }

    override suspend fun handleIntent(intent: ViewIntent) {

    }
}