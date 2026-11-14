package com.mrl.pixiv.novel

import com.mrl.pixiv.common.data.novel.NovelImage
import com.mrl.pixiv.common.data.novel.NovelRating
import com.mrl.pixiv.common.data.novel.NovelTextResp
import com.mrl.pixiv.common.data.novel.NovelUrls
import kotlin.test.Test
import kotlin.test.assertEquals

class NovelSpanParserTest {
    @Test
    fun uploadedImageKeepsThumbnailInReaderAndUsesOriginalInPreview() {
        val original = "https://i.pximg.net/original.png?token=A%2FB"

        assertEquals(
            NovelSpanData.UploadedImage(ThumbnailUrl, original),
            parseUploadedImage(
                NovelUrls(the128X128 = ThumbnailUrl, the1200X1200 = LargeUrl, original = original),
            ),
        )
    }

    @Test
    fun uploadedImageFallsBackToLargeThenThumbnailWhenBetterUrlsAreInvalid() {
        assertEquals(
            NovelSpanData.UploadedImage(LargeUrl),
            parseUploadedImage(
                NovelUrls(
                    the128X128 = " ",
                    the1200X1200 = " HTTPS://i.pximg.net/large.png ",
                    original = "https:///image.png",
                ),
            ),
        )
        assertEquals(
            NovelSpanData.UploadedImage(ThumbnailUrl),
            parseUploadedImage(NovelUrls(the128X128 = ThumbnailUrl, original = "file:///image.png")),
        )
    }

    @Test
    fun uploadedImageWithoutAUsableUrlRemainsPlainText() {
        assertEquals(
            NovelSpanData.Text("42"),
            parseUploadedImage(NovelUrls(the128X128 = " ", original = "//i.pximg.net/image.png")),
        )
    }

    private fun parseUploadedImage(urls: NovelUrls): NovelSpanData {
        val token = "[uploadedimage:42]"
        val response = NovelTextResp(
            id = "1",
            title = "Novel",
            userId = "2",
            coverUrl = "",
            tags = emptyList(),
            caption = "",
            cdate = "",
            rating = NovelRating(0, 0, 0),
            text = token,
            images = mapOf("42" to NovelImage(sl = "", urls = urls)),
        )
        return NovelSpanParser.buildSpans(token, response).single()
    }

    private companion object {
        const val ThumbnailUrl = "https://i.pximg.net/thumbnail.png"
        const val LargeUrl = "https://i.pximg.net/large.png"
    }
}
