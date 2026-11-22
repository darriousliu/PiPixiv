package com.mrl.pixiv.common.repository

import com.mrl.pixiv.common.data.Constants.API_HOST
import com.mrl.pixiv.common.data.collection.CollectionSearchQuery
import com.mrl.pixiv.common.data.collection.CollectionWorkType
import com.mrl.pixiv.common.datasource.remote.CollectionSearchApi
import io.ktor.http.URLProtocol
import io.ktor.http.Url

object CollectionSearchRepository {
    private suspend fun <T> withApi(block: suspend (CollectionSearchApi) -> T): T =
        PixivRepository.withApiServices { block(it.collection) }

    suspend fun syncStatus() = withApi { it.syncStatus() }

    suspend fun illusts(query: CollectionSearchQuery, nextUrl: String? = null) =
        withApi { it.illusts(collectionPageParameters(query, nextUrl)) }

    suspend fun novels(query: CollectionSearchQuery, nextUrl: String? = null) =
        withApi { it.novels(collectionPageParameters(query, nextUrl)) }

    suspend fun tags(query: CollectionSearchQuery, bookmarkTags: Boolean, nextUrl: String? = null) =
        withApi { api ->
            val filter = query.tagParameters(if (bookmarkTags) query.bookmarkTag else query.workTag)
            val parameters = if (nextUrl == null) filter else {
                collectionNextParameters(nextUrl, collectionTagPath(query.type, bookmarkTags)) + filter
            }
            when (query.type) {
                CollectionWorkType.ILLUST ->
                    if (bookmarkTags) api.illustBookmarkTags(parameters) else api.illustWorkTags(parameters)

                CollectionWorkType.NOVEL ->
                    if (bookmarkTags) api.novelBookmarkTags(parameters) else api.novelWorkTags(parameters)
            }
        }

    suspend fun periods(query: CollectionSearchQuery) = withApi { api ->
        when (query.type) {
            CollectionWorkType.ILLUST -> api.illustPeriods(query.periodParameters())
            CollectionWorkType.NOVEL -> api.novelPeriods(query.periodParameters())
        }
    }
}

internal fun collectionTagPath(type: CollectionWorkType, bookmarkTags: Boolean): String =
    "v1/search/bookmark/${type.value}/${if (bookmarkTags) "bookmark-tag" else "${type.value}-tag"}"

/** Keep server cursors, but never follow a URL to another host or another operation. */
internal fun collectionNextParameters(nextUrl: String, expectedPath: String): Map<String, String> {
    val url = Url(nextUrl)
    require(url.protocol == URLProtocol.HTTPS && url.host == API_HOST && url.port == 443)
    require(url.encodedPath.trimStart('/') == expectedPath && url.user == null && url.password == null)
    return url.parameters.names().associateWith { url.parameters[it].orEmpty() }
}

internal fun collectionPageParameters(query: CollectionSearchQuery, nextUrl: String?): Map<String, String> =
    if (nextUrl == null) query.toMap()
    else collectionNextParameters(nextUrl, "v1/search/bookmark/${query.type.value}") + query.toMap()
