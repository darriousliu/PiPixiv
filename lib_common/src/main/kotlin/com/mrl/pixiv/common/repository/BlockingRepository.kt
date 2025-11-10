package com.mrl.pixiv.common.repository

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mrl.pixiv.common.mmkv.MMKVUser
import com.mrl.pixiv.common.mmkv.asMutableStateFlow
import com.mrl.pixiv.common.mmkv.mmkvStringSet
import kotlinx.coroutines.flow.asStateFlow

object BlockingRepository : MMKVUser {
    private val blockIllusts by mmkvStringSet(emptySet()).asMutableStateFlow()
    val blockIllustsFlow = blockIllusts.asStateFlow()

    private val blockUsers by mmkvStringSet(emptySet()).asMutableStateFlow()
    val blockUsersFlow = blockUsers.asStateFlow()

    fun blockIllust(illustId: Long) {
        blockIllusts.value = (blockIllusts.value ?: emptySet()) + illustId.toString()
    }

    fun removeBlockIllust(illustId: Long) {
        blockIllusts.value = blockIllusts.value?.minus(illustId.toString())
    }

    fun blockUser(userId: Long) {
        blockUsers.value = (blockUsers.value ?: emptySet()) + userId.toString()
    }

    fun removeBlockUser(userId: Long) {
        blockUsers.value = blockUsers.value?.minus(userId.toString())
    }

    @Composable
    fun collectIllustBlockAsState(illustId: Long): Boolean {
        val blockingIllusts by blockIllustsFlow.collectAsStateWithLifecycle()
        return blockingIllusts?.contains(illustId.toString()) ?: false
    }

    @Composable
    fun collectUserBlockAsState(userId: Long): Boolean {
        val blockingUsers by blockUsersFlow.collectAsStateWithLifecycle()
        return blockingUsers?.contains(userId.toString()) ?: false
    }
}