package com.mrl.pixiv.common.repository

import com.mrl.pixiv.common.data.Constants.API_HOST
import com.mrl.pixiv.common.data.Constants.AUTH_HOST
import com.mrl.pixiv.common.data.Filter
import com.mrl.pixiv.common.data.Restrict
import com.mrl.pixiv.common.data.auth.AuthTokenFieldReq
import com.mrl.pixiv.common.data.auth.AuthTokenResp
import com.mrl.pixiv.common.data.search.SearchIllustQuery
import com.mrl.pixiv.common.data.search.SearchNovelQuery
import com.mrl.pixiv.common.datasource.remote.AuthApi
import com.mrl.pixiv.common.datasource.remote.PixivApi
import com.mrl.pixiv.common.datasource.remote.createAuthApi
import com.mrl.pixiv.common.datasource.remote.createCollectionSearchApi
import com.mrl.pixiv.common.datasource.remote.createCommunityApi
import com.mrl.pixiv.common.datasource.remote.createReadingApi
import com.mrl.pixiv.common.datasource.remote.createPixivApi
import com.mrl.pixiv.common.network.ApiClient
import com.mrl.pixiv.common.network.AuthClient
import com.mrl.pixiv.common.network.RoutedHttpClient
import com.mrl.pixiv.common.serialize.JSON
import de.jensklingenberg.ktorfit.Ktorfit
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import io.ktor.util.AttributeKey
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.qualifier.named

object PixivRepository : KoinComponent {
    private val authHttpClient by inject<RoutedHttpClient<HttpClient>>(named<AuthClient>())
    private val apiHttpClient by inject<RoutedHttpClient<HttpClient>>(named<ApiClient>())
    private val authApiKey = AttributeKey<AuthApi>("PixivAuthApi")
    private val apiServicesKey = AttributeKey<ApiServices>("PixivApiServices")

    internal class ApiServices(ktorfit: Ktorfit) {
        val pixiv = ktorfit.createPixivApi()
        val collection = ktorfit.createCollectionSearchApi()
        val community = ktorfit.createCommunityApi()
        val reading = ktorfit.createReadingApi()
    }

    internal suspend fun <T> withApiServices(block: suspend (ApiServices) -> T): T =
        apiHttpClient.withClient { client ->
            val services = client.attributes.computeIfAbsent(apiServicesKey) {
                ApiServices(createKtorfit(API_HOST, client))
            }
            block(services)
        }

    internal suspend fun <T> withApi(block: suspend (PixivApi) -> T): T =
        withApiServices { block(it.pixiv) }

    private suspend fun <T> withAuth(block: suspend (AuthApi) -> T): T =
        authHttpClient.withClient { client ->
            val auth = client.attributes.computeIfAbsent(authApiKey) {
                createKtorfit(AUTH_HOST, client).createAuthApi()
            }
            block(auth)
        }

    private fun createKtorfit(host: String, client: HttpClient) =
        Ktorfit.Builder().baseUrl("https://$host/").httpClient(client).build()

    suspend fun refreshToken(authTokenFieldReq: AuthTokenFieldReq) = withAuth {
        it.refreshToken(
            authTokenFieldReq.clientId,
            authTokenFieldReq.clientSecret,
            authTokenFieldReq.grantType,
            authTokenFieldReq.refreshToken,
            authTokenFieldReq.getSecureUrl,
            authTokenFieldReq.includePolicy
        ).readRefreshResponse()
    }

    suspend fun login(authTokenFieldReq: AuthTokenFieldReq) = withAuth {
        it.login(
            authTokenFieldReq.clientId,
            authTokenFieldReq.clientSecret,
            authTokenFieldReq.grantType,
            authTokenFieldReq.redirectUri,
            authTokenFieldReq.code,
            authTokenFieldReq.codeVerifier,
            authTokenFieldReq.getSecureUrl,
            authTokenFieldReq.includePolicy
        )
    }

    suspend fun getIllustRecommended(
        filter: String,
        includeRankingIllusts: Boolean,
        includePrivacyPolicy: Boolean,
    ) = withApi { it.getIllustRecommended(filter, includeRankingIllusts, includePrivacyPolicy) }

