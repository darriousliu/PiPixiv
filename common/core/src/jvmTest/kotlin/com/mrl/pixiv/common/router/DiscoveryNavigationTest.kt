package com.mrl.pixiv.common.router

import com.mrl.pixiv.common.data.discovery.CommunityUsersKind
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DiscoveryNavigationTest {
    @Test
    fun newDestinationsPreserveArgumentsAcrossSavedState() {
        val routes = listOf(
            Destination.Notifications(), Destination.Notifications(9_999_999_999L, "Updates"),
            Destination.CommunityUsers(CommunityUsersKind.RELATED, 34),
            Destination.CommunityUsers(CommunityUsersKind.FOLLOWERS),
            Destination.MangaSeries(56), Destination.MangaWatchlist,
            Destination.UserMangaSeries(34), Destination.CloudMute,
        )
        val codec = ListSerializer(Destination.serializer())
        assertEquals(routes, Json.decodeFromString(codec, Json.encodeToString(codec, routes)))
    }

    @Test
    fun mangaAndDiscoveryListsCanHostAnAuthorDetail() {
        listOf(
            Destination.MangaSeries(56), Destination.MangaWatchlist,
            Destination.CommunityUsers(CommunityUsersKind.RECOMMENDED), Destination.Notifications(),
        ).forEach { route ->
            assertTrue(route.paneSpec.preferAsSource)
            val navigation = NavigationManager(route)
            val source = navigation.backStack.last()
            navigation.forEntry(source.entryId).navigateToProfileDetailScreen(34)
            assertEquals(source.entryId, navigation.backStack.last().ownerEntryId)
            navigation.popBackStack()
            assertEquals(source, navigation.backStack.last())
        }
    }
}
