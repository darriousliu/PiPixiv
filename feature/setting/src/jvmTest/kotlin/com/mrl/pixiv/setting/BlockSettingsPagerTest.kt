package com.mrl.pixiv.setting

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.Text
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import com.mrl.pixiv.setting.block.BlockCategory
import com.mrl.pixiv.setting.block.BlockSettingsContent
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalTestApi::class)
class BlockSettingsPagerTest {
    @Test
    fun swipesAndTabJumpsKeepTheSelectedCategoryAndScrollPosition() {
        val previousLocale = Locale.getDefault()
        Locale.setDefault(Locale.SIMPLIFIED_CHINESE)
        try {
            runDesktopComposeUiTest(width = 390, height = 700, testTimeout = 30.seconds) {
                val changes = mutableListOf<BlockCategory>()
                setContent {
                    MaterialExpressiveTheme {
                        BlockSettingsContent(
                            onCategoryChange = { changes += it },
                            onBack = {}) { category, modifier ->
                            LazyColumn(
                                state = rememberLazyListState(),
                                modifier = modifier.testTag("page:${category.name}"),
                            ) {
                                items(60) { index -> Text("${category.name}:$index") }
                            }
                        }
                    }
                }
                runOnIdle { assertEquals(emptyList(), changes) }
                onNodeWithTag("page:Cloud").performScrollToIndex(25)
                onNodeWithTag("page:Cloud").performTouchInput { swipeLeft() }
                onNodeWithTag("page:Illust").assertIsDisplayed()
                onNodeWithText("屏蔽插画").assertIsSelected()

                onNodeWithText("屏蔽的评论").performScrollTo().performClick()
                onNodeWithTag("page:Comment").performTouchInput { swipeRight() }
                onNodeWithTag("page:Tag").assertIsDisplayed()
                onNodeWithText("屏蔽标签").assertIsSelected()

                onNodeWithText("Pixiv 云端屏蔽").performScrollTo().performClick()
                onNodeWithText("Cloud:25").assertIsDisplayed()
                onNodeWithText("Cloud:0").assertDoesNotExist()
                runOnIdle {
                    assertEquals(
                        listOf(
                            BlockCategory.Illust,
                            BlockCategory.Comment,
                            BlockCategory.Tag,
                            BlockCategory.Cloud
                        ), changes
                    )
                }
            }
        } finally {
            Locale.setDefault(previousLocale)
        }
    }
}