    suspend fun loadMoreIllustRecommended(queryMap: Map<String, String>) =
        withApi { it.loadMoreIllustRecommended(queryMap) }

    suspend fun getMangaRecommended(
        filter: String,
        includeRankingIllusts: Boolean,
        includePrivacyPolicy: Boolean,
    ) = withApi { it.getMangaRecommended(filter, includeRankingIllusts, includePrivacyPolicy) }

    suspend fun loadMoreMangaRecommended(queryMap: Map<String, String>) =
        withApi { it.loadMoreMangaRecommended(queryMap) }

    suspend fun getIllustRanking(
        mode: String,
        filter: Filter = Filter.ANDROID,
        date: String? = null,
        offset: Int? = null
    ) = withApi { it.getIllustRanking(mode, filter.value, date, offset) }

    suspend fun loadMoreIllustRanking(queryMap: Map<String, String>) =
        withApi { it.loadMoreIllustRanking(queryMap) }

    suspend fun postIllustBookmarkAdd(
        illustId: Long,
        restrict: Restrict = Restrict.PUBLIC,
        tags: List<String>? = null,
    ) = withApi { it.postIllustBookmarkAdd(illustId, restrict.value, tags) }

    suspend fun postIllustBookmarkDelete(illustId: Long) =
        withApi { it.postIllustBookmarkDelete(illustId) }

    suspend fun getIllustRelated(illustId: Long, filter: String) =
        withApi { it.getIllustRelated(illustId, filter) }

    suspend fun loadMoreIllustRelated(queryMap: Map<String, String>) =
        withApi { it.loadMoreIllustRelated(queryMap) }

    suspend fun getIllustDetail(illustId: Long, filter: String) =
        withApi { it.getIllustDetail(illustId, filter) }

    suspend fun getIllustBookmarkDetail(illustId: Long) =
        withApi { it.getIllustBookmarkDetail(illustId) }

    suspend fun searchIllust(query: SearchIllustQuery) = withApi { it.searchIllust(query.toMap()) }

    suspend fun searchIllustNext(queryMap: Map<String, String>) =
        withApi { it.searchIllustNext(queryMap) }

    suspend fun searchUser(word: String, offset: Int? = null) =
        withApi { it.searchUser(word = word, offset = offset) }

    suspend fun searchUserNext(queryMap: Map<String, String>) = withApi { it.searchUserNext(queryMap) }

    suspend fun getSearchOptions(word: String, searchTarget: String, searchAiTypes: Int) =
        withApi { it.getSearchOptions(word, searchTarget, searchAiTypes) }

    suspend fun searchAutoComplete(word: String, mergePlainKeywordResults: Boolean = true) =
        withApi { it.searchAutoComplete(word, mergePlainKeywordResults) }

    suspend fun trendingTags(filter: Filter) = withApi { it.trendingTags(filter.value) }

    suspend fun getUgoiraMetadata(illustId: Long) = withApi { it.getUgoiraMetadata(illustId) }

    suspend fun getUserDetail(
        filter: Filter = Filter.ANDROID,
        userId: Long
    ) = withApi { it.getUserDetail(filter.value, userId) }

    suspend fun getUserIllusts(
        filter: Filter = Filter.ANDROID,
        userId: Long,
        type: String
    ) = withApi { it.getUserIllusts(filter.value, userId, type) }

    suspend fun loadMoreUserIllusts(queryMap: Map<String, String>) =
        withApi { it.getUserIllusts(queryMap) }

    suspend fun getUserBookmarksIllust(
        restrict: Restrict,
        userId: Long,
        tag: String? = null,
        maxBookmarkId: Long? = null
    ) = withApi { it.getUserBookmarksIllust(restrict.value, userId, tag, maxBookmarkId) }

    suspend fun loadMoreUserBookmarksIllust(queryMap: Map<String, String>) =
        withApi { it.loadMoreUserBookmarksIllust(queryMap) }

    suspend fun getUserBookmarksNovels(
        restrict: Restrict,
        userId: Long,
        tag: String? = null,
        maxBookmarkId: Long? = null
    ) = withApi { it.getUserBookmarksNovels(restrict.value, userId, tag, maxBookmarkId) }

