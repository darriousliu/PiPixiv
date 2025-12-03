package com.mrl.pixiv.setting.block

import androidx.compose.runtime.Stable
import com.mrl.pixiv.common.coroutine.withUIContext
import com.mrl.pixiv.common.data.mute.MutedTag
import com.mrl.pixiv.common.data.mute.MutedUser
import com.mrl.pixiv.common.repository.BlockingRepository
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
            val resp = PixivRepository.getMuteList()
            BlockingRepository.blockUserList(resp.mutedUsers.map { it.user.id })
            updateState {
                copy(
                    allMutedTags = resp.mutedTags,
                    allMutedUsers = resp.mutedUsers,
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
            BlockingRepository.removeBlockUserList(state.toEditBlockUser)
            withUIContext {
                onSuccess()
            }
        }
    }

    override suspend fun handleIntent(intent: ViewIntent) {

    }
}