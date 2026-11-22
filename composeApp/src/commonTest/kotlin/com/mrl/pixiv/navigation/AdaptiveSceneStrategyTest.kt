package com.mrl.pixiv.navigation

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.metadata
import androidx.navigation3.scene.SceneStrategyScope
import androidx.navigationevent.NavigationEvent
import com.mrl.pixiv.common.compose.layout.PaneInputState
import com.mrl.pixiv.common.compose.layout.SplitPaneState
import com.mrl.pixiv.common.router.CommentType
import com.mrl.pixiv.common.router.Destination
import com.mrl.pixiv.common.router.NavigationRecord
import com.mrl.pixiv.common.router.PictureCoverSource
import com.mrl.pixiv.common.router.PictureImageKey
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNotSame
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class AdaptiveSceneStrategyTest {
    private val splitState = SplitPaneState()
    private val inputState = PaneInputState()
    private val paneTransitions = AdaptivePaneTransitionState()

    @Test
    fun fullWidthPicturesNeverBecomeDetailsEvenWithAnInvalidOwner() {
        val source = entry("list", Destination.Following(1))
        for (destination in listOf(Destination.Picture(0, "images"), Destination.PictureDeeplink(5))) {
            val picture = entry("picture", destination, "list")
            val scene = scene(listOf(source, picture), width = 2000.dp)

            assertNull(scene.source)
            assertEquals(listOf(picture), scene.entries)
            assertEquals(listOf(source), scene.previousEntries)
        }
    }

    @Test
    fun pictureCanHostSideContentOnlyAfterItsOwnLargeMinimumIsSatisfied() {
        val picture = entry("picture", Destination.PictureDeeplink(5))
        val comment = entry("comment", Destination.Comment(5, CommentType.ILLUST), "picture")
        val entries = listOf(picture, comment)

        assertNull(scene(entries, width = 1283.dp).source)
        assertSame(picture, scene(entries, width = 1284.dp).source)
    }

    @Test
    fun explicitOwnerMustExistAndCannotCrossAnotherFullWidthEntry() {
        val source = entry("list", Destination.Following(1))
        val picture = entry("picture", Destination.PictureDeeplink(5))
        val profile = entry("profile", Destination.ProfileDetail(2), "list")
        val missingOwner = entry("orphan", Destination.ProfileDetail(3), "absent")

        assertNull(scene(listOf(source, picture, profile), width = 2000.dp).source)
        assertNull(scene(listOf(source, missingOwner)).source)
        assertNull(scene(listOf(source, profile.copyWithOwner(null))).source)
    }

    @Test
    fun authenticationCannotHostASidePage() {
        val login = entry("login", Destination.LoginOption)
        val settings = entry("settings", Destination.NetworkSetting, "login")

        assertNull(scene(listOf(login, settings)).source)
    }

    @Test
    fun exactWidthAndHeightLimitsSelectSplitWithoutChangingHistory() {
        val source = entry("settings", Destination.Setting)
        val detail = entry("network", Destination.NetworkSetting, "settings")
        val entries = listOf(source, detail)

        assertNull(scene(entries, width = 803.dp).source)
        assertSame(source, scene(entries, width = 804.dp, height = 480.dp).source)
        assertNull(scene(entries, width = 804.dp, height = 479.dp).source)
        assertNull(scene(entries, width = 1200.dp, allowSplit = false).source)
        val narrow = scene(entries, width = 400.dp)
        val wide = scene(entries, width = 1200.dp)
        assertSame(detail, narrow.top)
        assertSame(detail, wide.top)
        assertEquals(narrow.previousEntries, wide.previousEntries)
        assertEquals(listOf(source, detail), entries)
    }

    @Test
    fun previousEntriesKeepEveryIntermediateRightPaneVisit() {
        val main = entry("main", Destination.Main)
        val source = entry("following", Destination.Following(1))
        val profile = entry("profile", Destination.ProfileDetail(2), "following")
        val artworks = entry("artworks", Destination.UserArtwork(2), "following")
        val entries = listOf(main, source, profile, artworks)
        val current = scene(entries)

        assertEquals(listOf(source, artworks), current.entries)
        assertEquals(listOf(main, source, profile), current.previousEntries)
        val afterBack = scene(current.previousEntries)
        assertEquals(listOf(source, profile), afterBack.entries)
        assertEquals(listOf(main, source), afterBack.previousEntries)
    }

    @Test
    fun equalDestinationsUseVisitIdentityForSceneKeysAndOwnerPairing() {
        val original = entry("list-one", Destination.Following(1))
        val repeated = entry("list-two", Destination.Following(1))
        val firstDetail = entry("detail-one", Destination.ProfileDetail(2), "list-two")
        val secondDetail = entry("detail-two", Destination.ProfileDetail(2), "list-two")
        val first = scene(listOf(original, repeated, firstDetail))
        val second = scene(listOf(original, repeated, firstDetail, secondDetail))

        assertSame(repeated, first.source)
        assertSame(repeated, second.source)
        assertNotEquals(first.key, second.key)
        assertEquals(listOf(original, repeated, firstDetail), second.previousEntries)
    }

    @Test
    fun rootHasNoPreviousEntriesAndEmptyInputFallsThrough() {
        val root = entry("main", Destination.Main)
        val strategy = strategy()
        val scope = SceneStrategyScope<NavigationRecord>()

        assertNull(with(strategy) { scope.calculateScene(emptyList()) })
        assertTrue(scene(listOf(root)).previousEntries.isEmpty())
        assertEquals(listOf(root), scene(listOf(root)).entries)
    }

    @Test
    fun resizeSuppressesOnlyTheSharedPageAndTheNextNavigationEnablesAnimations() {
        val source = entry("settings", Destination.Setting)
        val detail = entry("network", Destination.NetworkSetting, "settings")
        val nextDetail = entry("browsing", Destination.BrowsingSetting, "settings")
        val narrow = scene(listOf(source, detail), width = 400.dp)
        val wide = scene(listOf(source, detail))
        val next = scene(listOf(source, detail, nextDetail))

        paneTransitions.update(narrow, wide)
        assertTrue(paneTransitions.suppressesAnimationFor(narrow))
        assertTrue(paneTransitions.suppressesAnimationFor(wide))
        assertFalse(paneTransitions.suppressesAnimationFor(next))

        paneTransitions.update(wide, next)
        assertFalse(paneTransitions.suppressesAnimationFor(wide))
        assertFalse(paneTransitions.suppressesAnimationFor(next))
    }

    @Test
    fun predictiveBackAndCancellationAfterResizeDoNotKeepAnimationsDisabled() {
        val source = entry("settings", Destination.Setting)
        val detail = entry("network", Destination.NetworkSetting, "settings")
        val narrow = scene(listOf(source, detail), width = 400.dp)
        val wide = scene(listOf(source, detail))
        val previous = scene(listOf(source))
        paneTransitions.update(narrow, wide)

        paneTransitions.update(wide, previous)
        assertFalse(paneTransitions.suppressesAnimationFor(wide))
        assertFalse(paneTransitions.suppressesAnimationFor(previous))
        // 取消手势会重新指向原详情页面，此时仍属于导航操作，而非窗口尺寸变化。
        paneTransitions.update(previous, wide)
        assertFalse(paneTransitions.suppressesAnimationFor(wide))
        paneTransitions.update(wide, previous)
        assertFalse(paneTransitions.suppressesAnimationFor(wide))
    }

    @Test
    fun cardTransitionIsLimitedToEnabledSinglePaneNavigation() {
        val source = entry("settings", Destination.Setting)
        val detail = entry("network", Destination.NetworkSetting, "settings")
        val previous = scene(listOf(source), width = 400.dp)
        val current = scene(listOf(source, detail), width = 400.dp)
        val split = scene(listOf(source, detail))
        val disabled = AdaptivePaneTransitionState(enablePredictiveBackCard = false)
        disabled.update(current, previous, TransitionKind.Predictive, NavigationEvent.EDGE_LEFT)
        assertNull(disabled.backCardFor(current))

        val enabled = AdaptivePaneTransitionState(enablePredictiveBackCard = true)
        enabled.update(current, previous, TransitionKind.Back)
        assertNull(enabled.backCardFor(current), "A button pop must retain its original animation")
        enabled.update(current, previous, TransitionKind.Predictive, NavigationEvent.EDGE_RIGHT)
        assertNotNull(enabled.backCardFor(current))
        enabled.update(split, previous, TransitionKind.Predictive, NavigationEvent.EDGE_RIGHT)
        assertNull(enabled.backCardFor(split))
        assertNull(enabled.backCardFor(previous))
    }

    @Test
    fun sharedImageTransitionIsKeptOnlyWhenReturningToItsOpener() {
        val grid = entry("grid", Destination.Main)
        val cover = PictureCoverSource(PictureImageKey("grid", "home", 1L), "thumbnail")
        val picture = entry("picture", Destination.Picture(0, "images", cover))
        // 作品首图打开预览时沿用封面标识，entryId 指向瀑布流而不是作品页。
        val preview = entry("preview", Destination.ImagePreview(listOf("image"), 0, cover.key))
        val gridScene = scene(listOf(grid), width = 400.dp)
        val pictureScene = scene(listOf(grid, picture), width = 400.dp)
        val previewScene = scene(listOf(grid, picture, preview), width = 400.dp)

        assertTrue(pictureScene.returnsWithSharedImage(gridScene))
        assertTrue(previewScene.returnsWithSharedImage(pictureScene))
        assertFalse(previewScene.returnsWithSharedImage(gridScene), "Only the opener can receive the image")
        assertFalse(pictureScene.returnsWithSharedImage(null))

        val plainPicture = entry("plain-picture", Destination.Picture(0, "images"))
        val plainPreview = entry("plain-preview", Destination.ImagePreview(listOf("image"), 0))
        assertFalse(scene(listOf(grid, plainPicture), width = 400.dp).returnsWithSharedImage(gridScene))
        assertFalse(scene(listOf(grid, picture, plainPreview), width = 400.dp).returnsWithSharedImage(pictureScene))

        // 中等宽度下来源页可能是分栏，缩略图位于其中一栏，返回时仍由该场景接住共享图片。
        val list = entry("list", Destination.Following(1))
        val profile = entry("profile", Destination.ProfileDetail(2), "list")
        val profileCover = PictureCoverSource(PictureImageKey("profile", "works", 1L), "thumbnail")
        val fromDetail = entry("from-detail", Destination.Picture(0, "images", profileCover))
        val splitScene = scene(listOf(list, profile))
        assertNotNull(splitScene.source)
        assertTrue(scene(listOf(list, profile, fromDetail)).returnsWithSharedImage(splitScene))
        assertFalse(splitScene.returnsWithSharedImage(scene(listOf(list))), "A split detail has no shared image")
    }

    @Test
    fun cardStyleAndEdgeSurviveCommitAndCancelCallbacksUntilSceneDisposal() {
        val source = entry("settings", Destination.Setting)
        val detail = entry("network", Destination.NetworkSetting)
        val previous = scene(listOf(source), width = 400.dp)
        val current = scene(listOf(source, detail), width = 400.dp)
        val state = AdaptivePaneTransitionState(enablePredictiveBackCard = true)
        state.update(current, previous, TransitionKind.Predictive, NavigationEvent.EDGE_RIGHT)
        val card = assertNotNull(state.backCardFor(current))

        for (kind in listOf(TransitionKind.Back, TransitionKind.Forward)) {
            state.update(current, previous, kind)
            assertSame(card, state.backCardFor(current))
            assertSame(card, state.backCardFor(previous))
            assertEquals(NavigationEvent.EDGE_RIGHT, card.swipeEdge)
        }
        state.finishBackCard(card)
        assertNull(state.backCardFor(current))
        assertNull(state.backCardFor(previous))

        state.update(current, previous, TransitionKind.Predictive, NavigationEvent.EDGE_LEFT)
        val retry = assertNotNull(state.backCardFor(current))
        assertNotSame(card, retry)
        state.finishBackCard(card)
        assertSame(retry, state.backCardFor(current), "Old disposal cannot finish the next gesture")
        assertEquals(NavigationEvent.EDGE_LEFT, retry.swipeEdge)
    }

    private fun strategy(
        width: Dp = 1200.dp,
        height: Dp = 800.dp,
        allowSplit: Boolean = true,
    ) = AdaptiveSceneStrategy(width, height, splitState, inputState, paneTransitions, allowSplit)

    private fun scene(
        entries: List<NavEntry<NavigationRecord>>,
        width: Dp = 1200.dp,
        height: Dp = 800.dp,
        allowSplit: Boolean = true,
    ): AdaptiveScene = with(strategy(width, height, allowSplit)) {
        SceneStrategyScope<NavigationRecord>().calculateScene(entries) as AdaptiveScene
    }

    private fun entry(id: String, destination: Destination, owner: String? = null): NavEntry<NavigationRecord> {
        val record = NavigationRecord(id, destination, owner)
        return NavEntry(
            key = record,
            contentKey = record.entryId,
            metadata = metadata { put(NavigationRecordKey, record) },
        ) { }
    }

    private fun NavEntry<NavigationRecord>.copyWithOwner(owner: String?) =
        entry(record.entryId, record.destination, owner)
}
