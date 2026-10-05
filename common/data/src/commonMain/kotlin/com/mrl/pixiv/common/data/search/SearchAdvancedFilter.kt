package com.mrl.pixiv.common.data.search

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Bounds are non-negative, inclusive, and may be open at either end. */
data class SearchNumberRange(val min: Int? = null, val max: Int? = null) {
    init {
        require(min == null || min >= 0)
        require(max == null || max >= 0)
        require(min == null || max == null || min <= max)
    }
    val isEmpty: Boolean get() = min == null && max == null
    fun parameters(prefix: String): Map<String, String> = buildMap {
        min?.let { put("${prefix}_min", it.toString()) }
        max?.let { put("${prefix}_max", it.toString()) }
    }
}

fun parseSearchNumberRange(min: String, max: String): SearchNumberRange? {
    if ((min.isNotBlank() && min.toIntOrNull() == null) ||
        (max.isNotBlank() && max.toIntOrNull() == null)) return null
    return runCatching { SearchNumberRange(min.toIntOrNull(), max.toIntOrNull()) }.getOrNull()
}

enum class SearchRatio(val value: String) { PORTRAIT("portrait"), LANDSCAPE("landscape"), SQUARE("square") }

data class IllustAdvancedFilter(
    val width: SearchNumberRange = SearchNumberRange(),
    val height: SearchNumberRange = SearchNumberRange(),
    val ratio: SearchRatio? = null,
    val tool: String? = null,
    val language: String? = null,
) {
    val isActive: Boolean get() = !width.isEmpty || !height.isEmpty || ratio != null || tool != null || language != null
    fun toMap(): Map<String, String> = buildMap {
        putAll(width.parameters("width"))
        putAll(height.parameters("height"))
        ratio?.let { put("ratio_pattern", it.value) }
        tool?.takeIf(String::isNotBlank)?.let { put("tool", it) }
        language?.takeIf(String::isNotBlank)?.let { put("lang", it) }
    }
}

data class NovelAdvancedFilter(
    val textLength: SearchNumberRange = SearchNumberRange(),
    val wordCount: SearchNumberRange = SearchNumberRange(),
    val readingTime: SearchNumberRange = SearchNumberRange(),
    val language: String? = null,
    val genre: Int? = null,
    val originalOnly: Boolean = false,
    val replaceableOnly: Boolean = false,
) {
    val isActive: Boolean get() = !textLength.isEmpty || !wordCount.isEmpty || !readingTime.isEmpty ||
        language != null || genre != null || originalOnly || replaceableOnly
    fun toMap(): Map<String, String> = buildMap {
        putAll(textLength.parameters("text_length"))
        putAll(wordCount.parameters("word_count"))
        putAll(readingTime.parameters("reading_time"))
        language?.takeIf(String::isNotBlank)?.let { put("lang", it) }
        genre?.let { put("genre", it.toString()) }
        if (originalOnly) put("is_original_only", "true")
        if (replaceableOnly) put("is_replaceable_only", "true")
    }
}

@Serializable
data class SearchOptionsResponse(
    val illust: SearchIllustOptions = SearchIllustOptions(),
    val novel: SearchNovelOptions = SearchNovelOptions(),
)
@Serializable
data class SearchIllustOptions(
    val lang: SearchLanguages = SearchLanguages(),
    val tool: SearchTools = SearchTools(),
)
@Serializable
data class SearchNovelOptions(
    val lang: SearchLanguages = SearchLanguages(),
    val genre: SearchGenres = SearchGenres(),
    @SerialName("word_count_supported_languages") val wordCountSupportedLanguages: String = "",
)
@Serializable
data class SearchLanguages(val options: List<SearchLanguageOption> = emptyList())
@Serializable
data class SearchLanguageOption(val code: String, val name: String)
@Serializable
data class SearchTools(val options: List<String> = emptyList())
@Serializable
data class SearchGenres(val options: List<SearchGenreOption> = emptyList())
@Serializable
data class SearchGenreOption(val id: Int, val label: String)

/** Dynamic values must have come from the current options response. */
fun IllustAdvancedFilter.validatedBy(options: SearchOptionsResponse): IllustAdvancedFilter = copy(
    tool = tool?.takeIf { it in options.illust.tool.options },
    language = language?.takeIf { value -> options.illust.lang.options.any { it.code == value } },
)
fun NovelAdvancedFilter.validatedBy(options: SearchOptionsResponse): NovelAdvancedFilter = copy(
    language = language?.takeIf { value -> options.novel.lang.options.any { it.code == value } },
    genre = genre?.takeIf { value -> options.novel.genre.options.any { it.id == value } },
)
