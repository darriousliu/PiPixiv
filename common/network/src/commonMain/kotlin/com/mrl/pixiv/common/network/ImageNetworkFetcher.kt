package com.mrl.pixiv.common.network

import coil3.annotation.ExperimentalCoilApi
import coil3.network.CacheStrategy
import coil3.network.HttpException
import coil3.network.NetworkClient
import coil3.network.NetworkFetcher
import coil3.network.NetworkRequest
import coil3.network.NetworkResponse
import coil3.network.ktor3.asNetworkClient
import coil3.request.Options
import io.ktor.client.HttpClient

@OptIn(ExperimentalCoilApi::class)
fun imageNetworkFetcher(clients: RoutedHttpClient<HttpClient>): NetworkFetcher.Factory =
    NetworkFetcher.Factory(
        networkClient = {
            object : NetworkClient {
                override suspend fun <T> executeRequest(
                    request: NetworkRequest,
                    block: suspend (NetworkResponse) -> T,
                ): T = clients.withClient { client -> client.asNetworkClient().executeRequest(request, block) }
            }
        },
        cacheStrategy = { SuccessfulImageResponses },
    )

@OptIn(ExperimentalCoilApi::class)
private object SuccessfulImageResponses : CacheStrategy {
    override suspend fun read(
        cacheResponse: NetworkResponse,
        networkRequest: NetworkRequest,
        options: Options,
    ): CacheStrategy.ReadResult =
        if (cacheResponse.code in 200..299) {
            CacheStrategy.ReadResult(cacheResponse)
        } else {
            CacheStrategy.ReadResult(networkRequest)
        }

    override suspend fun write(
        cacheResponse: NetworkResponse?,
        networkRequest: NetworkRequest,
        networkResponse: NetworkResponse,
        options: Options,
    ): CacheStrategy.WriteResult {
        if (networkResponse.code !in 200..299) {
            // Throw while Coil still holds the old snapshot so its failure path closes it.
            throw HttpException(networkResponse)
        }
        return CacheStrategy.WriteResult(networkResponse)
    }
}
