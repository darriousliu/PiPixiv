package com.mrl.pixiv.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.Bookmarks
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.HowToReg
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mrl.pixiv.common.compose.layout.currentPaneLayoutInfo
import com.mrl.pixiv.common.compose.layout.isWidthAtLeastMedium
import com.mrl.pixiv.common.compose.layout.withMainNavigationBottomInset
import com.mrl.pixiv.common.compose.rememberThrottleClick
import com.mrl.pixiv.common.compose.theme.tabularNumbers
import com.mrl.pixiv.common.compose.ui.image.UserAvatar
import com.mrl.pixiv.common.data.AppViewMode
import com.mrl.pixiv.common.data.User
import com.mrl.pixiv.common.data.setting.SettingTheme
import com.mrl.pixiv.common.repository.SettingRepository
import com.mrl.pixiv.common.repository.VersionManager
import com.mrl.pixiv.common.repository.requireUserInfoFlow
import com.mrl.pixiv.common.repository.requireUserPreferenceValue
import com.mrl.pixiv.common.router.NavigationManager
import com.mrl.pixiv.common.router.currentNavigationManager
import com.mrl.pixiv.common.util.RStrings
import com.mrl.pixiv.community.NotificationUnreadBadge
import com.mrl.pixiv.strings.collection
import com.mrl.pixiv.strings.community_followers
import com.mrl.pixiv.strings.community_notifications
import com.mrl.pixiv.strings.download_manager
import com.mrl.pixiv.strings.followed
import com.mrl.pixiv.strings.history
import com.mrl.pixiv.strings.my
import com.mrl.pixiv.strings.new_version_available
import com.mrl.pixiv.strings.profile_view_homepage
import com.mrl.pixiv.strings.reading_title
import com.mrl.pixiv.strings.setting
import com.mrl.pixiv.strings.theme_dark
import com.mrl.pixiv.strings.theme_light
import com.mrl.pixiv.strings.theme_system
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

private val themeOptions = mapOf(
    SettingTheme.SYSTEM to RStrings.theme_system,
    SettingTheme.LIGHT to RStrings.theme_light,
    SettingTheme.DARK to RStrings.theme_dark,
)

@Composable
fun ProfileScreen(
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel = koinViewModel(),
    navigationManager: NavigationManager = currentNavigationManager(),
) {
    val userInfo by requireUserInfoFlow.collectAsStateWithLifecycle()
    val userPreference by SettingRepository.userPreferenceFlow.collectAsStateWithLifecycle()
    val hasNewVersion by VersionManager.hasNewVersion.collectAsStateWithLifecycle()
    LifecycleResumeEffect(Unit) {
        viewModel.dispatch(ProfileAction.GetUserInfo)
        onPauseOrDispose {}
    }
    ProfileContent(
        user = userInfo.user,
        currentTheme = userPreference.theme,
        hasNewVersion = hasNewVersion,
        onProfile = { navigationManager.navigateToProfileDetailScreen(userInfo.user.id) },
        onFollowing = { navigationManager.navigateToFollowingScreen(userInfo.user.id) },
        onFollowers = navigationManager::navigateToFollowers,
        onCollection = {
            navigationManager.navigateToCollectionScreen(
                userInfo.user.id,
                isNovel = requireUserPreferenceValue.collectionViewMode == AppViewMode.NOVEL,
            )
        },
        onReading = navigationManager::navigateToReadingScreen,
        onHistory = navigationManager::navigateToHistoryScreen,
        onDownloads = navigationManager::navigateToDownloadScreen,
        onNotifications = { navigationManager.navigateToNotifications() },
        onSettings = navigationManager::navigateToSettingScreen,
        onChangeTheme = viewModel::changeAppTheme,
        notificationBadge = { NotificationUnreadBadge() },
        modifier = modifier,
    )
}

@Composable
internal fun ProfileContent(
    user: User,
    currentTheme: String,
    hasNewVersion: Boolean,
    onProfile: () -> Unit,
    onFollowing: () -> Unit,
    onFollowers: () -> Unit,
    onCollection: () -> Unit,
    onReading: () -> Unit,
    onHistory: () -> Unit,
    onDownloads: () -> Unit,
    onNotifications: () -> Unit,
    onSettings: () -> Unit,
    onChangeTheme: (SettingTheme) -> Unit,
    modifier: Modifier = Modifier,
    notificationBadge: @Composable () -> Unit = {},
) {
    // 框架调整时同步 feature/setting/.../appearance/MainPageSkeletons.kt 中的 ProfileSkeleton。
    Scaffold(
        modifier = modifier,
        topBar = { ProfileAppBar(currentTheme, onChangeTheme) },
        contentWindowInsets = ScaffoldDefaults.contentWindowInsets.exclude(WindowInsets.navigationBars),
    ) { padding ->
        ProfileLayout(
            modifier = Modifier.padding(padding),
            identity = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    ProfileIdentityCard(user, onProfile)
                    ProfileConnections(onFollowing, onFollowers)
                }
            },
            menus = {
                Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                    ProfileMenuGroup(
                        listOf(
                            ProfileMenuEntry(RStrings.collection, Icons.Rounded.Bookmarks, onCollection),
                            ProfileMenuEntry(RStrings.reading_title, Icons.AutoMirrored.Rounded.MenuBook, onReading),
                            ProfileMenuEntry(RStrings.history, Icons.Rounded.History, onHistory),
                            ProfileMenuEntry(RStrings.download_manager, Icons.Rounded.Download, onDownloads),
                        )
                    )
                    ProfileMenuGroup(
                        listOf(
                            ProfileMenuEntry(
                                RStrings.community_notifications,
                                Icons.Rounded.Notifications,
                                onNotifications,
                                trailingContent = notificationBadge,
                            ),
                            ProfileMenuEntry(
                                RStrings.setting,
                                Icons.Rounded.Settings,
                                onSettings,
                                supportingText = if (hasNewVersion) RStrings.new_version_available else null,
                            ),
                        )
                    )
                }
            },
        )
    }
}

