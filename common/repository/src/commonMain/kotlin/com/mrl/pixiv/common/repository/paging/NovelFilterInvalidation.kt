package com.mrl.pixiv.common.repository.paging

import androidx.paging.PagingSource
import com.mrl.pixiv.common.repository.NovelFilterSettingsChanges

internal fun PagingSource<*, *>.invalidateOnNovelFilterSettingsChanges() {
    val unregister = NovelFilterSettingsChanges.register(::invalidate)
    registerInvalidatedCallback(unregister)
}