    suspend fun loadMoreUserBookmarksNovel(queryMap: Map<String, String>) =
        withApi { it.loadMoreUserBookmarksNovel(queryMap) }

    suspend fun followUser(
        userId: Long,
        restrict: Restrict
    ) = withApi { it.followUser(userId, restrict.value) }

    suspend fun unFollowUser(
        userId: Long
    ) = withApi { it.unFollowUser(userId) }

    suspend fun getUserBrowsingHistoryIllusts() = withApi { it.getUserBrowsingHistoryIllusts() }

    suspend fun loadMoreUserBrowsingHistoryIllusts(queryMap: Map<String, String>) =
        withApi { it.loadMoreUserBrowsingHistoryIllusts(queryMap) }

    suspend fun getUserBrowsingHistoryNovels() = withApi { it.getUserBrowsingHistoryNovels() }

    suspend fun loadMoreUserBrowsingHistoryNovels(queryMap: Map<String, String>) =
        withApi { it.loadMoreUserBrowsingHistoryNovels(queryMap) }

    suspend fun getUserBookmarkTagsIllust(
        userId: Long,
        restrict: String
    ) = withApi { it.getUserBookmarkTagsIllust(userId, restrict) }

    suspend fun getUserBookmarkTagsNovel(
        userId: Long,
        restrict: String
    ) = withApi { it.getUserBookmarkTagsNovel(userId, restrict) }

    suspend fun loadMoreUserBookmarkTagsIllust(queryMap: Map<String, String>) =
        withApi { it.loadMoreUserBookmarkTagsIllust(queryMap) }

    suspend fun loadMoreUserBookmarkTagsNovel(queryMap: Map<String, String>) =
        withApi { it.loadMoreUserBookmarkTagsNovel(queryMap) }

    suspend fun getUserFollowing(
        filter: Filter = Filter.ANDROID,
        userId: Long,
        restrict: Restrict = Restrict.PUBLIC,
        offset: Int? = null
    ) = withApi { it.getUserFollowing(filter.value, userId, restrict.value, offset) }

    suspend fun loadMoreUserFollowing(
        queryMap: Map<String, String>
    ) = withApi { it.loadMoreUserFollowing(queryMap) }

    suspend fun getFollowingIllusts(
        restrict: Restrict = Restrict.ALL,
        offset: Long? = null
    ) = withApi { it.getFollowingIllusts(restrict.value, offset) }

    suspend fun getMuteList() = withApi { it.getMuteList() }

    suspend fun postMuteSetting(
        addUserIds: List<Long>? = null,
        deleteUserIds: List<Long>? = null,
        addTags: List<String>? = null,
        deleteTags: List<String>? = null
    ) = withApi { it.postMuteSetting(addUserIds, deleteUserIds, addTags, deleteTags) }

    suspend fun addIllustBrowsingHistory(vararg illustIds: Long) =
        withApi { it.addIllustBrowsingHistory(illustIds.toList()) }

    suspend fun addIllustBrowsingHistory(illustIds: List<Long>) =
        withApi { it.addIllustBrowsingHistory(illustIds) }

    suspend fun addNovelBrowsingHistory(vararg novelIds: Long) =
        withApi { it.addNovelBrowsingHistory(novelIds.toList()) }

    suspend fun addNovelBrowsingHistory(novelIds: List<Long>) =
        withApi { it.addNovelBrowsingHistory(novelIds) }

    suspend fun searchPopularPreviewIllust(query: SearchIllustQuery) =
        withApi {
            it.searchPopularPreviewIllust(
                query.filter.value,
                query.includeTranslatedTagResults,
                query.mergePlainKeywordResults,
                query.word,
                query.searchTarget.value,
                query.searchAiType.value,
            )
        }

    suspend fun searchPopularPreviewNovel(query: SearchNovelQuery) =
        withApi {
            it.searchPopularPreviewNovel(
                query.filter.value,
                query.includeTranslatedTagResults,
                query.mergePlainKeywordResults,
                query.word,
                query.searchTarget.value,
                query.searchAiType.value,
            )
        }

