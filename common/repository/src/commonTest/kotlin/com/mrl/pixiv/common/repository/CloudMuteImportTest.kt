package com.mrl.pixiv.common.repository

import com.mrl.pixiv.common.data.Tag
import com.mrl.pixiv.common.data.User
import com.mrl.pixiv.common.data.mute.ForText
import com.mrl.pixiv.common.data.mute.MutedResp
import com.mrl.pixiv.common.data.mute.MutedTag
import com.mrl.pixiv.common.data.mute.MutedUser
import com.mrl.pixiv.common.datasource.local.entity.BlockTagEntity
import com.mrl.pixiv.common.datasource.local.entity.BlockUserEntity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class CloudMuteImportTest {
    @Test
    fun importPreservesLocalRegexAndNamesWithoutDuplicatingRemoteEntries() {
        val remote = muted(
            users = listOf(User(id = 1, name = "remote name"), User(id = 2), User(id = 2), User(id = 0)),
            tags = listOf("existing.*", "new", "new", "  new  ", " "),
        )
        val plan = planCloudMuteImport(
            remote,
            localUsers = listOf(BlockUserEntity(userId = 1, name = "local name")),
            localTags = listOf(BlockTagEntity(tag = "existing.*", isRegex = true)),
        )
        assertEquals(listOf(2L), plan.users.map { it.userId })
        assertEquals(listOf("new"), plan.tags.map { it.tag })
        assertFalse(plan.tags.single().isRegex)
        assertEquals(2, plan.count)
    }

    @Test
    fun importingAgainProducesNoWrites() {
        val remote = muted(users = listOf(User(id = 2)), tags = listOf("tag"))
        val first = planCloudMuteImport(remote, emptyList(), emptyList())
        val second = planCloudMuteImport(remote, first.users, first.tags)
        assertEquals(0, second.count)
    }

    private fun muted(users: List<User>, tags: List<String>) = MutedResp(
        forText = ForText(1, 500), muteLimitCount = 500, mutedCount = (users.size + tags.size).toLong(),
        mutedTags = tags.map { MutedTag(false, Tag(it)) }, mutedTagsCount = tags.size.toLong(),
        mutedUsers = users.map { MutedUser(false, it) }, mutedUsersCount = users.size.toLong(),
    )
}
