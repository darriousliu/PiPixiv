package com.mrl.pixiv.common.data.collection

import com.mrl.pixiv.common.data.Restrict
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class CollectionSearchTest {
    @Test
    fun collectionFiltersAreMonthlyTagsAndVisibilityWithoutFullTextOrUserId() {
        val query = CollectionSearchQuery(type = CollectionWorkType.NOVEL, restrict = Restrict.PRIVATE,
            bookmarkTag = " reading ", workTag = " fantasy ", period = "2026-10", order = CollectionSearchOrder.OLDEST)
        assertEquals(mapOf("bookmark_restrict" to "private", "bookmark_tag" to "reading",
            "novel_tag" to "fantasy", "bookmark_period" to "2026-10", "order" to "bookmarked_asc"), query.toMap())
        assertNull(query.toMap()["word"])
        assertNull(query.toMap()["user_id"])
        assertNull(query.toMap()["illust_tag"])
        assertEquals(mapOf("bookmark_restrict" to "private", "bookmark_tag" to "reading", "novel_tag" to "fantasy"), query.periodParameters())
        assertEquals(mapOf("bookmark_restrict" to "private", "bookmark_period" to "2026-10", "word" to "fa"), query.tagParameters(" fa "))
    }

    @Test
    fun monthCannotBeAnArbitraryQueryFragment() {
        listOf("2026-00", "2026-13", "2026-1", "2026-10-01", "0000-01", "2026-10&order=bad").forEach {
            assertFalse(isCollectionMonth(it))
            assertFailsWith<IllegalArgumentException> { CollectionSearchQuery(period = it) }
        }
        assertFailsWith<IllegalArgumentException> { CollectionSearchQuery(restrict = Restrict.ALL) }
    }

    @Test
    fun synchronisationMustBeExplicitAndTagDtoKeepsPagination() {
        assertEquals(false, Json.decodeFromString<CollectionSyncStatus>("""{"is_synchronised":false}""").isSynchronised)
        assertFailsWith<SerializationException> { Json.decodeFromString<CollectionSyncStatus>("{}") }
        val tags = Json.decodeFromString<CollectionTagOptions>("""{"tags":[{"name":"猫","count":42,"translated_name":"cat"}],"next_url":"https://app-api.pixiv.net/v1/search/bookmark/illust/bookmark-tag?offset=30"}""")
        assertEquals("cat", tags.tags.single().translatedName)
        assertEquals(42L, tags.tags.single().count)
        assertEquals(true, tags.nextUrl?.endsWith("offset=30"))
    }
}
