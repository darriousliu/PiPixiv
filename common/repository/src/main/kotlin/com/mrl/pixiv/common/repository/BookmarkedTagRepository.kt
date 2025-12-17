package com.mrl.pixiv.common.repository

import com.mrl.pixiv.common.data.Tag
import com.mrl.pixiv.common.mmkv.MMKVUser
import com.mrl.pixiv.common.mmkv.asMutableStateFlow
import com.mrl.pixiv.common.mmkv.mmkvSerializable
import kotlinx.coroutines.flow.StateFlow

object BookmarkedTagRepository : MMKVUser {
    private val bookmarkedTagsDelegate by mmkvSerializable<List<Tag>>(emptyList()).asMutableStateFlow()
    val bookmarkedTags: StateFlow<List<Tag>> = bookmarkedTagsDelegate

    fun addTag(tag: Tag) {
        val current = bookmarkedTagsDelegate.value.toMutableList()
        if (current.none { it.name == tag.name }) {
            current.add(0, tag)
            bookmarkedTagsDelegate.value = current
        }
    }

    fun removeTag(tag: Tag) {
        val current = bookmarkedTagsDelegate.value.toMutableList()
        current.removeAll { it.name == tag.name }
        bookmarkedTagsDelegate.value = current
    }

    fun isBookmarked(tag: Tag): Boolean {
        return bookmarkedTagsDelegate.value.any { it.name == tag.name }
    }

    fun restore(tags: List<Tag>) {
        bookmarkedTagsDelegate.value = tags
    }
}