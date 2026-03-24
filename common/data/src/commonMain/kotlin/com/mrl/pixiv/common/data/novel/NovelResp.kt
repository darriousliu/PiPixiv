package com.mrl.pixiv.common.data.novel

import androidx.compose.runtime.Immutable
import com.mrl.pixiv.common.data.AiType
import com.mrl.pixiv.common.data.Novel
import com.mrl.pixiv.common.data.User
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.mapSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject

/**
 * 小说系列详情响应
 */
@Serializable
@Immutable
data class NovelSeriesResp(
    @SerialName("novel_series_detail")
    val novelSeriesDetail: NovelSeriesDetail,

    @SerialName("novel_series_first_novel")
    val novelSeriesFirstNovel: Novel,

    @SerialName("novel_series_latest_novel")
    val novelSeriesLatestNovel: Novel,

    val novels: List<Novel>,

    @SerialName("next_url")
    val nextUrl: String? = null
)

/**
 * 小说系列详情
 */
@Serializable
@Immutable
data class NovelSeriesDetail(
    val id: Long,
    val title: String,
    val caption: String,

    @SerialName("is_original")
    val isOriginal: Boolean,

    @SerialName("is_concluded")
    val isConcluded: Boolean,

    @SerialName("content_count")
    val contentCount: Int,

    @SerialName("total_character_count")
    val totalCharacterCount: Long,

    val user: User,

    @SerialName("display_text")
    val displayText: String,

    @SerialName("novel_ai_type")
    val novelAiType: AiType,

    @SerialName("watchlist_added")
    val watchlistAdded: Boolean
)

/**
 * 小说详情响应
 */
@Serializable
@Immutable
data class NovelDetailResp(
    val novel: Novel
)

/**
 * 搜索小说响应
 */
@Serializable
@Immutable
data class SearchNovelResp(
    val novels: List<Novel>,

    @SerialName("next_url")
    val nextUrl: String? = null,

    @SerialName("search_span_limit")
    val searchSpanLimit: Long? = null
)


/**
 * 小说文本内容响应
 * 从HTML中解析得到的小说正文
 */
@Serializable
@Immutable
data class NovelTextResp(
    val id: String,
    val title: String,
    val seriesId: JsonElement? = null,
    val seriesTitle: JsonElement? = null,
    val seriesIsWatched: JsonElement? = null,
    val userId: String,
    val coverUrl: String,
    val tags: List<String>,
    val caption: String,
    val cdate: String,
    val rating: NovelRating,
    val text: String,
    val marker: JsonElement? = null,
    val seriesNavigation: SeriesNavigation? = null,
    val glossaryItems: List<JsonElement>? = null,
    val replaceableItemIds: List<JsonElement>? = null,
    @Serializable(with = IllustsSerializer::class)
    val illusts: Map<String, NovelIllusts?>? = null,
    @Serializable(with = ImagesSerializer::class)
    val images: Map<String, NovelImage>? = null,
    val aiType: Int? = null,
    val isOriginal: Boolean? = null,
)

private class IllustsSerializer : KSerializer<Map<String, NovelIllusts?>?> {
    override val descriptor = mapSerialDescriptor<String, NovelIllusts?>()

    override fun deserialize(decoder: Decoder): Map<String, NovelIllusts?>? {
        val jsonDecoder = decoder as? JsonDecoder ?: error("Only JSON supported")
        // 空数组 [] 或任意 JsonArray → 返回 null
        return when (val element = jsonDecoder.decodeJsonElement()) {
            is JsonArray -> null
            // null → 返回 null
            is JsonNull -> null
            // 正常 Map → 解析
            is JsonObject -> element.mapValues { (_, v) ->
                if (v is JsonNull || v is JsonObject && v["illust"] is JsonNull) {
                    null
                } else {
                    Json.decodeFromJsonElement(NovelIllusts.serializer(), v)
                }
            }

            else -> null
        }
    }

    override fun serialize(encoder: Encoder, value: Map<String, NovelIllusts?>?) {
        if (value == null) {
            encoder.encodeNull()
        } else {
            val jsonEncoder = encoder as? JsonEncoder ?: error("Only JSON supported")
            val obj = JsonObject(value.mapValues { (_, v) ->
                if (v == null) JsonNull
                else Json.encodeToJsonElement(NovelIllusts.serializer(), v)
            })
            jsonEncoder.encodeJsonElement(obj)
        }
    }
}

private class ImagesSerializer : KSerializer<Map<String, NovelImage>?> {
    override val descriptor = mapSerialDescriptor<String, NovelImage>()

    override fun deserialize(decoder: Decoder): Map<String, NovelImage>? {
        val jsonDecoder = decoder as? JsonDecoder ?: error("Only JSON supported")
        return when (val element = jsonDecoder.decodeJsonElement()) {
            is JsonArray -> null
            is JsonNull -> null
            is JsonObject -> element.mapValues { (_, v) ->
                Json.decodeFromJsonElement(NovelImage.serializer(), v)
            }
            else -> null
        }
    }

    override fun serialize(encoder: Encoder, value: Map<String, NovelImage>?) {
        if (value == null) {
            encoder.encodeNull()
        } else {
            val jsonEncoder = encoder as? JsonEncoder ?: error("Only JSON supported")
            val obj = JsonObject(value.mapValues { (_, v) ->
                Json.encodeToJsonElement(NovelImage.serializer(), v)
            })
            jsonEncoder.encodeJsonElement(obj)
        }
    }
}

@Serializable
@Immutable
data class NovelIllusts(
    val illust: NovelIllust,
)

@Serializable
@Immutable
data class NovelIllust(
    val images: NovelIllustImages,
)

@Serializable
@Immutable
data class NovelIllustImages(
    val small: String? = null,
    val medium: String? = null,
    val original: String? = null,
)

@Serializable
@Immutable
data class NovelRating(
    val like: Int,
    val bookmark: Int,
    val view: Int,
)

@Serializable
@Immutable
data class NovelImage(
    val novelImageId: String? = null,
    val sl: String,
    val urls: NovelUrls,
)

@Serializable
@Immutable
data class NovelUrls(
    @SerialName("240mw")
    val the240Mw: String? = null,
    @SerialName("480mw")
    val the480Mw: String? = null,
    @SerialName("1200x1200")
    val the1200X1200: String? = null,
    @SerialName("128x128")
    val the128X128: String? = null,
    val original: String? = null,
)

@Serializable
@Immutable
data class SeriesNavigation(
    val nextNovel: SeriesNovel? = null,
    val prevNovel: SeriesNovel? = null
)

@Serializable
@Immutable
data class SeriesNovel(
    val id: Long,
    val viewable: Boolean,
    val contentOrder: String,
    val title: String,
    val coverUrl: String,
    val viewableMessage: String? = null
)

/**
 * 小说推荐响应
 */
@Serializable
@Immutable
data class NovelRecommendedResp(
    val novels: List<Novel>,

    @SerialName("ranking_novels")
    val rankingNovels: List<Novel> = emptyList(),

    @SerialName("next_url")
    val nextUrl: String? = null
)

/**
 * 小说排行榜响应
 */
@Serializable
@Immutable
data class NovelRankingResp(
    val novels: List<Novel>,

    @SerialName("next_url")
    val nextUrl: String? = null
)

/**
 * 小说相关作品响应
 */
@Serializable
@Immutable
data class NovelRelatedResp(
    val novels: List<Novel>,

    @SerialName("next_url")
    val nextUrl: String? = null
)
