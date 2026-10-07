package com.mrl.pixiv.picture

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.paging.compose.collectAsLazyPagingItems
import com.ctrip.flight.mmkv.MMKVCLibLoader
import com.ctrip.flight.mmkv.MMKVLogLevel
import com.ctrip.flight.mmkv.initialize
import com.mrl.pixiv.common.data.Illust
import com.mrl.pixiv.common.data.ImageUrls
import com.mrl.pixiv.common.data.MetaSinglePage
import com.mrl.pixiv.common.data.Type
import com.mrl.pixiv.common.data.User
import com.mrl.pixiv.common.data.user.IllustsWithNextUrl
import com.mrl.pixiv.common.data.user.UserIllustsResp
import com.mrl.pixiv.common.datasource.local.dao.BrowsingHistoryDao
import com.mrl.pixiv.common.network.ApiClient
import com.mrl.pixiv.common.network.AuthClient
import com.mrl.pixiv.common.network.ImageClient
import com.mrl.pixiv.common.repository.BrowsingHistoryRepository
import com.mrl.pixiv.common.util.JvmZipUtil
import com.mrl.pixiv.common.util.ToastUtil
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.PlatformFile
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.swing.Swing
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.module.dsl.viewModel
import org.koin.core.parameter.parametersOf
import org.koin.core.qualifier.named
import org.koin.dsl.module
import java.io.File
import java.lang.reflect.Proxy
import java.nio.file.Files
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotSame
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalTestApi::class, ExperimentalCoroutinesApi::class)
class PicturePagerViewModelTest {
    @Test
    fun swipingChangesBothPreviewsAndDisposedPagesReleaseTheirPagingScope() {
        // PixivRepository retains its clients in an object: exercise all cases with one Koin setup.
        val directory = Files.createTempDirectory("picture-pager-test").toFile()
        initializeStorage(directory)
        val works = (0..8).map { index ->
            illust(id = 100L + index, author = if (index == 2) 11 else 10L + index)
        }.toMutableList().apply {
            this[6] = this[0]
            this[7] = this[0]
        }.toImmutableList()
        val creations = CopyOnWriteArrayList<PictureViewModel>()
        val requests = CopyOnWriteArrayList<Pair<String, Long>>()
        val cancelledRequests = CopyOnWriteArrayList<Long>()
        val pendingRelatedId = works.last().id
        val saveStarted = CompletableDeferred<Unit>()
        val saveGate = CompletableDeferred<Unit>()
        val saveCancelled = AtomicBoolean(false)
        val savedImage = directory.resolve("saved-image.jpg")
        val imageBytes = byteArrayOf(1, 2, 3, 4)
        var savingModel: PictureViewModel? = null
        val imageClient = HttpClient(MockEngine { request ->
            assertEquals("/test-save.jpg", request.url.encodedPath)
            saveStarted.complete(Unit)
            try {
                saveGate.await()
            } catch (error: kotlinx.coroutines.CancellationException) {
                saveCancelled.set(true)
                throw error
            }
            respond(imageBytes, headers = headersOf(HttpHeaders.ContentType, "image/jpeg"))
        })
        val json = Json { ignoreUnknownKeys = true }
        val client = HttpClient(MockEngine { request ->
            val path = request.url.encodedPath
            val id = request.url.parameters[if (path == "/v1/user/illusts") "user_id" else "illust_id"]!!.toLong()
            requests += path to id
            val body = when (path) {
                "/v1/user/illusts" -> json.encodeToString(UserIllustsResp(illusts = listOf(illust(1_000 + id, id))))
                "/v2/illust/related" -> {
                    if (id == pendingRelatedId) {
                        try { awaitCancellation() } finally { cancelledRequests += id }
                    }
                    json.encodeToString(IllustsWithNextUrl(listOf(illust(2_000 + id, 99))))
                }
                else -> error("Unexpected request: ${request.url}")
            }
            respond(body, headers = headersOf(HttpHeaders.ContentType, "application/json"))
        }) { install(ContentNegotiation) { json(json) } }
        val rootOwner = object : ViewModelStoreOwner {
            override val viewModelStore = ViewModelStore()
        }
        val parentSentinel = object : ViewModel() {}
        val history = BrowsingHistoryRepository(unusedHistoryDao())
        startKoin {
            modules(module {
                single<HttpClient>(named<ApiClient>()) { client }
                single<HttpClient>(named<AuthClient>()) { client }
                single<HttpClient>(named<ImageClient>()) { imageClient }
                viewModel {
                    parameters -> PictureViewModel(
                        parameters.getOrNull<Illust>(), parameters.getOrNull<Long>(), JvmZipUtil(), history,
                    ).also { creations += it }
                }
            })
        }
        Dispatchers.setMain(Dispatchers.Swing)
        // Consume the real success toast so its rendezvous Channel sender can finish.
        val toastScope = CoroutineScope(Dispatchers.Swing)
        val toastReceived = CompletableDeferred<Unit>()
        toastScope.launch { ToastUtil.toastFlow.collect { toastReceived.complete(Unit) } }
        try {
            runDesktopComposeUiTest(width = 480, height = 320, testTimeout = 60.seconds) {
                val phase = mutableStateOf(Phase.DefaultKeyProbe)
                val visibleModels = ConcurrentHashMap<Long, PictureViewModel>()
                val visibleRelatedIds = ConcurrentHashMap<Long, List<Long>>()
                lateinit var probeA: PictureViewModel
                lateinit var probeB: PictureViewModel
                lateinit var pager: PagerState
                lateinit var selectPage: (Int) -> Unit
                setContent {
                    CompositionLocalProvider(LocalViewModelStoreOwner provides rootOwner) {
                        when (phase.value) {
                            Phase.DefaultKeyProbe -> {
                                probeA = koinViewModel { parametersOf(works[0], null) }
                                probeB = koinViewModel { parametersOf(works[1], null) }
                            }
                            Phase.KeyedProbe -> {
                                probeA = rememberPictureViewModel(works[0])
                                probeB = rememberPictureViewModel(works[1])
                            }
                            Phase.Pager -> {
                                pager = rememberPagerState { works.size }
                                val scope = rememberCoroutineScope()
                                selectPage = { index -> scope.launch { pager.scrollToPage(index) } }
                                PicturePager(works, pager, Modifier.fillMaxSize().testTag("picture-pager")) { work ->
                                    val model = rememberPictureViewModel(work)
                                    val state by model.uiState.collectAsState()
                                    val related = model.relatedIllusts.collectAsLazyPagingItems()
                                    SideEffect {
                                        visibleModels[work.id] = model
                                        visibleRelatedIds[work.id] = related.itemSnapshotList.items.map { it.id }
                                    }
                                    Column(Modifier.fillMaxSize()) {
                                        Text("work:${work.id}")
                                        Text("author:${work.id}:${state.userIllusts.joinToString { it.id.toString() }}")
                                        Text("related:${work.id}:${related.itemSnapshotList.items.joinToString { it.id.toString() }}")
                                    }
                                }
                            }
                            Phase.Removed -> Unit
                        }
                    }
                }
                waitForIdle()
                runOnIdle {
                    assertSame(probeA, probeB, "Default Koin key ignores changed constructor parameters in the same owner")
                    assertEquals(1, creations.size, "B must not call the factory when A already occupies the default key")
                    assertEquals(works[0].id, probeB.uiState.value.illust?.id)
                    rootOwner.viewModelStore.clear()
                    phase.value = Phase.KeyedProbe
                }
                waitForIdle()
                runOnIdle {
                    assertNotSame(probeA, probeB, "An illust ID key is sufficient to isolate instances in one owner")
                    assertEquals(works[1].id, probeB.uiState.value.illust?.id)
                    rootOwner.viewModelStore.clear()
                    rootOwner.viewModelStore.put("parent-sentinel", parentSentinel)
                    phase.value = Phase.Pager
                }
                waitUntil(timeoutMillis = 10_000) { visibleModels[100]?.uiState?.value?.userIllusts?.firstOrNull()?.id == 1_010L }
                waitUntil(timeoutMillis = 10_000) { visibleRelatedIds[100] == listOf(2100L) }
                waitForIdle()
                onNodeWithText("author:100:1010").assertIsDisplayed()
                onNodeWithText("related:100:2100").assertIsDisplayed()
                val firstA = visibleModels.getValue(100)

                // A -> B uses real pointer input and the production pager, with different authors.
                onNodeWithTag("picture-pager").performTouchInput { swipeLeft() }
                waitUntil(timeoutMillis = 10_000) { pager.settledPage == 1 && visibleModels[101]?.uiState?.value?.userIllusts?.isNotEmpty() == true }
                waitUntil(timeoutMillis = 10_000) { visibleRelatedIds[101] == listOf(2101L) }
                waitForIdle()
                onNodeWithText("author:101:1011").assertIsDisplayed()
                onNodeWithText("related:101:2101").assertIsDisplayed()
                assertNotSame(firstA, visibleModels.getValue(101))

                // B -> C keeps the author but must still change the related-work source.
                onNodeWithTag("picture-pager").performTouchInput { swipeLeft() }
                waitUntil(timeoutMillis = 10_000) { pager.settledPage == 2 && visibleModels[102]?.uiState?.value?.userIllusts?.isNotEmpty() == true }
                waitUntil(timeoutMillis = 10_000) { visibleRelatedIds[102] == listOf(2102L) }
                waitForIdle()
                onNodeWithText("author:102:1011").assertIsDisplayed()
                onNodeWithText("related:102:2102").assertIsDisplayed()

                onNodeWithTag("picture-pager").performTouchInput { swipeRight() }
                waitUntil(timeoutMillis = 10_000) { pager.settledPage == 1 }
                onNodeWithTag("picture-pager").performTouchInput { swipeRight() }
                waitUntil(timeoutMillis = 10_000) { pager.settledPage == 0 }
                waitForIdle()
                onNodeWithText("author:100:1010").assertIsDisplayed()
                onNodeWithText("related:100:2100").assertIsDisplayed()
                val returnedA = visibleModels.getValue(100)

                // A confirmed Save As operation outlives A's disposable page scope.
                runOnIdle {
                    savingModel = returnedA
                    returnedA.saveAsImage("https://fixture.invalid/test-save.jpg", PlatformFile(savedImage))
                }
                waitUntil(timeoutMillis = 10_000) { saveStarted.isCompleted }

                // A distant page evicts A; cachedIn must die with that page's ViewModelStore.
                runOnIdle { selectPage(works.lastIndex) }
                waitUntil(timeoutMillis = 10_000) { pager.settledPage == works.lastIndex && !returnedA.scopeJob().isActive }
                waitUntil(timeoutMillis = 10_000) { requests.contains("/v2/illust/related" to pendingRelatedId) }
                assertFalse(returnedA.scopeJob().isActive)
                assertTrue(returnedA.scopeJob().children.none { it.isActive })
                val lastModel = visibleModels.getValue(pendingRelatedId)
                assertTrue(lastModel.scopeJob().isActive)

                runOnIdle { selectPage(0) }
                waitUntil(timeoutMillis = 10_000) { pager.settledPage == 0 && visibleModels[100] !== returnedA && visibleModels[100]?.uiState?.value?.userIllusts?.isNotEmpty() == true }
                waitUntil(timeoutMillis = 10_000) { visibleRelatedIds[100] == listOf(2100L) }
                waitUntil(timeoutMillis = 10_000) { cancelledRequests.contains(pendingRelatedId) }
                assertFalse(saveCancelled.get(), "Evicting A must not cancel its confirmed file save")
                saveGate.complete(Unit)
                waitUntil(timeoutMillis = 10_000) {
                    savedImage.exists() && savedImage.length() == imageBytes.size.toLong() && !returnedA.uiState.value.loading
                }
                assertContentEquals(imageBytes, savedImage.readBytes())
                assertFalse(saveCancelled.get())
                waitUntil(timeoutMillis = 10_000) { toastReceived.isCompleted }
                waitForIdle()
                onNodeWithText("author:100:1010").assertIsDisplayed()
                onNodeWithText("related:100:2100").assertIsDisplayed()
                assertNotSame(returnedA, visibleModels.getValue(100), "An evicted page is recreated with its own sources")

                val rebuiltA = visibleModels.getValue(100)

                // Feed snapshots may contain the same work twice; both page slots remain valid.
                runOnIdle { selectPage(6) }
                waitUntil(timeoutMillis = 10_000) { pager.settledPage == 6 && visibleModels[100]?.uiState?.value?.userIllusts?.isNotEmpty() == true }
                waitUntil(timeoutMillis = 10_000) { visibleRelatedIds[100] == listOf(2100L) }
                waitForIdle()
                onNodeWithTag("picture-pager").performTouchInput { swipeLeft() }
                waitUntil(timeoutMillis = 10_000) { pager.settledPage == 7 }
                waitForIdle()
                // The neighboring duplicate can remain composed, so match a visible preview.
                listOf("author:100:1010", "related:100:2100").forEach { text ->
                    assertTrue(onAllNodesWithText(text).fetchSemanticsNodes().any {
                        it.boundsInRoot.overlaps(Rect(0f, 0f, 480f, 320f))
                    })
                }

                runOnIdle { selectPage(0) }
                waitUntil(timeoutMillis = 10_000) { pager.settledPage == 0 && visibleModels[100] !== rebuiltA && visibleModels[100]?.uiState?.value?.userIllusts?.isNotEmpty() == true }
                waitUntil(timeoutMillis = 10_000) { visibleRelatedIds[100] == listOf(2100L) }
                waitForIdle()
                onNodeWithText("author:100:1010").assertIsDisplayed()
                onNodeWithText("related:100:2100").assertIsDisplayed()

                runOnIdle { phase.value = Phase.Removed }
                waitUntil(timeoutMillis = 10_000) { creations.all { !it.scopeJob().isActive } }
                runOnIdle {
                    assertTrue(creations.all { it.scopeJob().children.none { child -> child.isActive } })
                    // The navigation owner's lifetime continues after pager removal.
                    assertTrue(requireNotNull(parentSentinel.viewModelScope.coroutineContext[Job]).isActive)
                }
            }
        } finally {
            // Release the process-scoped request even if an earlier UI assertion fails.
            saveGate.complete(Unit)
            try {
                runBlocking {
                    withTimeout(10_000) {
                        if (savingModel != null) {
                            saveStarted.await()
                            savingModel.uiState.first { !it.loading }
                            toastReceived.await()
                        }
                    }
                }
            } finally {
                toastScope.cancel()
                rootOwner.viewModelStore.clear()
                Dispatchers.resetMain()
                stopKoin()
                client.close()
                imageClient.close()
                directory.deleteRecursively()
            }
        }
    }

