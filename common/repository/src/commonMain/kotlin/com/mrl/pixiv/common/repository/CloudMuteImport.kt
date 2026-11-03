package com.mrl.pixiv.common.repository

import com.mrl.pixiv.common.data.mute.MutedResp
import com.mrl.pixiv.common.datasource.local.entity.BlockTagEntity
import com.mrl.pixiv.common.datasource.local.entity.BlockUserEntity
import kotlinx.coroutines.flow.first

data class CloudMuteImportPlan(
    val users: List<BlockUserEntity>,
    val tags: List<BlockTagEntity>,
) {
    val count: Int get() = users.size + tags.size
}

/** Import adds missing entries only, preserving local names and regular expressions. */
fun planCloudMuteImport(
    remote: MutedResp,
    localUsers: List<BlockUserEntity>,
    localTags: List<BlockTagEntity>,
): CloudMuteImportPlan {
    val existingUsers = localUsers.mapTo(mutableSetOf()) { it.userId }
    val existingTags = localTags.mapTo(mutableSetOf()) { it.tag.trim() }
    return CloudMuteImportPlan(
        users = remote.mutedUsers.map { it.user }
            .filter { it.id > 0 && existingUsers.add(it.id) }
            .map { BlockUserEntity(userId = it.id, name = it.name) },
        tags = remote.mutedTags.map { it.tag.name.trim() }
            .filter { it.isNotEmpty() && existingTags.add(it) }
            .map { BlockTagEntity(tag = it, isRegex = false) },
    )
}

suspend fun importCloudMutesToLocal(remote: MutedResp): Int {
    val plan = planCloudMuteImport(
        remote,
        BlockingRepositoryV2.blockUserItemsFlow.first(),
        BlockingRepositoryV2.blockTagItemsFlow.first(),
    )
    BlockingRepositoryV2.restore(
        illusts = emptyList(),
        users = plan.users,
        comments = emptyList(),
        tags = plan.tags,
    )
    return plan.count
}