    suspend fun getIllustComments(illustId: Long, offset: Int? = null) =
        withApi { it.getIllustComments(illustId, offset) }

    suspend fun loadMoreIllustComments(queryMap: Map<String, String>) =
        withApi { it.loadMoreIllustComments(queryMap) }

    suspend fun getIllustCommentReplies(commentId: Long, offset: Int? = null) =
        withApi { it.getIllustCommentReplies(commentId, offset) }

    suspend fun loadMoreIllustCommentReplies(queryMap: Map<String, String>) =
        withApi { it.loadMoreIllustCommentReplies(queryMap) }

    suspend fun getNovelComments(novelId: Long, offset: Int? = null) =
        withApi { it.getNovelComments(novelId, offset) }

    suspend fun loadMoreNovelComments(queryMap: Map<String, String>) =
        withApi { it.loadMoreNovelComments(queryMap) }

    suspend fun getNovelCommentReplies(commentId: Long, offset: Int? = null) =
        withApi { it.getNovelCommentReplies(commentId, offset) }

    suspend fun loadMoreNovelCommentReplies(queryMap: Map<String, String>) =
        withApi { it.loadMoreNovelCommentReplies(queryMap) }

    suspend fun getEmojis() = withApi { it.getEmojis() }

    suspend fun getStamps() = withApi { it.getStamps() }

    suspend fun addIllustComment(
        illustId: Long,
        comment: String,
        stampId: Int? = null,
        parentCommentId: Long? = null
    ) = withApi { it.addIllustComment(illustId, comment, stampId, parentCommentId) }

    suspend fun deleteIllustComment(commentId: Long) = withApi { it.deleteIllustComment(commentId) }

    suspend fun addNovelComment(
        novelId: Long,
        comment: String,
        stampId: Int? = null,
        parentCommentId: Long? = null
    ) = withApi { it.addNovelComment(novelId, comment, stampId, parentCommentId) }

    suspend fun deleteNovelComment(commentId: Long) = withApi { it.deleteNovelComment(commentId) }

    suspend fun getUserReportTopicList() = withApi { it.getUserReportTopicList() }

    suspend fun getIllustReportTopicList() = withApi { it.getIllustReportTopicList() }

    suspend fun getNovelReportTopicList() = withApi { it.getNovelReportTopicList() }

    suspend fun getIllustCommentReportTopicList() = withApi { it.getIllustCommentReportTopicList() }

    suspend fun getNovelCommentReportTopicList() = withApi { it.getNovelCommentReportTopicList() }

    suspend fun reportUser(userId: Long, topicId: Int, description: String) =
        withApi { it.reportUser(userId, topicId, description) }

    suspend fun reportIllust(illustId: Long, topicId: Int, description: String) =
        withApi { it.reportIllust(illustId, topicId, description) }

    suspend fun reportNovel(novelId: Long, topicId: Int, description: String) =
        withApi { it.reportNovel(novelId, topicId, description) }

    suspend fun reportIllustComment(commentId: Long, topicId: Int, description: String) =
        withApi { it.reportIllustComment(commentId, topicId, description) }

    suspend fun reportNovelComment(commentId: Long, topicId: Int, description: String) =
        withApi { it.reportNovelComment(commentId, topicId, description) }

    // ==================== Novel Repository Methods ====================

    suspend fun getUserNovels(
        filter: Filter = Filter.ANDROID,
        userId: Long,
        offset: Int? = null
    ) = withApi { it.getUserNovels(filter.value, userId, offset) }

    suspend fun loadMoreUserNovels(queryMap: Map<String, String>) =
        withApi { it.loadMoreUserNovels(queryMap) }

    suspend fun getNovelSeries(
        seriesId: Long,
        filter: Filter = Filter.ANDROID,
        offset: Int? = null
    ) = withApi { it.getNovelSeries(seriesId, filter.value, offset) }

    suspend fun loadMoreNovelSeries(queryMap: Map<String, String>) =
        withApi { it.loadMoreNovelSeries(queryMap) }

    suspend fun getNovelWatchlist() = withApi { it.getNovelWatchlist() }

    suspend fun loadMoreNovelWatchlist(queryMap: Map<String, String>) =
        withApi { it.loadMoreNovelWatchlist(queryMap) }

