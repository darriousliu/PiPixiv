package com.mrl.pixiv.common.compose.ui

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/** An empty page owns its loading state, including refreshes started by a pull gesture. */
@Composable
fun ContentPullToRefreshBox(
    isRefreshing: Boolean,
    hasContent: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    state: PullToRefreshState = rememberPullToRefreshState(),
    content: @Composable BoxScope.() -> Unit,
) {
    val showRefresh = isRefreshing && hasContent
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier,
        state = state,
        indicator = {
            if (hasContent && (showRefresh || state.distanceFraction > 0f)) {
                PullToRefreshDefaults.LoadingIndicator(
                    state = state,
                    isRefreshing = showRefresh,
                    modifier = Modifier.align(Alignment.TopCenter),
                )
            }
        },
        content = content,
    )
}
