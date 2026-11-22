package com.mrl.pixiv.common.repository

import com.mrl.pixiv.common.datasource.remote.ReadingApi
import com.mrl.pixiv.common.repository.util.queryParams
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

object ReadingRepository {
    private suspend fun <T> withApi(block: suspend (ReadingApi) -> T): T =
        PixivRepository.withApiServices { block(it.reading) }

    private val watchlistMutex = Mutex()
    private val _watchlistChanges = MutableSharedFlow<Pair<Long, Boolean>>(extraBufferCapacity = 8)
    val watchlistChanges = _watchlistChanges.asSharedFlow()

    suspend fun mangaSeries(seriesId: Long) = withApi { it.mangaSeries(seriesId) }
    suspend fun mangaSeriesNext(nextUrl: String) = withApi { it.mangaSeriesNext(nextUrl.queryParams) }
    suspend fun mangaContext(illustId: Long) = withApi { it.mangaContext(illustId) }
    suspend fun userMangaSeries(userId: Long) = withApi { it.userMangaSeries(userId) }
    suspend fun userMangaSeriesNext(nextUrl: String) = withApi { it.userMangaSeriesNext(nextUrl.queryParams) }
    suspend fun mangaWatchlist() = withApi { it.mangaWatchlist() }
    suspend fun mangaWatchlistNext(nextUrl: String) = withApi { it.mangaWatchlistNext(nextUrl.queryParams) }

    suspend fun setMangaWatchlist(seriesId: Long, added: Boolean) = watchlistMutex.withLock {
        require(seriesId > 0)
        withApi { api ->
            if (added) api.addMangaWatchlist(seriesId) else api.deleteMangaWatchlist(seriesId)
        }
        _watchlistChanges.emit(seriesId to added)
    }
}
