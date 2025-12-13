@file:Suppress("NOTHING_TO_INLINE")

package com.mrl.pixiv.common.repository.util

import com.mrl.pixiv.common.data.Illust
import com.mrl.pixiv.common.data.XRestrict

inline fun List<Illust>.filterNormal() = filter { it.xRestrict == XRestrict.Normal }