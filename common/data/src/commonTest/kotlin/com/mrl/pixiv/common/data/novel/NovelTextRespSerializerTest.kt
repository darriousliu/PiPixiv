package com.mrl.pixiv.common.data.novel

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class NovelTextRespSerializerTest {
    private val json = Json {
        ignoreUnknownKeys = true
    }

    @Test
    fun configuredJsonIsUsedForNestedIllustrationsAndImages() {
        val response = json.decodeFromString<NovelTextResp>(
            novelJson(
                illusts = """
                    {
                      "1": {
                        "unknown_wrapper_field": true,
                        "illust": {
                          "unknown_illust_field": true,
                          "images": {
                            "small": "small.jpg",
                            "medium": "medium.jpg",
                            "unknown_image_field": true
                          }
                        }
                      }
                    }
                """.trimIndent(),
                images = """
                    {
                      "2": {
                        "novelImageId": "2",
                        "sl": "private",
                        "unknown_image_field": true,
                        "urls": {
                          "240mw": "240.jpg",
                          "unknown_url_field": true
                        }
                      }
                    }
                """.trimIndent(),
            )
        )

        assertEquals("small.jpg", response.illusts?.get("1")?.illust?.images?.small)
        assertEquals("240.jpg", response.images?.get("2")?.urls?.the240Mw)
    }

    @Test
    fun nullAndArrayShapesRemainSupported() {
        val nullResponse = json.decodeFromString<NovelTextResp>(
            novelJson(illusts = "null", images = "null")
        )
        val arrayResponse = json.decodeFromString<NovelTextResp>(
            novelJson(illusts = "[]", images = "[]")
        )

        assertNull(nullResponse.illusts)
        assertNull(nullResponse.images)
        assertNull(arrayResponse.illusts)
        assertNull(arrayResponse.images)
    }

    @Test
    fun nullIllustrationEntryIsPreserved() {
        val response = json.decodeFromString<NovelTextResp>(
            novelJson(
                illusts = """{"1":{"illust":null}}""",
                images = "{}",
            )
        )

        assertEquals(mapOf("1" to null), response.illusts)
    }

    private fun novelJson(
        illusts: String,
        images: String,
    ) = """
        {
          "id": "100",
          "title": "title",
          "userId": "10",
          "coverUrl": "cover.jpg",
          "tags": [],
          "caption": "",
          "cdate": "",
          "rating": {"like": 0, "bookmark": 0, "view": 0},
          "text": "body",
          "illusts": $illusts,
          "images": $images,
          "unknown_root_field": true
        }
    """.trimIndent()
}
