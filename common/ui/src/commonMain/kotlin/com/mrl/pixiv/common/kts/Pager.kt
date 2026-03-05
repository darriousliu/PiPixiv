package com.mrl.pixiv.common.kts

import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemKey

inline fun <T : Any> LazyPagingItems<T>.itemIndexKey(
    crossinline key: (index: Int, item: T) -> Any
): (index: Int) -> Any {
    return { index ->
        itemKey { key(index, it) }(index)
    }
}
