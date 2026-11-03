package com.mrl.pixiv.profile.reading

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import com.mrl.pixiv.common.data.setting.ReadingTab
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalTestApi::class)
class ReadingContentTest {
    @Test
    fun swipesSelectPagesWithoutLosingTheirSavedState() {
        runDesktopComposeUiTest(width = 390, height = 844, testTimeout = 30.seconds) {
            val selections = mutableListOf<ReadingTab>()
            setContent {
                MaterialExpressiveTheme {
                    ReadingContent(ReadingTab.NOVEL_MARKERS, { selections += it }, {}) { tab ->
                        var count by rememberSaveable { mutableIntStateOf(0) }
                        Column(Modifier.fillMaxSize().testTag("page:${tab.name}")) {
                            Button(onClick = { count++ }) { Text("${tab.name}:$count") }
                        }
                    }
                }
            }
            val tabs = onAllNodes(isSelectable())
            onNodeWithText("NOVEL_MARKERS:0").assertIsDisplayed()
            tabs[1].assertIsSelected()
            runOnIdle { assertEquals(emptyList(), selections) }

            onNodeWithTag("page:NOVEL_MARKERS").performTouchInput { swipeLeft() }
            onNodeWithText("MANGA_WATCHLIST:0").assertIsDisplayed()
            tabs[2].assertIsSelected()
            onNodeWithTag("page:MANGA_WATCHLIST").performTouchInput { swipeRight() }
            onNodeWithText("NOVEL_MARKERS:0").assertIsDisplayed()
            tabs[1].assertIsSelected()
            runOnIdle {
                assertEquals(
                    listOf(ReadingTab.MANGA_WATCHLIST, ReadingTab.NOVEL_MARKERS),
                    selections
                )
            }

            tabs[0].performScrollTo().performClick()
            onNodeWithText("READ_LATER:0").performClick()
            tabs[2].performScrollTo().performClick()
            onNodeWithText("MANGA_WATCHLIST:0").assertIsDisplayed()
            tabs[0].performScrollTo().performClick()
            onNodeWithText("READ_LATER:1").assertIsDisplayed()
        }
    }
}
