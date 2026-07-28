package com.mrl.pixiv.common.data.novel

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class NovelMarkerSerializationTest {
    private val json = Json {
        ignoreUnknownKeys = true
    }

    @Test
    fun decodesMarkerPage() {
        val marker = json.decodeFromString<NovelMarker>(
            """{"page":7,"ignored":"value"}"""
        )

        assertEquals(7, marker.page)
    }

    @Test
    fun decodesEmptyMarkerListResponse() {
        val response = json.decodeFromString<NovelMarkersResp>(
            """{"marked_novels":[],"next_url":null}"""
        )

        assertEquals(emptyList(), response.markedNovels)
        assertNull(response.nextUrl)
    }

    @Test
    fun decodesNullableMarkerWithoutRejectingWholePage() {
        val response = json.decodeFromString<NovelMarkersResp>(
            """
            {
              "marked_novels": [
                {
                  "novel": {
                    "id": 1,
                    "title": "Unavailable",
                    "caption": "",
                    "restrict": 0,
                    "x_restrict": 0,
                    "is_original": true,
                    "image_urls": {},
                    "create_date": "",
                    "tags": [],
                    "page_count": 1,
                    "text_length": 1,
                    "user": {"id": 2, "name": "Author"},
                    "series": {},
                    "is_bookmarked": false,
                    "total_bookmarks": 0,
                    "total_view": 0,
                    "visible": false,
                    "is_muted": false,
                    "is_mypixiv_only": false,
                    "is_x_restricted": false,
                    "novel_ai_type": "0"
                  },
                  "novel_marker": null
                }
              ],
              "next_url": null
            }
            """.trimIndent()
        )

        assertEquals(1, response.markedNovels.size)
        assertNull(response.markedNovels.single().novelMarker)
    }
}
