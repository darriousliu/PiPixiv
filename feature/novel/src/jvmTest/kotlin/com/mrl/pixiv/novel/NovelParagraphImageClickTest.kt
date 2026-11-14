package com.mrl.pixiv.novel

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.click
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import coil3.ColorImage
import coil3.annotation.ExperimentalCoilApi
import coil3.compose.AsyncImagePreviewHandler
import coil3.compose.LocalAsyncImagePreviewHandler
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalTestApi::class, ExperimentalCoilApi::class)
class NovelParagraphImageClickTest {
    @Test
    fun uploadedAndPixivImagesOpenTheirOwnDestinations() = runDesktopComposeUiTest(
        width = 360,
        height = 500,
        testTimeout = 30.seconds,
    ) {
        val uploaded = NovelSpanData.UploadedImage(
            url = "https://i.pximg.net/novel-cover-min/sample.jpg",
            previewUrl = "https://i.pximg.net/novel-cover-original/sample.jpg",
        )
        val pixiv = NovelSpanData.PixivImage(
            illustId = 42L,
            targetIndex = 0,
            token = "[pixivimage:42]",
            imageUrl = "https://i.pximg.net/img-original/sample.jpg",
        )
        val previewUrls = mutableListOf<String>()
        val illustIds = mutableListOf<Long>()
        val imageHandler = AsyncImagePreviewHandler { _ ->
            ColorImage(color = 0xFF227766.toInt(), width = 160, height = 90)
        }
        val uriHandler = object : UriHandler {
            override fun openUri(uri: String) = error("Image click opened an external URI: $uri")
        }

        setContent {
            CompositionLocalProvider(
                LocalInspectionMode provides true,
                LocalAsyncImagePreviewHandler provides imageHandler,
                LocalUriHandler provides uriHandler,
            ) {
                MaterialTheme {
                    Column {
                        listOf(uploaded, pixiv).forEachIndexed { index, span ->
                            NovelParagraph(
                                paragraphIndex = index,
                                fontSize = 16,
                                lineSpacingSp = 4,
                                span = span,
                                onParagraphTextLayout = { _, _ -> },
                                onContentClick = { error("Image click reached the paragraph") },
                                onPixivImageClick = { illustIds += it },
                                onUploadedImageClick = { previewUrls += it },
                            )
                        }
                    }
                }
            }
        }

        onNodeWithContentDescription(uploaded.url, useUnmergedTree = true)
            .performTouchInput { click() }
        onNodeWithContentDescription(pixiv.token, useUnmergedTree = true)
            .performMouseInput { click() }

        runOnIdle {
            assertEquals(listOf(uploaded.previewUrl), previewUrls)
            assertEquals(listOf(pixiv.illustId), illustIds)
        }
    }
}
