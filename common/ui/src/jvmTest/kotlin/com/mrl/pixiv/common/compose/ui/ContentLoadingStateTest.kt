package com.mrl.pixiv.common.compose.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import androidx.paging.CombinedLoadStates
import androidx.paging.LoadState
import androidx.paging.LoadStates
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalTestApi::class)
class ContentLoadingStateTest {
    @Test
    fun pullingDuringInitialLoadDoesNotRestartTheRequest() = runDesktopComposeUiTest(
        width = 390,
        height = 640,
        testTimeout = 30.seconds,
    ) {
        var refreshes = 0
        setContent {
            MaterialExpressiveTheme {
                ContentPullToRefreshBox(
                    isRefreshing = true,
                    hasContent = false,
                    onRefresh = { refreshes++ },
                    modifier = Modifier.fillMaxSize(),
                ) {
                    LazyColumn(Modifier.fillMaxSize().testTag("loading-list")) {
                        item { ContentLoadingState() }
                    }
                }
            }
        }
        repeat(2) {
            onNodeWithTag("loading-list").performTouchInput { swipeDown() }
        }
        runOnIdle { assertEquals(0, refreshes) }
        onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo))
            .assertCountEquals(1)
    }

    @Test
    fun onlyOneProgressIndicatorIsShownAcrossInitialRefreshAndAppendLoading() =
        runDesktopComposeUiTest(
            width = 390,
            height = 640,
            testTimeout = 30.seconds,
        ) {
            var count by mutableStateOf(0)
            var loads by mutableStateOf(states(LoadState.Loading, LoadState.Loading))
            setContent {
                MaterialExpressiveTheme {
                    ContentPullToRefreshBox(
                        isRefreshing = loads.refresh is LoadState.Loading,
                        hasContent = count > 0,
                        onRefresh = {},
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        if (count > 0) Text(
                            "Existing content",
                            Modifier.testTag("retained-content")
                        )
                        PagingContentState(loads, count, {})
                        Box(Modifier.align(Alignment.BottomCenter)) {
                            PagingAppendState(loads, count, {})
                        }
                    }
                }
            }
            val progress = SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo)
            onAllNodes(progress).assertCountEquals(1)
            runOnIdle { count = 12 }
            onNodeWithTag("retained-content").assertExists()
            onAllNodes(progress).assertCountEquals(1)
            runOnIdle { loads = states(idle, LoadState.Loading) }
            onAllNodes(progress).assertCountEquals(1)
            runOnIdle { loads = states(idle, idle) }
            onAllNodes(progress).assertCountEquals(0)
        }

    @Test
    fun refreshFailureKeepsContentAndExposesASingleWorkingRetry() = runDesktopComposeUiTest(
        width = 320,
        height = 640,
        testTimeout = 30.seconds,
    ) {
        var retries = 0
        val failure = LoadState.Error(IllegalStateException("offline"))
        val loads = states(failure, failure)
        setContent {
            MaterialExpressiveTheme {
                Box(Modifier.fillMaxSize()) {
                    Text("Existing content", Modifier.testTag("retained-content"))
                    PagingContentState(loads, 12, { retries++ })
                    PagingAppendState(loads, 12, { retries++ })
                }
            }
        }
        onNodeWithTag("retained-content").assertExists()
        onAllNodes(hasClickAction()).assertCountEquals(1)
        onAllNodes(hasClickAction()).onFirst().performClick()
        runOnIdle { assertEquals(1, retries) }
    }

    private fun states(refresh: LoadState, append: LoadState): CombinedLoadStates {
        return CombinedLoadStates(refresh, idle, append, LoadStates(refresh, idle, append))
    }

    private companion object {
        val idle = LoadState.NotLoading(endOfPaginationReached = true)
    }
}
