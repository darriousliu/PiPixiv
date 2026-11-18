package com.mrl.pixiv.common.network

import coil3.ImageLoader
import coil3.PlatformContext
import coil3.decode.DataSource
import coil3.disk.DiskCache
import coil3.fetch.SourceFetchResult
import coil3.network.HttpException
import coil3.network.ktor3.KtorNetworkFetcherFactory
import coil3.request.Options
import coil3.toUri
import com.mrl.pixiv.common.data.setting.UserPreference.BypassSetting
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okio.Path.Companion.toPath
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.time.Duration.Companion.seconds

class ImageNetworkFetcherTest {
    @Test
    fun retryAfterChangingProxySkipsCachedFailureAndReleasesItsSnapshot() = runBlocking {
        val directory = Files.createTempDirectory("pixiv-coil-cache").toFile()
        val context = PlatformContext.INSTANCE
        val options = Options(context)
        val cache = DiskCache.Builder().directory(directory.absolutePath.toPath()).build()
        val loader = ImageLoader.Builder(context).diskCache { cache }.build()
        val url = "http://image-target.invalid/image.jpg"
        val uri = url.toUri()
        try {
            MockWebServer().use { oldProxy ->
                MockWebServer().use { newProxy ->
                    oldProxy.start()
                    newProxy.start()
                    val oldSetting = BypassSetting.Proxy(oldProxy.hostName, oldProxy.port)
                    val newSetting = BypassSetting.Proxy(newProxy.hostName, newProxy.port)
                    client(oldSetting).use { oldClient ->
                        withTimeout(10.seconds) {
                            oldProxy.enqueue(MockResponse(code = 404, body = "missing"))
                            val oldFactory = KtorNetworkFetcherFactory(oldClient)
                            val oldFetcher = assertNotNull(oldFactory.create(uri, options, loader))
                            assertEquals(404, assertFailsWith<HttpException> { oldFetcher.fetch() }.response.code)
                            assertNotNull(cache.openSnapshot(url)).close()

                            client(newSetting).use { newClient ->
                                val clients = RoutedHttpClient({ newSetting }, { newClient }, HttpClient::close)
                                val factory = imageNetworkFetcher(clients)
                                newProxy.enqueue(MockResponse(code = 503, body = "unavailable"))
                                val failed = assertNotNull(factory.create(uri, options, loader))
                                assertEquals(503, assertFailsWith<HttpException> { failed.fetch() }.response.code)
                                assertNotNull(cache.openEditor(url)).abort()

                                newProxy.enqueue(MockResponse(body = "image data"))
                                val fetched = assertNotNull(factory.create(uri, options, loader)).fetch() as SourceFetchResult
                                assertEquals(DataSource.NETWORK, fetched.dataSource)
                                assertEquals("image data", fetched.source.use { it.source().readUtf8() })

                                val cached = assertNotNull(factory.create(uri, options, loader)).fetch() as SourceFetchResult
                                assertEquals(DataSource.DISK, cached.dataSource)
                                cached.source.close()
                                assertEquals(1, oldProxy.requestCount)
                                assertEquals(2, newProxy.requestCount)
                            }
                        }
                    }
                }
            }
        } finally {
            loader.shutdown()
            cache.shutdown()
            directory.deleteRecursively()
        }
    }

    private fun client(setting: BypassSetting) = HttpClient(OkHttp) { configureNetworkProxy(setting) }
}
