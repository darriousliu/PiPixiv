@file:Suppress("NOTHING_TO_INLINE")

package com.mrl.pixiv.common.repository.util

import com.mrl.pixiv.common.data.Illust
import com.mrl.pixiv.common.data.Novel
import com.mrl.pixiv.common.data.XRestrict
import com.mrl.pixiv.common.data.comment.Comment
import com.mrl.pixiv.common.repository.BlockingRepositoryV2

inline fun List<Illust>.filterNormalIllust() = filter { it.xRestrict == XRestrict.Normal }

inline fun List<Novel>.filterNormalNovel() = filter { it.xRestrict == XRestrict.Normal }

inline fun List<Comment>.filterBlocked(): List<Comment> {
    return filter { comment -> !BlockingRepositoryV2.isCommentBlocked(comment.id) }
}