    private fun initializeStorage(directory: File) {
        FileKit.init(filesDir = directory.resolve("files"), cacheDir = directory.resolve("cache"))
        val name = when {
            System.getProperty("os.name") == "Mac OS X" -> "libmmkvc.dylib"
            System.getProperty("os.name").startsWith("Windows") -> "mmkvc.dll"
            else -> "libmmkvc.so"
        }
        val library = directory.resolve(name)
        requireNotNull(javaClass.classLoader.getResourceAsStream(name)).use { input -> library.outputStream().use(input::copyTo) }
        initialize(directory.resolve("mmkv").absolutePath, MMKVCLibLoader { library.absolutePath }, MMKVLogLevel.LevelNone)
    }

    private fun unusedHistoryDao(): BrowsingHistoryDao = Proxy.newProxyInstance(
        BrowsingHistoryDao::class.java.classLoader, arrayOf(BrowsingHistoryDao::class.java),
    ) { _, method, _ -> error("Unexpected history access: ${method.name}") } as BrowsingHistoryDao

    private fun PictureViewModel.scopeJob(): Job = requireNotNull(viewModelScope.coroutineContext[Job])

    private fun illust(id: Long, author: Long) = Illust(
        id = id, title = "Work $id", type = Type.Illust, imageUrls = ImageUrls(),
        user = User(id = author), width = 1, height = 1, metaSinglePage = MetaSinglePage(),
        totalView = 0, totalBookmarks = 0, isBookmarked = false,
    )

    private enum class Phase { DefaultKeyProbe, KeyedProbe, Pager, Removed }
}
