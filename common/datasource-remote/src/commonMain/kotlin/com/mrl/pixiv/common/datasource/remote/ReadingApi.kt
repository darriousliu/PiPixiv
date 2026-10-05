package com.mrl.pixiv.common.datasource.remote

import com.mrl.pixiv.common.data.EmptyResp
import com.mrl.pixiv.common.data.manga.MangaSeriesResp
import com.mrl.pixiv.common.data.manga.MangaSeriesContextResp
import com.mrl.pixiv.common.data.manga.MangaWatchlistResp
import com.mrl.pixiv.common.data.manga.UserMangaSeriesResp
import com.mrl.pixiv.common.data.novel.NovelPollResponse
import com.mrl.pixiv.common.data.novel.NovelRelatedResp
import de.jensklingenberg.ktorfit.http.Field
import de.jensklingenberg.ktorfit.http.FieldMap
import de.jensklingenberg.ktorfit.http.FormUrlEncoded
import de.jensklingenberg.ktorfit.http.GET
import de.jensklingenberg.ktorfit.http.POST
import de.jensklingenberg.ktorfit.http.Query
import de.jensklingenberg.ktorfit.http.QueryMap

interface ReadingApi {
    @GET("v1/illust/series?filter=for_android")
    suspend fun mangaSeries(@Query("illust_series_id") seriesId: Long): MangaSeriesResp

    @GET("v1/illust/series")
    suspend fun mangaSeriesNext(@QueryMap params: Map<String, String>): MangaSeriesResp

    @GET("v1/illust-series/illust?filter=for_android")
    suspend fun mangaContext(@Query("illust_id") illustId: Long): MangaSeriesContextResp

    @GET("v1/user/illust-series")
    suspend fun userMangaSeries(@Query("user_id") userId: Long): UserMangaSeriesResp

    @GET("v1/user/illust-series")
    suspend fun userMangaSeriesNext(@QueryMap params: Map<String, String>): UserMangaSeriesResp

    @GET("v1/watchlist/manga")
    suspend fun mangaWatchlist(): MangaWatchlistResp

    @GET("v1/watchlist/manga")
    suspend fun mangaWatchlistNext(@QueryMap params: Map<String, String>): MangaWatchlistResp

    @FormUrlEncoded
    @POST("v1/watchlist/manga/add")
    suspend fun addMangaWatchlist(@Field("series_id") seriesId: Long): EmptyResp

    @FormUrlEncoded
    @POST("v1/watchlist/manga/delete")
    suspend fun deleteMangaWatchlist(@Field("series_id") seriesId: Long): EmptyResp

    // params is supplied by the official web reader's requestRelatedWorks message.
    // Do not construct an undocumented seed parameter from a novel ID.
    @FormUrlEncoded
    @POST("v1/novel/related")
    suspend fun novelRelated(
        @FieldMap params: Map<String, String>,
        @Field("read_novel_ids[]") readNovelIds: List<Long>,
        @Field("view_novel_ids[]") viewNovelIds: List<Long>,
        @Field("read_novel_datetimes[]") readNovelDatetimes: List<String>,
        @Field("view_novel_datetimes[]") viewNovelDatetimes: List<String>,
    ): NovelRelatedResp

    @GET("v1/novel/related")
    suspend fun novelRelatedNext(@QueryMap params: Map<String, String>): NovelRelatedResp

    @FormUrlEncoded
    @POST("v1/novel/poll/answer")
    suspend fun answerNovelPoll(
        @Field("novel_id") novelId: Long,
        @Field("choice_id") choiceId: Int,
    ): NovelPollResponse
}
