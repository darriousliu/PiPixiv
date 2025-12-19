@file:Suppress("NOTHING_TO_INLINE")

package com.mrl.pixiv.common.repository.util

import com.mrl.pixiv.common.data.Illust
import com.mrl.pixiv.common.data.XRestrict
import com.mrl.pixiv.common.data.comment.Comment
import com.mrl.pixiv.common.repository.BlockingRepositoryV2

inline fun List<Illust>.filterNormal() = filter { it.xRestrict == XRestrict.Normal }

inline fun List<Comment>.filterBlocked() = filter { comment ->
    BlockingRepositoryV2.blockCommentsFlow.value.all { it.id != comment.id }
}