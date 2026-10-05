package com.mrl.pixiv.common.datasource.remote

import com.mrl.pixiv.common.data.collection.CollectionIllustSearchResponse
import com.mrl.pixiv.common.data.collection.CollectionNovelSearchResponse
import com.mrl.pixiv.common.data.collection.CollectionPeriodOptions
import com.mrl.pixiv.common.data.collection.CollectionSyncStatus
import com.mrl.pixiv.common.data.collection.CollectionTagOptions
import de.jensklingenberg.ktorfit.http.GET
import de.jensklingenberg.ktorfit.http.QueryMap

interface CollectionSearchApi {
    @GET("v1/search/bookmark/sync-status")
    suspend fun syncStatus(): CollectionSyncStatus

    @GET("v1/search/bookmark/illust")
    suspend fun illusts(@QueryMap query: Map<String, String>): CollectionIllustSearchResponse

    @GET("v1/search/bookmark/novel")
    suspend fun novels(@QueryMap query: Map<String, String>): CollectionNovelSearchResponse

    @GET("v1/search/bookmark/illust/bookmark-tag")
    suspend fun illustBookmarkTags(@QueryMap query: Map<String, String>): CollectionTagOptions

    @GET("v1/search/bookmark/illust/illust-tag")
    suspend fun illustWorkTags(@QueryMap query: Map<String, String>): CollectionTagOptions

    @GET("v1/search/bookmark/novel/bookmark-tag")
    suspend fun novelBookmarkTags(@QueryMap query: Map<String, String>): CollectionTagOptions

    @GET("v1/search/bookmark/novel/novel-tag")
    suspend fun novelWorkTags(@QueryMap query: Map<String, String>): CollectionTagOptions

    @GET("v1/search/bookmark/illust/period")
    suspend fun illustPeriods(@QueryMap query: Map<String, String>): CollectionPeriodOptions

    @GET("v1/search/bookmark/novel/period")
    suspend fun novelPeriods(@QueryMap query: Map<String, String>): CollectionPeriodOptions
}
