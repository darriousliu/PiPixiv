package com.mrl.pixiv.common.router

import androidx.compose.ui.unit.dp
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class NavigationManagerTest {
    @Test
    fun rightPaneDrillsIntoListsAndReturnsOneVisitAtATime() {
        val navigation = NavigationManager(Destination.Main)
        navigation.navigateToFollowingScreen(1)
        val source = navigation.backStack.last()
        navigation.forEntry(source.entryId).navigateToProfileDetailScreen(2)
        val profile = navigation.backStack.last()

        navigation.forEntry(profile.entryId).navigateToUserIllustScreen(2)

        assertEquals(source.entryId, profile.ownerEntryId)
        assertEquals(source.entryId, navigation.backStack.last().ownerEntryId)
        assertEquals(Destination.UserArtwork(2), navigation.currentDestination)
        navigation.popBackStack()
        assertEquals(profile, navigation.backStack.last())
        navigation.popBackStack()
        assertEquals(source, navigation.backStack.last())
    }

    @Test
    fun selectingAgainFromTheSourceReplacesItsEntireDetailBranch() {
        val navigation = NavigationManager(Destination.Main, Destination.Following(1))
        val source = navigation.backStack.last()
        val left = navigation.forEntry(source.entryId)
        left.navigateToProfileDetailScreen(2)
        val firstProfile = navigation.backStack.last()
        navigation.forEntry(firstProfile.entryId).navigateToUserNovelsScreen(2)

        left.navigateToProfileDetailScreen(3)

        assertEquals(3, navigation.backStack.size)
        assertEquals(source, navigation.backStack[1])
        assertEquals(Destination.ProfileDetail(3), navigation.currentDestination)
        assertEquals(source.entryId, navigation.backStack.last().ownerEntryId)
        assertFalse(navigation.backStack.any { it.entryId == firstProfile.entryId })
    }

    @Test
    fun equalDestinationsStillHaveIndependentVisitIdentities() {
        val navigation = NavigationManager(Destination.Following(1))
        navigation.navigateToProfileDetailScreen(2)
        val first = navigation.backStack.last()
        navigation.forEntry(first.entryId).navigateToProfileDetailScreen(2)
        val second = navigation.backStack.last()

        assertEquals(first.destination, second.destination)
        assertNotEquals(first.entryId, second.entryId)
        assertEquals(first.ownerEntryId, second.ownerEntryId)
        navigation.popBackStack()
        assertEquals(first, navigation.backStack.last())
    }

    @Test
    fun pictureFromEitherPanePreservesTheWholePreviousContext() {
        for (openFromSource in listOf(true, false)) {
            val navigation = NavigationManager(Destination.Following(1))
            val source = navigation.backStack.last()
            navigation.navigateToProfileDetailScreen(2)
            val previousContext = navigation.backStack
            val origin = if (openFromSource) source else navigation.backStack.last()

            navigation.forEntry(origin.entryId).navigateToSinglePictureScreen(99)

            assertNull(navigation.backStack.last().ownerEntryId)
            assertEquals(previousContext, navigation.backStack.dropLast(1))
            navigation.popBackStack()
            assertEquals(previousContext, navigation.backStack)
        }
    }

    @Test
    fun pictureOwnsItsSidePagesAndLeavesThePriorSplitBoundaryIntact() {
        val navigation = NavigationManager(Destination.Following(1))
        navigation.navigateToProfileDetailScreen(2)
        val priorSplit = navigation.backStack
        navigation.navigate(Destination.Picture(0, "pictures", false))
        val picture = navigation.backStack.last()
        val pictureNavigation = navigation.forEntry(picture.entryId)

        pictureNavigation.navigateToCommentScreen(99, CommentType.ILLUST)

        assertNull(picture.ownerEntryId)
        assertEquals(picture.entryId, navigation.backStack.last().ownerEntryId)
        assertEquals(840.dp, picture.destination.paneSpec.minSourceWidth)
        assertTrue(picture.destination.paneSpec.preferredFullWidth)
        assertFalse(picture.destination.paneSpec.canShowAsDetail)
        pictureNavigation.popBackStack()
        assertEquals(priorSplit, navigation.backStack)
    }

    @Test
    fun sourceContentChangesCloseOnlyItsOwnActiveBranch() {
        val navigation = NavigationManager(Destination.Main)
        navigation.navigateToSinglePictureScreen(99)
        val picture = navigation.backStack.last()
        val pictureNavigation = navigation.forEntry(picture.entryId)
        pictureNavigation.navigateToCommentScreen(99, CommentType.ILLUST)
        navigation.navigateToProfileDetailScreen(2)

        pictureNavigation.closeCurrentDetailBranch()

        assertEquals(picture, navigation.backStack.last())
        assertEquals(2, navigation.backStack.size)
    }

    @Test
    fun closingAHiddenBranchDoesNotDeleteTheFullWidthVisitAboveIt() {
        val navigation = NavigationManager(Destination.Following(1))
        val source = navigation.backStack.last()
        navigation.navigateToProfileDetailScreen(2)
        navigation.navigateToSinglePictureScreen(99)
        val history = navigation.backStack

        navigation.closeDetailBranch(source.entryId)

        assertEquals(history, navigation.backStack)
    }

    @Test
    fun independentListsStartSourcesButRightPaneSearchKeepsTheOriginalOwner() {
        val navigation = NavigationManager(Destination.Main)
        navigation.navigateToFollowingScreen(1)
        val source = navigation.backStack.last()
        assertNull(source.ownerEntryId)
        navigation.navigateToProfileDetailScreen(2)

        navigation.navigateToSearchResultScreen("landscape")

        assertEquals(source.entryId, navigation.backStack.last().ownerEntryId)
        assertEquals(Destination.SearchResults("landscape"), navigation.currentDestination)
    }

    @Test
    fun settingsOpenSidePagesButAuthenticationDoesNotBecomeASource() {
        val settings = NavigationManager(Destination.Main)
        settings.navigateToSettingScreen()
        val source = settings.backStack.last()
        settings.navigateToNetworkSettingScreen()
        assertNull(source.ownerEntryId)
        assertEquals(source.entryId, settings.backStack.last().ownerEntryId)

        val authentication = NavigationManager(Destination.LoginOption)
        authentication.navigateToNetworkSettingScreen()
        assertNull(authentication.backStack.last().ownerEntryId)
    }

    @Test
    fun imagePreviewReturnsToTheSameSplitVisits() {
        val navigation = NavigationManager(Destination.Following(1))
        navigation.navigateToProfileDetailScreen(2)
        val split = navigation.backStack

        navigation.navigateToImagePreviewScreen(listOf("image"), 5)

        assertNull(navigation.backStack.last().ownerEntryId)
        assertEquals(Destination.ImagePreview(listOf("image"), 0), navigation.currentDestination)
        navigation.popBackStack()
        assertEquals(split, navigation.backStack)
    }

    @Test
    fun staleEntryCallbacksCannotNavigateOrPopTheActiveContext() {
        val navigation = NavigationManager(Destination.Following(1))
        val source = navigation.backStack.last()
        navigation.navigateToProfileDetailScreen(2)
        val oldDetail = navigation.forEntry(navigation.backStack.last().entryId)
        navigation.forEntry(source.entryId).navigateToProfileDetailScreen(3)
        val history = navigation.backStack

        oldDetail.navigateToCommentScreen(1, CommentType.ILLUST)
        oldDetail.popBackStack()

        assertEquals(history, navigation.backStack)
    }

    @Test
    fun changingMainTabsClearsThePreviousContextWithoutReplacingMain() {
        val navigation = NavigationManager(Destination.Main)
        val main = navigation.backStack.single()
        navigation.navigateToFollowingScreen(1)
        navigation.navigateToProfileDetailScreen(2)
        navigation.switchMainPage(MainPage.Home)
        assertEquals(3, navigation.backStack.size)

        navigation.switchMainPage(MainPage.Ranking)

        assertEquals(listOf(main), navigation.backStack)
        assertEquals(MainPage.Ranking, navigation.currentMainPage)
    }

    @Test
    fun coldStartDoesNotInferAnOwnerAndRootBackNeverEmptiesTheStack() {
        val navigation = NavigationManager(Destination.ProfileDetail(2))
        val initial = navigation.backStack.single()
        assertNull(initial.ownerEntryId)

        navigation.popBackStack()
        navigation.forEntry(initial.entryId).popBackStack()

        assertEquals(listOf(initial), navigation.backStack)
    }

    @Test
    fun historyIsAnImmutableSnapshotAndSerializesVisitAndOwnerIdentity() {
        val navigation = NavigationManager(Destination.Main)
        val original = navigation.backStack
        navigation.navigateToProfileDetailScreen(2)
        navigation.navigateToUserIllustScreen(2)
        val serializer = ListSerializer(NavigationRecord.serializer())
        val encoded = Json.encodeToString(serializer, navigation.backStack)

        assertEquals(1, original.size)
        assertEquals(navigation.backStack, Json.decodeFromString(serializer, encoded))
        assertTrue(
            navigation.backStack.drop(1).all { it.ownerEntryId == original.single().entryId })
    }

    @Test
    fun blockManagementReturnsThroughSettingsToProfile() {
        val navigation = NavigationManager(Destination.Main)
        navigation.switchMainPage(MainPage.Profile)
        navigation.navigateToSettingScreen()
        navigation.navigateToBlockSettings()

        assertEquals(
            listOf(Destination.Main, Destination.Setting, Destination.BlockSettings),
            navigation.backStack.map { it.destination },
        )
        navigation.popBackStack()
        assertEquals(Destination.Setting, navigation.currentDestination)
        navigation.popBackStack()
        assertEquals(MainPage.Profile, navigation.currentMainPage)
    }

    @Test
    fun explicitlyOpeningAFullWidthVisitPreservesThePreviousRightPane() {
        val navigation = NavigationManager(Destination.NovelReadLater)
        navigation.navigateToNovelDetailScreen(10, markerPage = 3)
        val split = navigation.backStack

        navigation.forEntry(split.last().entryId).openInMainPane()

        assertNull(navigation.backStack.last().ownerEntryId)
        assertEquals(split.last().destination, navigation.currentDestination)
        assertNotEquals(split.last().entryId, navigation.backStack.last().entryId)
        navigation.popBackStack()
        assertEquals(split, navigation.backStack)
    }

    @Test
    fun restoringAStateSnapshotKeepsVisitsOwnersAndMainPage() {
        val navigation = NavigationManager(Destination.Main)
        navigation.switchMainPage(MainPage.Latest)
        navigation.navigateToFollowingScreen(1)
        navigation.navigateToProfileDetailScreen(2)
        val snapshot = navigation.saveState()
        val encoded = Json.encodeToString(NavigationStateSnapshot.serializer(), snapshot)
        val restored = NavigationManager()

        restored.restoreState(Json.decodeFromString(NavigationStateSnapshot.serializer(), encoded))

        assertEquals(navigation.backStack, restored.backStack)
        assertEquals(MainPage.Latest, restored.currentMainPage)
        restored.forEntry(restored.backStack.last().entryId).navigateToUserIllustScreen(2)
        assertEquals(snapshot.records.last().ownerEntryId, restored.backStack.last().ownerEntryId)
    }

    @Test
    fun invalidRestorationCannotPairAcrossAFullWidthBoundaryOrOverwriteCurrentState() {
        val navigation = NavigationManager(Destination.Main)
        val initial = navigation.saveState()
        val invalid = NavigationStateSnapshot(
            records = listOf(
                NavigationRecord("list", Destination.Following(1)),
                NavigationRecord("picture", Destination.PictureDeeplink(99)),
                NavigationRecord("comment", Destination.Comment(99, CommentType.ILLUST), "list"),
            ),
            currentMainPage = MainPage.Profile,
        )

        assertFailsWith<IllegalArgumentException> { navigation.restoreState(invalid) }

        assertEquals(initial, navigation.saveState())
    }

    @Test
    fun emptyRestorationCannotRemoveTheRootEntry() {
        val navigation = NavigationManager(Destination.Main)
        val initial = navigation.saveState()

        assertFailsWith<IllegalArgumentException> {
            navigation.restoreState(NavigationStateSnapshot(emptyList(), MainPage.Profile))
        }

        assertEquals(initial, navigation.saveState())
        assertEquals(Destination.Main, navigation.currentDestination)
    }
}