    suspend fun addNovelSeriesToWatchlist(seriesId: Long) =
        withApi { it.addNovelSeriesToWatchlist(seriesId) }

    suspend fun deleteNovelSeriesFromWatchlist(seriesId: Long) =
        withApi { it.deleteNovelSeriesFromWatchlist(seriesId) }

    suspend fun getNovelDetail(novelId: Long) = withApi { it.getNovelDetail(novelId) }

    suspend fun getTrendingNovelTags(filter: Filter = Filter.ANDROID) =
        withApi { it.getTrendingNovelTags(filter.value) }

    suspend fun searchNovel(query: SearchNovelQuery) = withApi { it.searchNovel(query.toMap()) }

    suspend fun searchNovelNext(queryMap: Map<String, String>) =
        withApi { it.searchNovelNext(queryMap) }

    suspend fun getNovelRecommended(
        filter: Filter = Filter.ANDROID,
        includeRankingNovels: Boolean = true,
        includePrivacyPolicy: Boolean = false
    ) = withApi { it.getNovelRecommended(filter.value, includeRankingNovels, includePrivacyPolicy) }

    suspend fun loadMoreNovelRecommended(queryMap: Map<String, String>) =
        withApi { it.loadMoreNovelRecommended(queryMap) }

    suspend fun getNovelRanking(
        mode: String,
        filter: Filter = Filter.ANDROID,
        date: String? = null,
        offset: Int? = null
    ) = withApi { it.getNovelRanking(mode, filter.value, date, offset) }

    suspend fun loadMoreNovelRanking(queryMap: Map<String, String>) =
        withApi { it.loadMoreNovelRanking(queryMap) }

    suspend fun getNovelNew(
        filter: Filter = Filter.ANDROID,
        offset: Int? = null
    ) = withApi { it.getNovelNew(filter.value, offset) }

    suspend fun loadMoreNovelNew(queryMap: Map<String, String>) =
        withApi { it.loadMoreNovelNew(queryMap) }

    suspend fun getFollowNovels(
        restrict: Restrict = Restrict.ALL,
        offset: Long? = null
    ) = withApi { it.getFollowNovels(restrict.value, offset) }

    suspend fun loadMoreFollowNovels(queryMap: Map<String, String>) =
        withApi { it.loadMoreFollowNovels(queryMap) }

    suspend fun postNovelBookmarkAdd(
        novelId: Long,
        restrict: Restrict = Restrict.PUBLIC,
        tags: List<String>? = null
    ) = withApi { it.postNovelBookmarkAdd(novelId, restrict.value, tags) }

    suspend fun postNovelBookmarkDelete(novelId: Long) =
        withApi { it.postNovelBookmarkDelete(novelId) }

    suspend fun getNovelContent(novelId: Long) = withApi { it.getNovelContent(novelId) }
    suspend fun getNovelBookmarkDetail(novelId: Long) = withApi { it.getNovelBookmarkDetail(novelId) }

    suspend fun getNovelMarkers() = withApi { it.getNovelMarkers() }

    suspend fun loadMoreNovelMarkers(queryMap: Map<String, String>) =
        withApi { it.loadMoreNovelMarkers(queryMap) }

    suspend fun postNovelMarkerAdd(novelId: Long, page: Int) =
        withApi { it.postNovelMarkerAdd(novelId, page) }

    suspend fun postNovelMarkerDelete(novelId: Long) =
        withApi { it.postNovelMarkerDelete(novelId) }
}

internal class InvalidRefreshGrantException : IllegalStateException("The refresh token is invalid")

private suspend fun HttpResponse.readRefreshResponse(): AuthTokenResp {
    if (status.isSuccess()) return body()
    if (status == HttpStatusCode.BadRequest) {
        val error = try {
            val payload = JSON.parseToJsonElement(bodyAsText()) as? JsonObject
            (payload?.get("error") as? JsonPrimitive)?.contentOrNull
        } catch (_: SerializationException) {
            null
        }
        if (error == "invalid_grant") throw InvalidRefreshGrantException()
    }
    error("Token refresh returned HTTP ${status.value}")
}
