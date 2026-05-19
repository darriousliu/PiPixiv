@file:Suppress("NOTHING_TO_INLINE")

package com.mrl.pixiv.common.repository.util

import com.mrl.pixiv.common.data.Illust
import com.mrl.pixiv.common.data.Novel
import com.mrl.pixiv.common.data.XRestrict
import com.mrl.pixiv.common.data.comment.Comment
import com.mrl.pixiv.common.repository.BlockingRepositoryV2
import kotlin.jvm.JvmName

inline fun List<Illust>.filterNormalIllust() = filter { it.xRestrict == XRestrict.Normal }

inline fun List<Novel>.filterNormalNovel() = filter { it.xRestrict == XRestrict.Normal }

@JvmName("filterBlockedTagsIllust")
inline fun List<Illust>.filterBlockedTags(): List<Illust> {
    return filterNot { illust ->
        illust.tags.orEmpty().any { tag ->
            val isTagBlocked = BlockingRepositoryV2.isTagBlocked(tag.name)
            val isTranslatedTagBlocked = tag.translatedName.isNotBlank() &&
                    BlockingRepositoryV2.isTagBlocked(tag.translatedName)
            isTagBlocked || isTranslatedTagBlocked
        }
    }
}

@JvmName("filterBlockedTagsNovel")
inline fun List<Novel>.filterBlockedTags(): List<Novel> {
    return filterNot { novel ->
        novel.tags.any { tag ->
            val isTagBlocked = BlockingRepositoryV2.isTagBlocked(
                tag.name,
                allowKeywordMatch = true
            )
            val isTranslatedTagBlocked = tag.translatedName.isNotBlank() &&
                    BlockingRepositoryV2.isTagBlocked(
                        tag.translatedName,
                        allowKeywordMatch = true
                    )
            isTagBlocked || isTranslatedTagBlocked
        }
    }
}

inline fun List<Comment>.filterBlocked(): List<Comment> {
    return filter { comment -> !BlockingRepositoryV2.isCommentBlocked(comment.id) }
}
