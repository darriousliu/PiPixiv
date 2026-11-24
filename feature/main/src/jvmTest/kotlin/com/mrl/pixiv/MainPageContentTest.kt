package com.mrl.pixiv

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.mrl.pixiv.common.compose.layout.LocalPaneKeyEventsEnabled
import com.mrl.pixiv.common.router.MainPage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.MainCoroutineDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import javax.swing.SwingUtilities
import kotlin.coroutines.CoroutineContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

@OptIn(
    ExperimentalTestApi::class,
    ExperimentalSharedTransitionApi::class,
    ExperimentalCoroutinesApi::class,
)
class MainPageContentTest {
    private object SwingMainDispatcher : MainCoroutineDispatcher() {
        override val immediate: MainCoroutineDispatcher get() = this
        override fun isDispatchNeeded(context: CoroutineContext): Boolean =
            !SwingUtilities.isEventDispatchThread()

        override fun dispatch(context: CoroutineContext, block: Runnable) {
            SwingUtilities.invokeLater(block)
        }
    }

    private inline fun withSwingMain(block: () -> Unit) {
        Dispatchers.setMain(SwingMainDispatcher)
        try {
            block()
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun crossTabJumpAndRapidSwitchPreserveStateAndLifecycle() = withSwingMain {
        runDesktopComposeUiTest(width = 412, height = 915, testTimeout = 30.seconds) {
            var selectedPage by mutableStateOf<MainPage>(MainPage.Latest)
            val lifecycleOwners = mutableMapOf<MainPage, LifecycleOwner>()
            val keyEventsEnabled = mutableMapOf<MainPage, Boolean>()

            setContent {
                val parentLifecycleOwner = remember {
                    object : LifecycleOwner {
                        override val lifecycle = LifecycleRegistry(this).apply {
                            currentState = Lifecycle.State.RESUMED
                        }
                    }
                }
                SharedTransitionLayout(Modifier.fillMaxSize()) {
                    CompositionLocalProvider(
                        LocalLifecycleOwner provides parentLifecycleOwner,
                        LocalPaneKeyEventsEnabled provides true,
                    ) {
                        MainPageContent(
                            selectedPage = selectedPage,
                            usePager = true,
                            modifier = Modifier.fillMaxSize(),
                        ) { page ->
                            val lifecycleOwner = LocalLifecycleOwner.current
                            val keyEnabled = LocalPaneKeyEventsEnabled.current
                            SideEffect {
                                lifecycleOwners[page] = lifecycleOwner
                                keyEventsEnabled[page] = keyEnabled
                            }
                            var count by rememberSaveable { mutableIntStateOf(0) }
                            Scaffold { innerPadding ->
                                Box(Modifier.fillMaxSize().padding(innerPadding)) {
                                    Button(onClick = { count++ }) {
                                        Text("${page::class.simpleName}:$count")
                                    }
                                }
                            }
                        }
                    }
                }
            }

            onNodeWithText("Latest:0").assertIsDisplayed().performClick()
            onNodeWithText("Latest:1").assertIsDisplayed()
            runOnIdle {
                assertEquals(
                    Lifecycle.State.RESUMED,
                    lifecycleOwners[MainPage.Latest]?.lifecycle?.currentState,
                )
                assertEquals(true, keyEventsEnabled[MainPage.Latest])
            }

            // 覆盖跨页往返跳转（Latest -> Home -> Latest、Home -> Profile -> Home）。
            runOnIdle { selectedPage = MainPage.Home }
            onNodeWithText("Home:0").assertIsDisplayed().performClick()
            onNodeWithText("Home:1").assertIsDisplayed()

            runOnIdle { selectedPage = MainPage.Latest }
            onNodeWithText("Latest:1").assertIsDisplayed()

            runOnIdle { selectedPage = MainPage.Home }
            onNodeWithText("Home:1").assertIsDisplayed()

            runOnIdle { selectedPage = MainPage.Profile }
            onNodeWithText("Profile:0").assertIsDisplayed()

            // 覆盖动画中途连续快切多页，避免复用失活节点崩溃。
            mainClock.autoAdvance = false
            selectedPage = MainPage.Home
            repeat(3) { mainClock.advanceTimeByFrame(); waitForIdle() }
            runOnIdle {
                assertEquals(
                    Lifecycle.State.STARTED,
                    lifecycleOwners[MainPage.Home]?.lifecycle?.currentState,
                )
                assertEquals(false, keyEventsEnabled[MainPage.Home])
            }
            selectedPage = MainPage.Latest
            repeat(3) { mainClock.advanceTimeByFrame(); waitForIdle() }
            selectedPage = MainPage.Ranking
            repeat(3) { mainClock.advanceTimeByFrame(); waitForIdle() }
            selectedPage = MainPage.Profile
            repeat(3) { mainClock.advanceTimeByFrame(); waitForIdle() }
            selectedPage = MainPage.Latest
            mainClock.autoAdvance = true
            waitForIdle()

            onNodeWithText("Latest:1").assertIsDisplayed()
            runOnIdle {
                assertEquals(
                    Lifecycle.State.RESUMED,
                    lifecycleOwners[MainPage.Latest]?.lifecycle?.currentState,
                )
                assertEquals(true, keyEventsEnabled[MainPage.Latest])
            }
        }
    }

    @Test
    fun switchingBetweenCompactAndWideHostKeepsCompositionIdentity() = withSwingMain {
        runDesktopComposeUiTest(width = 412, height = 915, testTimeout = 30.seconds) {
            var selectedPage by mutableStateOf<MainPage>(MainPage.Home)
            var usePager by mutableStateOf(true)
            val createdTokens = mutableListOf<Any>()

            setContent {
                SharedTransitionLayout(Modifier.fillMaxSize()) {
                    CompositionLocalProvider(LocalPaneKeyEventsEnabled provides true) {
                        MainPageContent(
                            selectedPage = selectedPage,
                            usePager = usePager,
                            modifier = Modifier.fillMaxSize(),
                        ) { page ->
                            remember { Any().also(createdTokens::add) }
                            var count by rememberSaveable { mutableIntStateOf(0) }
                            Button(onClick = { count++ }) {
                                Text("${page::class.simpleName}:$count")
                            }
                        }
                    }
                }
            }

            onNodeWithText("Home:0").assertIsDisplayed().performClick()
            onNodeWithText("Home:1").assertIsDisplayed()
            runOnIdle { assertEquals(1, createdTokens.size) }

            runOnIdle { usePager = false }
            onNodeWithText("Home:1").assertIsDisplayed()
            runOnIdle { assertEquals(1, createdTokens.size) }

            runOnIdle { selectedPage = MainPage.Latest }
            onNodeWithText("Latest:0").assertIsDisplayed()
            runOnIdle { assertTrue(createdTokens.size == 2) }

            runOnIdle { usePager = true }
            onNodeWithText("Latest:0").assertIsDisplayed()
            runOnIdle { assertEquals(2, createdTokens.size) }
        }
    }
}
