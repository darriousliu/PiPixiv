package com.mrl.pixiv.novel.series

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.mrl.pixiv.common.repository.NovelSeriesReadingProgress
import java.io.File
import java.util.Locale
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalTestApi::class)
class NovelSeriesContinueReadingCardTest {
    @Test
    fun longChapterTitleKeepsTheActionCompactAndClickable() = checkCard(
        locale = Locale.SIMPLIFIED_CHINESE,
        label = "继续阅读",
        fontScale = 1f,
        maximumHeight = 128f,
    )

    @Test
    fun largeTextKeepsTheProgressVisibleAlongsideALongTranslation() = checkCard(
        locale = Locale.forLanguageTag("ru"),
        label = "Продолжить чтение",
        fontScale = 2f,
        maximumHeight = 240f,
    )

    private fun checkCard(locale: Locale, label: String, fontScale: Float, maximumHeight: Float) {
        val previousLocale = Locale.getDefault()
        Locale.setDefault(locale)
        try {
            runDesktopComposeUiTest(width = 320, height = 400, testTimeout = 30.seconds) {
                val title = "很长的章节标题，需要保留阅读进度并限制标题行数。".repeat(12)
                var clicks = 0
                setContent {
                    CompositionLocalProvider(LocalDensity provides Density(1f, fontScale)) {
                        MaterialTheme {
                            Surface(Modifier.fillMaxSize()) {
                                Box(Modifier.fillMaxSize().padding(12.dp)) {
                                    NovelSeriesContinueReadingCard(
                                        progress = NovelSeriesReadingProgress(7, title, 0.42f),
                                        onClick = { clicks++ },
                                        modifier = Modifier.fillMaxWidth().testTag("continue-reading"),
                                    )
                                }
                            }
                        }
                    }
                }
                val card = onNodeWithTag("continue-reading")
                // 使用虚构章节保存组件预览，不包含账号或真实阅读记录。
                val directory = File("build/reports/novel-series-preview").apply { mkdirs() }
                ImageIO.write(
                    captureToImage().toAwtImage(), "png",
                    File(directory, "continue-${locale.language}-$fontScale.png"),
                )
                val action = onNodeWithText(label, useUnmergedTree = true)
                val percent = onNodeWithText("42%", useUnmergedTree = true)
                action.assertIsDisplayed()
                percent.assertIsDisplayed()
                onNodeWithText("42%%", useUnmergedTree = true).assertDoesNotExist()
                onNodeWithText(title, useUnmergedTree = true)
                    .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { getLayout ->
                        val layouts = mutableListOf<TextLayoutResult>()
                        assertTrue(getLayout(layouts))
                        assertEquals(2, layouts.single().lineCount)
                        assertTrue(layouts.single().getLineEnd(1) < title.length)
                    }
                val cardBounds = card.fetchSemanticsNode().boundsInRoot
                val actionBounds = action.fetchSemanticsNode().boundsInRoot
                val percentBounds = percent.fetchSemanticsNode().boundsInRoot
                assertTrue(cardBounds.height <= maximumHeight)
                assertTrue(actionBounds.right <= percentBounds.left)
                assertTrue(percentBounds.right <= cardBounds.right)
                card.performClick()
                runOnIdle { assertEquals(1, clicks) }
            }
        } finally {
            Locale.setDefault(previousLocale)
        }
    }
}
