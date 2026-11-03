package com.mrl.pixiv.common.data.collection

import com.mrl.pixiv.common.data.Illust
import com.mrl.pixiv.common.data.Novel
import com.mrl.pixiv.common.data.Restrict
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

enum class CollectionWorkType(val value: String, val tagParameter: String) {
    ILLUST("illust", "illust_tag"), NOVEL("novel", "novel_tag")
}

enum class CollectionSearchOrder(val value: String) {
    NEWEST("bookmarked_desc"), OLDEST("bookmarked_asc")
}

/** Only the two visibility values and monthly periods observed in the official client. */
data class CollectionSearchQuery(
    val type: CollectionWorkType = CollectionWorkType.ILLUST,
    val restrict: Restrict = Restrict.PUBLIC,
    val bookmarkTag: String = "",
    val workTag: String = "",
    val period: String? = null,
    val order: CollectionSearchOrder = CollectionSearchOrder.NEWEST,
) {
    init {
        require(restrict == Restrict.PUBLIC || restrict == Restrict.PRIVATE)
        require(period == null || isCollectionMonth(period))
        require(bookmarkTag.length <= 100 && workTag.length <= 100)
    }

    fun toMap(): Map<String, String> = buildMap {
        put("bookmark_restrict", restrict.value)
        bookmarkTag.trim().takeIf(String::isNotEmpty)?.let { put("bookmark_tag", it) }
        workTag.trim().takeIf(String::isNotEmpty)?.let { put(type.tagParameter, it) }
        period?.let { put("bookmark_period", it) }
        put("order", order.value)
    }

    fun periodParameters(): Map<String, String> = toMap() - setOf("bookmark_period", "order")

    fun tagParameters(word: String): Map<String, String> = buildMap {
        put("bookmark_restrict", restrict.value)
        period?.let { put("bookmark_period", it) }
        word.trim().take(100).takeIf(String::isNotEmpty)?.let { put("word", it) }
    }
}

fun isCollectionMonth(value: String): Boolean =
    Regex("[0-9]{4}-(0[1-9]|1[0-2])").matches(value) && value.take(4).toInt() > 0

@Serializable
data class CollectionIllustSearchResponse(
    val illusts: List<Illust> = emptyList(),
    val total: Long = 0,
    @SerialName("next_url") val nextUrl: String? = null,
)

@Serializable
data class CollectionNovelSearchResponse(
    val novels: List<Novel> = emptyList(),
    val total: Long = 0,
    @SerialName("next_url") val nextUrl: String? = null,
)

@Serializable
data class CollectionSyncStatus(@SerialName("is_synchronised") val isSynchronised: Boolean)

@Serializable
data class CollectionTagOption(
    val name: String,
    val count: Long = 0,
    @SerialName("translated_name") val translatedName: String? = null,
)

@Serializable
data class CollectionTagOptions(
    val tags: List<CollectionTagOption> = emptyList(),
    @SerialName("next_url") val nextUrl: String? = null,
)

@Serializable
data class CollectionPeriodOption(val name: String, val count: Long = 0)

@Serializable
data class CollectionPeriodOptions(val periods: List<CollectionPeriodOption> = emptyList())
