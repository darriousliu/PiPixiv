package com.mrl.pixiv.common.repository

import com.mrl.pixiv.common.data.Constants.API_HOST
import com.mrl.pixiv.common.data.collection.CollectionSearchQuery
import com.mrl.pixiv.common.data.collection.CollectionWorkType
import com.mrl.pixiv.common.datasource.remote.createCollectionSearchApi
import io.ktor.http.URLProtocol
import io.ktor.http.Url

object CollectionSearchRepository {
    private val api by lazy { PixivRepository.apiKtorfit.createCollectionSearchApi() }

    suspend fun syncStatus() = api.syncStatus()

    suspend fun illusts(query: CollectionSearchQuery, nextUrl: String? = null) =
        api.illusts(collectionPageParameters(query, nextUrl))

    suspend fun novels(query: CollectionSearchQuery, nextUrl: String? = null) =
        api.novels(collectionPageParameters(query, nextUrl))

    suspend fun tags(query: CollectionSearchQuery, bookmarkTags: Boolean, nextUrl: String? = null) =
        (if (nextUrl == null) query.tagParameters(if (bookmarkTags) query.bookmarkTag else query.workTag)
        else collectionNextParameters(nextUrl, collectionTagPath(query.type, bookmarkTags)) +
            query.tagParameters(if (bookmarkTags) query.bookmarkTag else query.workTag)).let {
            when (query.type) {
                CollectionWorkType.ILLUST -> if (bookmarkTags) api.illustBookmarkTags(it) else api.illustWorkTags(it)
                CollectionWorkType.NOVEL -> if (bookmarkTags) api.novelBookmarkTags(it) else api.novelWorkTags(it)
            }
        }

    suspend fun periods(query: CollectionSearchQuery) = when (query.type) {
        CollectionWorkType.ILLUST -> api.illustPeriods(query.periodParameters())
        CollectionWorkType.NOVEL -> api.novelPeriods(query.periodParameters())
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
