package com.mrl.pixiv.common.data.manga

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MangaSeriesSerializationTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun watchlistKeepsMaskedAndMissingEntriesWithoutCreatingNavigationTargets() {
        val response = json.decodeFromString<MangaWatchlistResp>("""
            {"series":[null,{"mask_text":"Unavailable"},
              {"id":12,"latest_content_id":30,"mask_text":"Restricted"},
              {"id":13,"title":"Series","latest_content_id":31,
               "published_content_count":4,"last_published_content_datetime":"2026-10-05T10:00:00+09:00"}],
             "next_url":"https://app-api.pixiv.net/v1/watchlist/manga?offset=30"}
        """)
        val entries = response.series.orEmpty()
        assertNull(entries[0])
        assertTrue(entries[1]!!.isMasked)
        assertNull(entries[1]!!.navigableSeriesId)
        assertNull(entries[2]!!.navigableLatestId)
        assertEquals(13L, entries[3]!!.navigableSeriesId)
        assertEquals(31L, entries[3]!!.navigableLatestId)
        assertEquals(4, entries[3]!!.publishedContentCount)
    }

    @Test
    fun seriesResponseUsesOfficialFieldNamesAndNullableChapterBoundaries() {
        val response = json.decodeFromString<MangaSeriesContextResp>("""
            {"illust_series_detail":{"id":10,"title":"Manga","caption":"Intro",
              "cover_image_urls":{"medium":"cover.png"},"series_work_count":3,
              "watchlist_added":true,"user":{"id":4}},
             "illust_series_context":{"content_order":1,"prev":null,"next":null}}
        """)
        assertEquals(10L, response.detail!!.id)
        assertEquals("cover.png", response.detail!!.coverImageUrls.medium)
        assertTrue(response.detail!!.watchlistAdded)
        assertEquals(1, response.context!!.contentOrder)
        assertNull(response.context!!.prev)
    }

    @Test
    fun invalidIdsAndNullListsCannotBecomeClickableEntries() {
        assertNull(json.decodeFromString<MangaWatchlistResp>("""{"series":null}""").series)
        val entry = MangaWatchlistEntry(id = 0, latestContentId = -2)
        assertFalse(entry.isMasked)
        assertNull(entry.navigableSeriesId)
        assertNull(entry.navigableLatestId)
    }
}
