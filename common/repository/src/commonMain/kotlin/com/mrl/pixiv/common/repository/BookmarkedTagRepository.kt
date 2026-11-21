package com.mrl.pixiv.common.repository

import com.mrl.pixiv.common.data.Tag
import com.mrl.pixiv.common.mmkv.AccountMMKVProperty
import com.mrl.pixiv.common.mmkv.AccountScoped
import com.mrl.pixiv.common.mmkv.AccountStore
import com.mrl.pixiv.common.mmkv.MMKVUser
import com.mrl.pixiv.common.mmkv.mmkvSerializable

object BookmarkedTagRepository : MMKVUser, AccountScoped {
    private val bookmarkedTagsDelegate = AccountMMKVProperty(
        mmkvSerializable<List<Tag>>(emptyList()), "bookmarkedTagsDelegate", emptyList(),
        loadOnAccess = true,
    )
    val bookmarkedTags = bookmarkedTagsDelegate.flow

    override fun reload() = bookmarkedTagsDelegate.reload(AccountStore.activeUserId)

    internal fun prefetch(userId: Long) = bookmarkedTagsDelegate.prefetch(userId)

    fun addTag(tag: Tag) {
        bookmarkedTagsDelegate.update(AccountStore.activeUserId) { current ->
            if (current.any { it.name == tag.name }) current else listOf(tag) + current
        }
    }

    fun removeTag(tag: Tag) {
        bookmarkedTagsDelegate.update(AccountStore.activeUserId) { current ->
            current.filterNot { it.name == tag.name }
        }
    }

    fun isBookmarked(tag: Tag): Boolean =
        bookmarkedTags.value.any { it.name == tag.name }

    fun tagsForAccount(userId: Long): List<Tag> = bookmarkedTagsDelegate.read(userId)

    fun restore(tags: List<Tag>, userId: Long = AccountStore.activeUserId) =
        bookmarkedTagsDelegate.write(userId, tags)
}
