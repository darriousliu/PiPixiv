package com.mrl.pixiv.common.repository

import com.mrl.pixiv.common.data.Filter
import com.mrl.pixiv.common.data.Restrict
import com.mrl.pixiv.common.data.auth.AuthTokenFieldReq
import com.mrl.pixiv.common.data.search.SearchIllustQuery
import com.mrl.pixiv.common.datasource.remote.createAuthApi
import com.mrl.pixiv.common.datasource.remote.createPixivApi
import com.mrl.pixiv.common.network.ApiClient
import com.mrl.pixiv.common.network.AuthClient
import com.mrl.pixiv.common.util.API_HOST
import com.mrl.pixiv.common.util.AUTH_HOST
import com.mrl.pixiv.common.util.NetworkUtil.enableBypassSniffing
import com.mrl.pixiv.common.util.hostMap
import de.jensklingenberg.ktorfit.Ktorfit
import io.ktor.client.HttpClient
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.qualifier.named

object PixivRepository : KoinComponent {
    private val authHttpClient by inject<HttpClient>(named<AuthClient>())
    private val apiHttpClient by inject<HttpClient>(named<ApiClient>())

    private val authKtorfit = Ktorfit.Builder()
        .baseUrl(
            "https://${if (enableBypassSniffing) AUTH_HOST else hostMap[AUTH_HOST]!!}/"
        )
        .httpClient(authHttpClient)
        .build()

    private val apiKtorfit = Ktorfit.Builder()
        .baseUrl(
            "https://${if (enableBypassSniffing) API_HOST else hostMap[API_HOST]!!}/"
        )
        .httpClient(apiHttpClient)
        .build()

    private val authApi = authKtorfit.createAuthApi()

    private val apiApi = apiKtorfit.createPixivApi()

    suspend fun refreshToken(authTokenFieldReq: AuthTokenFieldReq) = authApi.refreshToken(
        authTokenFieldReq.clientId,
        authTokenFieldReq.clientSecret,
        authTokenFieldReq.grantType,
        authTokenFieldReq.refreshToken,
        authTokenFieldReq.getSecureUrl,
        authTokenFieldReq.includePolicy
    )

    suspend fun login(authTokenFieldReq: AuthTokenFieldReq) = authApi.login(
        authTokenFieldReq.clientId,
        authTokenFieldReq.clientSecret,
        authTokenFieldReq.grantType,
        authTokenFieldReq.redirectUri,
        authTokenFieldReq.code,
        authTokenFieldReq.codeVerifier,
        authTokenFieldReq.getSecureUrl,
        authTokenFieldReq.includePolicy
    )

    suspend fun getIllustRecommended(
        filter: String,
        includeRankingIllusts: Boolean,
        includePrivacyPolicy: Boolean,
    ) = apiApi.getIllustRecommended(filter, includeRankingIllusts, includePrivacyPolicy)

    suspend fun loadMoreIllustRecommended(queryMap: Map<String, String>) =
        apiApi.loadMoreIllustRecommended(queryMap)

    suspend fun postIllustBookmarkAdd(
        illustId: Long,
        restrict: String = Restrict.PUBLIC,
        tags: List<String>? = null,
    ) = apiApi.postIllustBookmarkAdd(illustId, restrict, tags)

    suspend fun postIllustBookmarkDelete(illustId: Long) =
        apiApi.postIllustBookmarkDelete(illustId)

    suspend fun getIllustRelated(illustId: Long, filter: String) =
        apiApi.getIllustRelated(illustId, filter)

    suspend fun loadMoreIllustRelated(queryMap: Map<String, String>) =
        apiApi.loadMoreIllustRelated(queryMap)

    suspend fun getIllustDetail(illustId: Long, filter: String) =
        apiApi.getIllustDetail(illustId, filter)

    suspend fun getIllustBookmarkDetail(illustId: Long) =
        apiApi.getIllustBookmarkDetail(illustId)

    suspend fun searchIllust(query: SearchIllustQuery) =
        apiApi.searchIllust(
            query.filter.value,
            query.includeTranslatedTagResults,
            query.mergePlainKeywordResults,
            query.word,
            query.sort.value,
            query.searchTarget.value,
            query.bookmarkNumMin,
            query.bookmarkNumMax,
            query.startDate,
            query.endDate,
            query.searchAiType.value,
            query.offset
        )

    suspend fun searchIllustNext(queryMap: Map<String, String>) =
        apiApi.searchIllustNext(queryMap)

    suspend fun searchAutoComplete(word: String, mergePlainKeywordResults: Boolean = true) =
        apiApi.searchAutoComplete(word, mergePlainKeywordResults)

    suspend fun trendingTags(filter: Filter) = apiApi.trendingTags(filter.value)

    suspend fun getUgoiraMetadata(illustId: Long) = apiApi.getUgoiraMetadata(illustId)

    suspend fun getUserDetail(
        filter: Filter = Filter.ANDROID,
        userId: Long
    ) = apiApi.getUserDetail(filter.value, userId)

    suspend fun getUserIllusts(
        filter: Filter = Filter.ANDROID,
        userId: Long,
        type: String
    ) = apiApi.getUserIllusts(filter.value, userId, type)

    suspend fun getUserBookmarksIllust(
        restrict: String,
        userId: Long,
        tag: String? = null,
        maxBookmarkId: Long? = null
    ) = apiApi.getUserBookmarksIllust(restrict, userId, tag, maxBookmarkId)

    suspend fun loadMoreUserBookmarksIllust(queryMap: Map<String, String>) =
        apiApi.loadMoreUserBookmarksIllust(queryMap)

    suspend fun getUserBookmarksNovels(
        restrict: String,
        userId: Long,
        tag: String = ""
    ) = apiApi.getUserBookmarksNovels(restrict, userId, tag)

    suspend fun followUser(
        userId: Long,
        restrict: String
    ) = apiApi.followUser(userId, restrict)

    suspend fun unFollowUser(
        userId: Long
    ) = apiApi.unFollowUser(userId)

    suspend fun getUserBrowsingHistoryIllusts() = apiApi.getUserBrowsingHistoryIllusts()

    suspend fun loadMoreUserBrowsingHistoryIllusts(queryMap: Map<String, String>) =
        apiApi.loadMoreUserBrowsingHistoryIllusts(queryMap)

    suspend fun getUserBookmarkTagsIllust(
        userId: Long,
        restrict: String
    ) = apiApi.getUserBookmarkTagsIllust(userId, restrict)

    suspend fun getUserBookmarkTagsNovel(
        userId: Long,
        restrict: String
    ) = apiApi.getUserBookmarkTagsNovel(userId, restrict)

    suspend fun getUserFollowing(
        filter: Filter = Filter.ANDROID,
        userId: Long,
        restrict: String = Restrict.PUBLIC,
        offset: Int? = null
    ) = apiApi.getUserFollowing(filter.value, userId, restrict, offset)

    suspend fun loadMoreUserFollowing(
        queryMap: Map<String, String>
    ) = apiApi.loadMoreUserFollowing(queryMap)

    suspend fun getFollowingIllusts(
        restrict: String = Restrict.ALL,
        offset: Long? = null
    ) = apiApi.getFollowingIllusts(restrict, offset)
}