@Composable
private fun ProfileLayout(
    identity: @Composable () -> Unit,
    menus: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    val wide = currentPaneLayoutInfo().sizeClass.isWidthAtLeastMedium
    val edgePadding = if (wide) 24.dp else 16.dp
    val contentPadding = PaddingValues(start = edgePadding, end = edgePadding, bottom = 24.dp)
        .withMainNavigationBottomInset()
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(contentPadding),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        if (wide) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterHorizontally),
            ) {
                val paneModifier = Modifier.weight(1f, fill = false).widthIn(max = 420.dp)
                Column(paneModifier) { identity() }
                Column(paneModifier) { menus() }
            }
        } else {
            identity()
            menus()
        }
    }
}

@Composable
private fun ProfileIdentityCard(user: User, onProfile: () -> Unit) {
    Surface(
        onClick = rememberThrottleClick(onClick = onProfile),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            UserAvatar(
                url = user.profileImageUrls.medium,
                modifier = Modifier.size(64.dp),
                onClick = onProfile,
                contentDescription = stringResource(RStrings.profile_view_homepage),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = user.name,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "ID: ${user.id}",
                    style = MaterialTheme.typography.labelMedium.tabularNumbers,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(RStrings.profile_view_homepage),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

private data class ProfileMenuEntry(
    val label: StringResource,
    val icon: ImageVector,
    val onClick: () -> Unit,
    val supportingText: StringResource? = null,
    val trailingContent: (@Composable () -> Unit)? = null,
)

@Composable
private fun ProfileMenuGroup(entries: List<ProfileMenuEntry>) {
    Column(verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
        entries.forEachIndexed { index, entry ->
            SegmentedListItem(
                onClick = rememberThrottleClick(onClick = entry.onClick),
                shapes = ListItemDefaults.segmentedShapes(index, entries.size),
                colors = ListItemDefaults.segmentedColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                modifier = Modifier.fillMaxWidth().semantics { role = Role.Button },
                leadingContent = {
                    Icon(entry.icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                },
                supportingContent = entry.supportingText?.let { text ->
                    { Text(stringResource(text), color = MaterialTheme.colorScheme.primary) }
                },
                trailingContent = entry.trailingContent,
            ) {
                Text(stringResource(entry.label))
            }
        }
    }
}

@Composable
private fun ProfileAppBar(currentTheme: String, onChangeTheme: (SettingTheme) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    TopAppBar(
        title = { Text(stringResource(RStrings.my)) },
        actions = {
            Box {
                IconButton(onClick = { expanded = true }, shapes = IconButtonDefaults.shapes()) {
                    Icon(
                        Icons.Rounded.Palette, contentDescription = stringResource(
                            themeOptions.entries.firstOrNull { it.key.name == currentTheme }?.value
                                ?: RStrings.theme_system
                        )
                    )
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    themeOptions.forEach { (theme, label) ->
                        DropdownMenuItem(
                            text = { Text(stringResource(label)) },
                            trailingIcon = if (currentTheme == theme.name) {
                                { Icon(Icons.Rounded.Check, contentDescription = null) }
                            } else null,
                            onClick = {
                                onChangeTheme(theme)
                                expanded = false
                            },
                        )
                    }
                }
            }
        },
    )
}

@Composable
internal fun ProfileConnections(onFollowingClick: () -> Unit, onFollowersClick: () -> Unit) {
    Row(
        modifier = Modifier.height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ProfileConnectionButton(
            label = stringResource(RStrings.followed),
            icon = Icons.Rounded.HowToReg,
            onClick = onFollowingClick,
            modifier = Modifier.weight(1f),
        )
        ProfileConnectionButton(
            label = stringResource(RStrings.community_followers),
            icon = Icons.Rounded.People,
            onClick = onFollowersClick,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun ProfileConnectionButton(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier
) {
    FilledTonalButton(
        onClick = rememberThrottleClick(onClick = onClick),
        modifier = modifier.fillMaxHeight().heightIn(min = 64.dp),
        shapes = ButtonDefaults.shapes(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Text(label, textAlign = TextAlign.Center)
        }
    }
}
