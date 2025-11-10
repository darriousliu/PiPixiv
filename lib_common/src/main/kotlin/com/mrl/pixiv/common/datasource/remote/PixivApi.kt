package com.mrl.pixiv.common.datasource.remote

import com.mrl.pixiv.common.data.EmptyResp
import com.mrl.pixiv.common.data.Filter
import com.mrl.pixiv.common.data.Restrict
import com.mrl.pixiv.common.data.illust.IllustBookmarkDetailResp
import com.mrl.pixiv.common.data.illust.IllustDetailResp
import com.mrl.pixiv.common.data.illust.IllustRecommendedResp
import com.mrl.pixiv.common.data.mute.MutedResp
import com.mrl.pixiv.common.data.search.SearchAiType
import com.mrl.pixiv.common.data.search.SearchAutoCompleteResp
import com.mrl.pixiv.common.data.search.SearchIllustResp
import com.mrl.pixiv.common.data.search.SearchSort
import com.mrl.pixiv.common.data.search.SearchTarget
import com.mrl.pixiv.common.data.search.TrendingTagsResp
import com.mrl.pixiv.common.data.ugoira.UgoiraMetadataResp
import com.mrl.pixiv.common.data.user.IllustsWithNextUrl
import com.mrl.pixiv.common.data.user.UserBookmarkTagsResp
import com.mrl.pixiv.common.data.user.UserDetailResp
import com.mrl.pixiv.common.data.user.UserFollowingResp
import com.mrl.pixiv.common.data.user.UserIllustsResp
import com.mrl.pixiv.common.data.user.UserNovelsResp
import de.jensklingenberg.ktorfit.http.Field
import de.jensklingenberg.ktorfit.http.FormUrlEncoded
import de.jensklingenberg.ktorfit.http.GET
import de.jensklingenberg.ktorfit.http.POST
import de.jensklingenberg.ktorfit.http.Query
import de.jensklingenberg.ktorfit.http.QueryMap

interface PixivApi {
    @GET("v1/illust/recommended")
    suspend fun getIllustRecommended(
        @Query("filter") filter: String,
        @Query("include_ranking_illusts") includeRankingIllusts: Boolean,
        @Query("include_privacy_policy") includePrivacyPolicy: Boolean,
    ): IllustRecommendedResp

    @GET("v1/illust/recommended")
    suspend fun loadMoreIllustRecommended(
        @QueryMap queryMap: Map<String, String>,
    ): IllustRecommendedResp

    @FormUrlEncoded
    @POST("v2/illust/bookmark/add")
    suspend fun postIllustBookmarkAdd(
        @Field("illust_id") illustId: Long,
        @Field("restrict") restrict: String = Restrict.PUBLIC.value,
        @Field("tags[]") tags: List<String>? = null,
    ): EmptyResp

    @FormUrlEncoded
    @POST("v1/illust/bookmark/delete")
    suspend fun postIllustBookmarkDelete(
        @Field("illust_id") illustId: Long,
    ): EmptyResp

    @GET("v2/illust/related")
    suspend fun getIllustRelated(
        @Query("illust_id") illustId: Long,
        @Query("filter") filter: String,
    ): IllustsWithNextUrl

    @GET("v2/illust/related")
    suspend fun loadMoreIllustRelated(
        @QueryMap queryMap: Map<String, String>,
    ): IllustsWithNextUrl

    @GET("v1/illust/detail")
    suspend fun getIllustDetail(
        @Query("illust_id") illustId: Long,
        @Query("filter") filter: String,
    ): IllustDetailResp

    @GET("v2/illust/bookmark/detail")
    suspend fun getIllustBookmarkDetail(
        @Query("illust_id") illustId: Long,
    ): IllustBookmarkDetailResp

    @GET("v1/search/illust")
    suspend fun searchIllust(
        @Query("filter") filter: String = Filter.ANDROID.value,
        @Query("include_translated_tag_results") includeTranslatedTagResults: Boolean = true,
        @Query("merge_plain_keyword_results") mergePlainKeywordResults: Boolean = true,
        @Query("word") word: String,
        @Query("sort") sort: String = SearchSort.POPULAR_DESC.value,
        @Query("search_target") searchTarget: String = SearchTarget.PARTIAL_MATCH_FOR_TAGS.value,
        @Query("bookmark_num_min") bookmarkNumMin: Int? = null,
        @Query("bookmark_num_max") bookmarkNumMax: Int? = null,
        @Query("start_date") startDate: String? = null,
        @Query("end_date") endDate: String? = null,
        @Query("search_ai_type") searchAiType: Int = SearchAiType.HIDE_AI.value,
        @Query("offset") offset: Int = 0,
    ): SearchIllustResp

