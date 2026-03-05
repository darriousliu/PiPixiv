@file:Suppress("NOTHING_TO_INLINE")

package com.mrl.pixiv.common.kts

inline fun Int?.orEmpty(): Int = this ?: 0

inline fun Long?.orEmpty(): Long = this ?: 0L

inline fun Float?.orEmpty(): Float = this ?: 0f

inline operator fun Int?.plus(other: Int): Int = orEmpty() + other

inline operator fun Long?.plus(other: Long): Long = orEmpty() + other

inline operator fun Float?.plus(other: Float): Float = orEmpty() + other