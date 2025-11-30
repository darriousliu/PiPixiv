package com.mrl.pixiv.common.util

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filter

val LazyListState.isScrollToTop: Boolean
    @Composable
    get() {
        val isTop by remember {
            derivedStateOf {
                firstVisibleItemIndex == 0 && firstVisibleItemScrollOffset == 0
            }
        }
        return isTop
    }

val LazyGridState.isScrollToTop: Boolean
    @Composable
    get() {
        val isTop by remember {
            derivedStateOf {
                firstVisibleItemIndex == 0 && firstVisibleItemScrollOffset == 0
            }
        }
        return isTop
    }

val LazyStaggeredGridState.isScrollToTop: Boolean
    @Composable
    get() {
        val isTop by remember {
            derivedStateOf {
                firstVisibleItemIndex == 0 && firstVisibleItemScrollOffset == 0
            }
        }
        return isTop
    }

val LazyListState.isScrollToBottom: Boolean
    @Composable
    get() {
        val isBottom by remember {
            derivedStateOf {
                val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull()
                    ?: return@derivedStateOf false
                lastVisibleItem.index >= layoutInfo.totalItemsCount - 1
            }
        }
        return isBottom
    }

val LazyGridState.isScrollToBottom: Boolean
    @Composable
    get() {
        val isBottom by remember {
            derivedStateOf {
                val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull()
                    ?: return@derivedStateOf false
                lastVisibleItem.index >= layoutInfo.totalItemsCount - 1
            }
        }
        return isBottom
    }

val LazyStaggeredGridState.isScrollToBottom: Boolean
    @Composable
    get() {
        val isBottom by remember {
            derivedStateOf {
                val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull()
                    ?: return@derivedStateOf false
                lastVisibleItem.index >= layoutInfo.totalItemsCount - 1
            }
        }
        return isBottom
    }

@Composable
fun LazyStaggeredGridState.OnScrollToBottom(
    loadingItemCount: Int,
    debounceTime: Long = 300,
    block: () -> Unit = {},
) {
    val updatedBlock by rememberUpdatedState(block)
    val shouldLoadMore by remember {
        derivedStateOf {
            val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull()
                ?: return@derivedStateOf false
            lastVisibleItem.index >= layoutInfo.totalItemsCount - 1 - loadingItemCount
        }
    }
    LaunchedEffect(Unit) {
        snapshotFlow { shouldLoadMore }
            .debounce(debounceTime)
            .filter { it }
            .collect {
                updatedBlock()
            }
    }
}

@Composable
fun LazyGridState.OnScrollToBottom(
    loadingItemCount: Int,
    debounceTime: Long = 300,
    block: () -> Unit = {},
) {
    val updatedBlock by rememberUpdatedState(block)
    val shouldLoadMore by remember {
        derivedStateOf {
            val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull()
                ?: return@derivedStateOf false
            lastVisibleItem.index >= layoutInfo.totalItemsCount - 1 - loadingItemCount
        }
    }
    LaunchedEffect(Unit) {
        snapshotFlow { shouldLoadMore }
            .debounce(debounceTime)
            .filter { it }
            .collect {
                updatedBlock()
            }
    }
}

@Composable
fun LazyListState.OnScrollToBottom(
    loadingItemCount: Int,
    debounceTime: Long = 300,
    block: () -> Unit = {},
) {
    val updatedBlock by rememberUpdatedState(block)
    val shouldLoadMore by remember {
        derivedStateOf {
            val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull()
                ?: return@derivedStateOf false
            lastVisibleItem.index >= layoutInfo.totalItemsCount - 1 - loadingItemCount
        }
    }
    LaunchedEffect(Unit) {
        snapshotFlow { shouldLoadMore }
            .debounce(debounceTime)
            .filter { it }
            .collect {
                updatedBlock()
            }
    }
}