    @GET("v1/search/illust")
    suspend fun searchIllustNext(
        @QueryMap queryMap: Map<String, String>,
    ): SearchIllustResp

    @GET("v2/search/autocomplete")
    suspend fun searchAutoComplete(
        @Query("word") word: String,
        @Query("merge_plain_keyword_results") mergePlainKeywordResults: Boolean = true,
    ): SearchAutoCompleteResp

    @GET("v1/trending-tags/illust")
    suspend fun trendingTags(
        @Query("filter") filter: String,
    ): TrendingTagsResp

    @GET("v1/ugoira/metadata")
    suspend fun getUgoiraMetadata(
        @Query("illust_id") illustId: Long,
    ): UgoiraMetadataResp

    @GET("v2/user/detail")
    suspend fun getUserDetail(
        @Query("filter") filter: String = Filter.ANDROID.value,
        @Query("user_id") userId: Long,
    ): UserDetailResp

    @GET("v1/user/illusts")
    suspend fun getUserIllusts(
        @Query("filter") filter: String = Filter.ANDROID.value,
        @Query("user_id") userId: Long,
        @Query("type") type: String,
    ): UserIllustsResp

    @GET("v1/user/illusts")
    suspend fun getUserIllusts(
        @QueryMap queryMap: Map<String, String>
    ): UserIllustsResp

    @GET("v1/user/bookmarks/illust")
    suspend fun getUserBookmarksIllust(
        @Query("restrict") restrict: String,
        @Query("user_id") userId: Long,
        @Query("tag") tag: String? = null,
        @Query("max_bookmark_id") maxBookmarkId: Long? = null,
    ): IllustsWithNextUrl

    @GET("v1/user/bookmarks/illust")
    suspend fun loadMoreUserBookmarksIllust(
        @QueryMap queryMap: Map<String, String>,
    ): IllustsWithNextUrl

    @GET("v1/user/bookmarks/novel")
    suspend fun getUserBookmarksNovels(
        @Query("restrict") restrict: String,
        @Query("user_id") userId: Long,
        @Query("tag") tag: String = "",
    ): UserNovelsResp

    @FormUrlEncoded
    @POST("v1/user/follow/add")
    suspend fun followUser(
        @Field("user_id") userId: Long,
        @Field("restrict") restrict: String,
    ): EmptyResp

    @FormUrlEncoded
    @POST("v1/user/follow/delete")
    suspend fun unFollowUser(
        @Field("user_id") userId: Long,
    ): EmptyResp

    @GET("v1/user/browsing-history/illusts")
    suspend fun getUserBrowsingHistoryIllusts(): IllustsWithNextUrl

    @GET("v1/user/browsing-history/illusts")
    suspend fun loadMoreUserBrowsingHistoryIllusts(
        @QueryMap queryMap: Map<String, String>,
    ): IllustsWithNextUrl

    @GET("v1/user/bookmark-tags/illust")
    suspend fun getUserBookmarkTagsIllust(
        @Query("user_id") userId: Long,
        @Query("restrict") restrict: String,
    ): UserBookmarkTagsResp

    @GET("v1/user/bookmark-tags/novel")
    suspend fun getUserBookmarkTagsNovel(
        @Query("user_id") userId: Long,
        @Query("restrict") restrict: String,
    ): UserBookmarkTagsResp

    @GET("v1/user/following")
    suspend fun getUserFollowing(
        @Query("filter") filter: String = Filter.ANDROID.value,
        @Query("user_id") userId: Long,
        @Query("restrict") restrict: String = Restrict.PUBLIC.value,
        @Query("offset") offset: Int? = null,
    ): UserFollowingResp

    @GET("v1/user/following")
    suspend fun loadMoreUserFollowing(
        @QueryMap queryMap: Map<String, String>,
    ): UserFollowingResp

    @GET("v2/illust/follow")
    suspend fun getFollowingIllusts(
        @Query("restrict") restrict: String = Restrict.PUBLIC.value,
        @Query("offset") offset: Long? = null,
    ): IllustsWithNextUrl

    @GET("v1/mute/list")
    suspend fun getMuteList(): MutedResp

    @FormUrlEncoded
    @POST("v1/mute/edit")
    suspend fun postMuteSetting(
        @Field("add_user_ids[]") userIds: List<Long>? = null,
        @Field("delete_user_ids[]") deleteUserIds: List<Long>? = null,
        @Field("add_tags[]") addTags: List<String>? = null,
        @Field("delete_tags[]") deleteTags: List<String>? = null,
    ): EmptyResp
}