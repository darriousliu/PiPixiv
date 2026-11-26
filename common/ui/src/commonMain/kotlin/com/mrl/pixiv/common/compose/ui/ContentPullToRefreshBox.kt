package com.mrl.pixiv.common.compose.ui

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/** 空列表也显示下拉手势进度，松手触发刷新后由页面展示加载状态。 */
@Composable
fun ContentPullToRefreshBox(
    isRefreshing: Boolean,
    hasContent: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    state: PullToRefreshState = rememberPullToRefreshState(),
    content: @Composable BoxScope.() -> Unit,
) {
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier,
        state = state,
        indicator = {
            if (!isRefreshing && state.distanceFraction <= 0f) return@PullToRefreshBox

            // 空页加载结束后，等下拉位移归零再显示下一次手势，避免回弹时闪出指示器。
            var hideEmptyRefresh by remember(state) { mutableStateOf(false) }
            if (isRefreshing && !hasContent) {
                SideEffect { hideEmptyRefresh = true }
            } else if (hasContent || !hideEmptyRefresh) {
                PullToRefreshDefaults.LoadingIndicator(
                    state = state,
                    isRefreshing = isRefreshing,
                    modifier = Modifier.align(Alignment.TopCenter),
                )
            }
        },
        content = content,
    )
}
