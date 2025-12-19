package com.mrl.pixiv.common.repository

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mrl.pixiv.common.data.comment.Comment
import com.mrl.pixiv.common.mmkv.MMKVOwner
import com.mrl.pixiv.common.mmkv.asMutableStateFlow
import com.mrl.pixiv.common.mmkv.mmkvSerializable
import com.mrl.pixiv.common.mmkv.mmkvStringSet
import kotlinx.coroutines.flow.asStateFlow

object BlockingRepositoryV2 : MMKVOwner {
    override val id: String = "block_content"

    private val blockIllusts by mmkvStringSet(emptySet()).asMutableStateFlow()
    val blockIllustsFlow = blockIllusts.asStateFlow()

    private val blockUsers by mmkvStringSet(emptySet()).asMutableStateFlow()
    val blockUsersFlow = blockUsers.asStateFlow()

    private val blockComments by mmkvSerializable<List<Comment>>(emptyList()).asMutableStateFlow()
    val blockCommentsFlow = blockComments.asStateFlow()

    fun blockIllust(illustId: Long) {
        blockIllusts.value = (blockIllusts.value ?: emptySet()) + illustId.toString()
    }

    fun removeBlockIllust(illustId: Long) {
        blockIllusts.value = blockIllusts.value?.minus(illustId.toString())
    }

    fun blockUser(userId: Long) {
        blockUsers.value = (blockUsers.value ?: emptySet()) + userId.toString()
    }

    fun blockUserList(userIds: List<Long>) {
        blockUsers.value = (blockUsers.value ?: emptySet()) + userIds.map { it.toString() }.toSet()
    }

    fun removeBlockUser(userId: Long) {
        blockUsers.value = blockUsers.value?.minus(userId.toString())
    }

    fun removeBlockUserList(userIds: List<Long>) {
        blockUsers.value = blockUsers.value?.minus(userIds.map { it.toString() }.toSet())
    }

    fun blockComment(comment: Comment) {
        blockComments.value += comment
    }

    fun removeBlockComment(commentId: Long) {
        blockComments.value = blockComments.value.filter { it.id != commentId }
    }

    fun migrate() {
        restore(
            illusts = BlockingRepository.blockIllustsFlow.value.orEmpty(),
            users = BlockingRepository.blockUsersFlow.value.orEmpty(),
            comments = emptyList()
        )
    }

    fun restore(illusts: Set<String>, users: Set<String>, comments: List<Comment>) {
        blockIllusts.value = illusts
        blockUsers.value = users
        blockComments.value = comments
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

    @Composable
    fun collectCommentBlockAsState(commentId: Long): Boolean {
        val blockingComments by blockCommentsFlow.collectAsStateWithLifecycle()
        return blockingComments.find { it.id == commentId } != null
    }
}