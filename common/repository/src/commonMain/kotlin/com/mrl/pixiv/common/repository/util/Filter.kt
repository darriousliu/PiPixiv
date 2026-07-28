@file:Suppress("NOTHING_TO_INLINE")

package com.mrl.pixiv.common.repository.util

import com.mrl.pixiv.common.data.Illust
import com.mrl.pixiv.common.data.Novel
import com.mrl.pixiv.common.data.XRestrict
import com.mrl.pixiv.common.data.comment.Comment
import com.mrl.pixiv.common.data.setting.BrowsingSettings
import com.mrl.pixiv.common.repository.BlockingRepositoryV2
import com.mrl.pixiv.common.repository.requireUserPreferenceValue
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
    return filterBlockedTags(requireUserPreferenceValue.browsingSettings)
}

@JvmName("filterBlockedTagsNovelWithSettings")
inline fun List<Novel>.filterBlockedTags(
    browsingSettings: BrowsingSettings,
): List<Novel> {
    return filterNot { novel ->
        novel.hasDisallowedLongTag(browsingSettings) || novel.tags.any { tag ->
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

fun Novel.hasDisallowedLongTag(settings: BrowsingSettings): Boolean {
    return tags.asSequence()
        .map { it.name }
        .hasDisallowedLongNovelTag(settings)
}

fun Sequence<String>.hasDisallowedLongNovelTag(settings: BrowsingSettings): Boolean {
    if (!settings.filterLongNovelTags) return false

    val maxLength = settings.maxNovelTagLength.coerceAtLeast(
        BrowsingSettings.MIN_NOVEL_TAG_LIMIT
    )
    val maxSegments = settings.maxNovelTagSegments.coerceAtLeast(
        BrowsingSettings.MIN_NOVEL_TAG_LIMIT
    )
    return any { tag ->
        tag.length > maxLength ||
                tag.split(*NOVEL_TAG_SEGMENT_DELIMITERS).size > maxSegments
    }
}

private val NOVEL_TAG_SEGMENT_DELIMITERS = charArrayOf('/', '#', '、')

inline fun List<Comment>.filterBlocked(): List<Comment> {
    return filter { comment -> !BlockingRepositoryV2.isCommentBlocked(comment.id) }
}
