package com.mrl.pixiv.common.repository

import com.mrl.pixiv.common.datasource.remote.createReadingApi
import com.mrl.pixiv.common.repository.util.queryParams
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

object ReadingRepository {
    private val api by lazy { PixivRepository.apiKtorfit.createReadingApi() }
    private val publicNovels by lazy { PublicNovelClient(createPublicNovelHttpClient()) }
    private val watchlistMutex = Mutex()
    private val _watchlistChanges = MutableSharedFlow<Pair<Long, Boolean>>(extraBufferCapacity = 8)
    val watchlistChanges = _watchlistChanges.asSharedFlow()

    suspend fun mangaSeries(seriesId: Long) = api.mangaSeries(seriesId)
    suspend fun mangaSeriesNext(nextUrl: String) = api.mangaSeriesNext(nextUrl.queryParams)
    suspend fun mangaContext(illustId: Long) = api.mangaContext(illustId)
    suspend fun userMangaSeries(userId: Long) = api.userMangaSeries(userId)
    suspend fun userMangaSeriesNext(nextUrl: String) = api.userMangaSeriesNext(nextUrl.queryParams)
    suspend fun mangaWatchlist() = api.mangaWatchlist()
    suspend fun mangaWatchlistNext(nextUrl: String) = api.mangaWatchlistNext(nextUrl.queryParams)

    suspend fun publicNovelRecommendations(novelId: Long) = publicNovels.recommendations(novelId)
    suspend fun publicNovelRecommendationPage(ids: List<String>) = publicNovels.recommendationPage(ids)
    suspend fun publicNovelPoll(novelId: Long) = publicNovels.poll(novelId).pollData
    suspend fun answerNovelPoll(novelId: Long, choiceId: Int) = api.answerNovelPoll(novelId, choiceId).pollData

    suspend fun setMangaWatchlist(seriesId: Long, added: Boolean) = watchlistMutex.withLock {
        require(seriesId > 0)
        if (added) api.addMangaWatchlist(seriesId) else api.deleteMangaWatchlist(seriesId)
        _watchlistChanges.emit(seriesId to added)
    }
}
