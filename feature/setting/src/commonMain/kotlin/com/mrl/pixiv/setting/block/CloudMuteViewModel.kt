package com.mrl.pixiv.setting.block

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mrl.pixiv.common.data.mute.MutedResp
import com.mrl.pixiv.common.repository.PixivRepository
import com.mrl.pixiv.common.repository.importCloudMutesToLocal
import com.mrl.pixiv.common.repository.requireUserInfoFlow
import com.mrl.pixiv.common.repository.requireUserInfoValue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel

enum class CloudMuteInputKind { Tag, User }

fun canAddCloudMute(data: MutedResp?, kind: CloudMuteInputKind, input: String): Boolean {
    if (data == null || data.mutedCount >= data.muteLimitCount) return false
    val value = input.trim()
    return when (kind) {
        CloudMuteInputKind.Tag -> value.isNotEmpty() && data.mutedTags.none { it.tag.name == value }
        CloudMuteInputKind.User -> value.toLongOrNull()?.let { id ->
            id > 0 && data.mutedUsers.none { it.user.id == id }
        } == true
    }
}

data class CloudMuteState(
    val data: MutedResp? = null,
    val busy: Boolean = false,
    val failed: Boolean = false,
    val savedRefreshFailed: Boolean = false,
    val importedCount: Int? = null,
)

@KoinViewModel
class CloudMuteViewModel : ViewModel() {
    private val mutableState = MutableStateFlow(CloudMuteState())
    val state = mutableState.asStateFlow()
    private var operation: Job? = null
    private var generation = 0L
    private var accountId = requireUserInfoValue.user.id

    init {
        viewModelScope.launch {
            requireUserInfoFlow.map { it.user.id }.distinctUntilChanged().collect { id ->
                generation++
                operation?.cancel()
                accountId = id
                mutableState.value = CloudMuteState()
                if (id > 0) refresh()
            }
        }
    }

    fun refresh() = execute {
        CloudMuteState(data = PixivRepository.getMuteList())
    }

    fun add(kind: CloudMuteInputKind, input: String) {
        if (!canAddCloudMute(state.value.data, kind, input)) return
        val value = input.trim()
        edit {
            when (kind) {
                CloudMuteInputKind.Tag -> PixivRepository.postMuteSetting(addTags = listOf(value))
                CloudMuteInputKind.User -> PixivRepository.postMuteSetting(addUserIds = listOf(value.toLong()))
            }
        }
    }

    fun removeUser(userId: Long) = edit {
        PixivRepository.postMuteSetting(deleteUserIds = listOf(userId))
    }

    fun removeTag(tag: String) = edit {
        PixivRepository.postMuteSetting(deleteTags = listOf(tag))
    }

    fun importToLocal() {
        val data = state.value.data ?: return
        execute { CloudMuteState(data = data, importedCount = importCloudMutesToLocal(data)) }
    }

    private fun edit(change: suspend () -> Unit) {
        if (state.value.data == null) return
        execute {
            change()
            try {
                CloudMuteState(data = PixivRepository.getMuteList())
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                // The write succeeded. Clear stale data and offer a read-only retry.
                CloudMuteState(failed = true, savedRefreshFailed = true)
            }
        }
    }

    private fun execute(load: suspend () -> CloudMuteState) {
        if (state.value.busy || accountId <= 0) return
        val requestGeneration = ++generation
        val requestAccount = accountId
        mutableState.update { it.copy(busy = true, failed = false, importedCount = null) }
        operation = viewModelScope.launch {
            try {
                val next = load()
                if (generation == requestGeneration && accountId == requestAccount) mutableState.value = next
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                if (generation == requestGeneration) mutableState.update { it.copy(failed = true) }
            } finally {
                if (generation == requestGeneration) mutableState.update { it.copy(busy = false) }
            }
        }
    }
}
