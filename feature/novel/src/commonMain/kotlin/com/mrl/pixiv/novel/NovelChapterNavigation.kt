package com.mrl.pixiv.novel

internal fun novelChapterStateKey(
    entryNovelId: Long,
    loadedNovelId: Long?,
): Long = loadedNovelId ?: entryNovelId
