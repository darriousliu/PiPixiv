package com.mrl.pixiv.common.router

import androidx.compose.runtime.saveable.SaverScope
import com.mrl.pixiv.common.data.AppViewMode
import com.mrl.pixiv.common.data.Type
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class NavigationSerializationTest {
    private val saverScope = SaverScope { true }

    @Test
    fun commentStateCanBeSavedAndRestoredForBothContentTypes() {
        CommentType.entries.forEach { type ->
            assertRoundTrip(Destination.Comment(123, type))
        }
    }

    @Test
    fun reportStateCanBeSavedAndRestoredForEveryReportType() {
        ReportType.entries.forEach { type ->
            assertRoundTrip(Destination.Report(456, type))
        }
    }

    @Test
    fun restoringACommentAndReportBranchKeepsItsOwnerAndBackNavigation() {
        val navigation = NavigationManager(Destination.Main)
        navigation.switchMainPage(MainPage.Ranking)
        navigation.navigateToNovelDetailScreen(123)
        navigation.navigateToCommentScreen(123, CommentType.NOVEL)
        navigation.navigateToReportCommentScreen(456, ReportType.NOVEL_COMMENT)
        val snapshot = navigation.saveState()

        // 与后台保存导航状态相同，按基类序列化整个访问记录，不能只测试具体子类。
        val encoded = Json.encodeToString(NavigationStateSnapshot.serializer(), snapshot)
        val restored = NavigationManager()
        restored.restoreState(Json.decodeFromString(NavigationStateSnapshot.serializer(), encoded))

        assertEquals(snapshot, restored.saveState())
        restored.popBackStack()
        assertEquals(Destination.Comment(123, CommentType.NOVEL), restored.currentDestination)
        restored.popBackStack()
        assertEquals(Destination.NovelDetail(123), restored.currentDestination)
        assertEquals(MainPage.Ranking, restored.currentMainPage)
    }

    @OptIn(ExperimentalSerializationApi::class)
    @Test
    fun everyDestinationCanBeSavedAndRestoredWithTheDefaultJsonDiscriminator() {
        val destinations = listOf(
            Destination.LoginOption,
            Destination.Login("https://example.test/login"),
            Destination.OAuthLogin,
            Destination.WebCookieLogin,
            Destination.Main,
            Destination.ProfileDetail(1),
            Destination.Picture(2, "pictures", true),
            Destination.PictureDeeplink(3),
            Destination.ImagePreview(listOf("image"), 0, "shared"),
            Destination.Search,
            Destination.SearchResults("query", true, AppViewMode.NOVEL),
            Destination.Setting,
            Destination.NetworkSetting,
            Destination.BrowsingSetting,
            Destination.SearchSetting,
            Destination.HistorySetting,
            Destination.PrivacySetting,
            Destination.FileNameFormat,
            Destination.AiTranslationSetting,
            Destination.History,
            Destination.NovelReadLater,
            Destination.Collection(4, true),
            Destination.BookmarkedTags,
            Destination.NovelMarkers,
            Destination.Following(5),
            Destination.UserArtwork(6, Type.Illust),
            Destination.UserNovels(7),
            Destination.BlockSettings,
            Destination.BlockIllust,
            Destination.BlockNovel,
            Destination.BlockUser,
            Destination.BlockTag,
            Destination.BlockComments,
            Destination.AppData,
            Destination.Download,
            Destination.About,
            Destination.Comment(8, CommentType.ILLUST),
            Destination.Report(9, ReportType.USER),
            Destination.NovelDetail(10, markerPage = 3, readLaterTargetLanguage = "zh-CN"),
            Destination.NovelSeries(11),
        )
        // 新增路由时必须补全样本，防止其他业务字段再次与判别字段冲突。
        val subtypes = Destination.serializer().descriptor.getElementDescriptor(1)
        assertEquals(subtypes.elementsCount, destinations.map { it::class }.toSet().size)
        destinations.forEach(::assertRoundTrip)
    }

    @Test
    fun navigationJsonSavedBeforeTheFixRemainsReadable() {
        val saved = """
            {"records":[
              {"entryId":"main","destination":{"type":"com.mrl.pixiv.common.router.Destination.Main"}},
              {"entryId":"novel","destination":{"type":"com.mrl.pixiv.common.router.Destination.NovelDetail","novelId":123},"ownerEntryId":"main"}
            ],"currentMainPage":{"type":"com.mrl.pixiv.common.router.MainPage.Profile"}}
        """.trimIndent()
        val restored = NavigationManager()
        restored.restoreState(assertNotNull(NavigationStateSnapshotSaver.restore(saved)))

        assertEquals(Destination.NovelDetail(123), restored.currentDestination)
        assertEquals("main", restored.backStack.last().ownerEntryId)
        assertEquals(MainPage.Profile, restored.currentMainPage)
    }

    private fun assertRoundTrip(destination: Destination) {
        val snapshot = NavigationStateSnapshot(
            listOf(NavigationRecord("entry", destination)),
            MainPage.Home,
        )
        val encoded = Json.encodeToString(NavigationStateSnapshot.serializer(), snapshot)
        assertEquals(snapshot, Json.decodeFromString(NavigationStateSnapshot.serializer(), encoded))
        val saved = with(NavigationStateSnapshotSaver) {
            assertNotNull(saverScope.save(snapshot))
        }
        assertEquals(snapshot, NavigationStateSnapshotSaver.restore(saved))
    }
}
