package com.mrl.pixiv.profile

import com.mrl.pixiv.common.data.AppViewMode
import com.mrl.pixiv.common.repository.requireUserPreferenceValue

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.Bookmarks
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.ImportExport
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.Style
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material3.Badge
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mrl.pixiv.common.compose.LocalSharedTransitionScope
import com.mrl.pixiv.common.compose.rememberThrottleClick
import com.mrl.pixiv.common.compose.ui.image.UserAvatar
import com.mrl.pixiv.common.data.setting.SettingTheme
import com.mrl.pixiv.common.repository.SettingRepository
import com.mrl.pixiv.common.repository.VersionManager
import com.mrl.pixiv.common.repository.requireUserInfoFlow
import com.mrl.pixiv.common.router.NavigationManager
import com.mrl.pixiv.common.router.currentNavigationManager
import com.mrl.pixiv.common.util.RStrings
import com.mrl.pixiv.community.NotificationUnreadBadge
import com.mrl.pixiv.strings.community_notifications
import com.mrl.pixiv.strings.community_followers
import com.mrl.pixiv.strings.reading_manga_watchlist
import com.mrl.pixiv.strings.about
import com.mrl.pixiv.strings.app_data
import com.mrl.pixiv.strings.block_settings
import com.mrl.pixiv.strings.bookmark_tags
import com.mrl.pixiv.strings.collection
import com.mrl.pixiv.strings.download_manager
import com.mrl.pixiv.strings.export_token
import com.mrl.pixiv.strings.history
import com.mrl.pixiv.strings.new_version_available
import com.mrl.pixiv.strings.novel_markers
import com.mrl.pixiv.strings.preference
import com.mrl.pixiv.strings.read_later
import com.mrl.pixiv.strings.sign_out
import com.mrl.pixiv.strings.theme_dark
import com.mrl.pixiv.strings.theme_light
import com.mrl.pixiv.strings.theme_system
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

private val options =
    mapOf(
        SettingTheme.SYSTEM to RStrings.theme_system,
        SettingTheme.LIGHT to RStrings.theme_light,
        SettingTheme.DARK to RStrings.theme_dark,
    )

private const val KEY_USER_INFO = "user_info"
private const val KEY_DIVIDER = "divider"
private const val KEY_PREFERENCE = "preference"
private const val KEY_HISTORY = "history"
private const val KEY_READ_LATER = "read_later"
private const val KEY_COLLECTION = "collection"
private const val KEY_NOVEL_MARKERS = "novel_markers"
private const val KEY_BOOKMARK_TAGS = "bookmark_tags"
private const val KEY_BLOCK_SETTINGS = "block_settings"
private const val KEY_DOWNLOAD_MANAGER = "download_manager"
private const val KEY_APP_DATA = "app_data"
private const val KEY_EXPORT_TOKEN = "export_token"
private const val KEY_ABOUT = "about"
private const val KEY_LOGOUT = "logout"

@Composable
fun ProfileScreen(
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel = koinViewModel(),
    navigationManager: NavigationManager = currentNavigationManager(),
) {
    val userInfo by requireUserInfoFlow.collectAsStateWithLifecycle()
    val hasNewVersion by VersionManager.hasNewVersion.collectAsStateWithLifecycle()
    LifecycleResumeEffect(Unit) {
        viewModel.dispatch(ProfileAction.GetUserInfo)
        onPauseOrDispose {}
    }
    Scaffold(
        topBar = {
            ProfileAppBar(
                onChangeAppTheme = { theme ->
                    viewModel.changeAppTheme(theme)
                }
            )
        },
        contentWindowInsets = ScaffoldDefaults.contentWindowInsets.exclude(WindowInsets.navigationBars),
    ) {
        LazyColumn(
            modifier = modifier
                .padding(it)
                .fillMaxSize()
                .padding(top = 16.dp),
        ) {
            item(key = KEY_USER_INFO) {
                // 头像和昵称
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    with(LocalSharedTransitionScope.current) {
                        UserAvatar(
                            url = userInfo.user.profileImageUrls.medium,
                            modifier = Modifier.size(80.dp),
                            onClick = {
                                navigationManager.navigateToProfileDetailScreen(userInfo.user.id)
                            }
                        )
                        Column {
                            // 昵称
                            Text(
                                text = userInfo.user.name,
                            )
                            // ID
                            Text(
                                text = "ID: ${userInfo.user.id}",
                            )
                        }
                    }
                }
            }
            item(key = KEY_DIVIDER) {
                HorizontalDivider(
                    modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp)
                )
            }
            // 偏好设置
            item(key = KEY_PREFERENCE) {
                ListItem(
                    onClick = rememberThrottleClick {
                        navigationManager.navigateToSettingScreen()
                    },
                    shapes = ListItemDefaults.shapes(shape = RectangleShape),
                    content = {
                        Text(
                            text = stringResource(RStrings.preference),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    },
                    modifier = Modifier
                        .padding(horizontal = 8.dp),
                    leadingContent = {
                        Icon(imageVector = Icons.Rounded.Settings, contentDescription = null)
                    },
                )
            }
            // 历史记录
            item(key = KEY_HISTORY) {
                ListItem(
                    onClick = rememberThrottleClick {
                        navigationManager.navigateToHistoryScreen()
                    },
                    shapes = ListItemDefaults.shapes(shape = RectangleShape),
                    content = {
                        Text(
                            text = stringResource(RStrings.history),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    },
                    modifier = Modifier
                        .padding(horizontal = 8.dp),
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Rounded.History,
                            contentDescription = null
                        )
                    },
                )
            }
            item(key = KEY_READ_LATER) {
                ListItem(
                    onClick = rememberThrottleClick {
                        navigationManager.navigateToNovelReadLaterScreen()
                    },
                    shapes = ListItemDefaults.shapes(shape = RectangleShape),
                    content = {
                        Text(
                            text = stringResource(RStrings.read_later),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    },
                    modifier = Modifier
                        .padding(horizontal = 8.dp),
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Rounded.Schedule,
                            contentDescription = null,
                        )
                    },
                )
            }
            // 收藏
            item(key = "community_notifications") {
                ListItem(
                    onClick = rememberThrottleClick { navigationManager.navigateToNotifications() },
                    shapes = ListItemDefaults.shapes(shape = RectangleShape),
                    content = { Text(stringResource(RStrings.community_notifications)) },
                    leadingContent = { Icon(Icons.Rounded.Notifications, contentDescription = null) },
                    trailingContent = { NotificationUnreadBadge() },
                    modifier = Modifier.padding(horizontal = 8.dp),
                )
            }
            item(key = "community_followers") {
                ListItem(
                    onClick = rememberThrottleClick { navigationManager.navigateToFollowers() },
                    shapes = ListItemDefaults.shapes(shape = RectangleShape),
                    content = { Text(stringResource(RStrings.community_followers)) },
                    leadingContent = { Icon(Icons.Rounded.People, contentDescription = null) },
                    modifier = Modifier.padding(horizontal = 8.dp),
                )
            }
            item(key = "manga_watchlist") {
                ListItem(
                    onClick = rememberThrottleClick { navigationManager.navigateToMangaWatchlistScreen() },
                    shapes = ListItemDefaults.shapes(shape = RectangleShape),
                    content = { Text(stringResource(RStrings.reading_manga_watchlist)) },
                    leadingContent = { Icon(Icons.Rounded.MenuBook, contentDescription = null) },
                    modifier = Modifier.padding(horizontal = 8.dp),
                )
            }
            item(key = KEY_COLLECTION) {
                ListItem(
                    onClick = rememberThrottleClick {
                        navigationManager.navigateToCollectionScreen(
                            userInfo.user.id,
                            isNovel = requireUserPreferenceValue.collectionViewMode == AppViewMode.NOVEL,
                        )
                    },
                    shapes = ListItemDefaults.shapes(shape = RectangleShape),
                    content = {
                        Text(
                            text = stringResource(RStrings.collection),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    },
                    modifier = Modifier
                        .padding(horizontal = 8.dp),
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Rounded.Bookmarks,
                            contentDescription = null
                        )
                    },
                )
            }
            // 小说阅读书签
            item(key = KEY_NOVEL_MARKERS) {
                ListItem(
                    onClick = rememberThrottleClick {
                        navigationManager.navigateToNovelMarkersScreen()
                    },
                    shapes = ListItemDefaults.shapes(shape = RectangleShape),
                    content = {
                        Text(
                            text = stringResource(RStrings.novel_markers),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    },
                    modifier = Modifier
                        .padding(horizontal = 8.dp),
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Rounded.Bookmark,
                            contentDescription = null
                        )
                    },
                )
            }
            // 收藏标签
            item(key = KEY_BOOKMARK_TAGS) {
                ListItem(
                    onClick = rememberThrottleClick {
                        navigationManager.navigateToBookmarkedTagsScreen()
                    },
                    shapes = ListItemDefaults.shapes(shape = RectangleShape),
                    content = {
                        Text(
                            text = stringResource(RStrings.bookmark_tags),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    },
                    modifier = Modifier
                        .padding(horizontal = 8.dp),
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Rounded.Style,
                            contentDescription = null
                        )
                    },
                )
            }
            // 屏蔽设定
            item(key = KEY_BLOCK_SETTINGS) {
                ListItem(
                    onClick = rememberThrottleClick {
                        navigationManager.navigateToBlockSettings()
                    },
                    shapes = ListItemDefaults.shapes(shape = RectangleShape),
                    content = {
                        Text(
                            text = stringResource(RStrings.block_settings),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    },
                    modifier = Modifier
                        .padding(horizontal = 8.dp),
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Rounded.Block,
                            contentDescription = null
                        )
                    },
                )
            }
            // 下载管理
            item(key = KEY_DOWNLOAD_MANAGER) {
                ListItem(
                    onClick = rememberThrottleClick {
                        navigationManager.navigateToDownloadScreen()
                    },
                    shapes = ListItemDefaults.shapes(shape = RectangleShape),
                    content = {
                        Text(
                            text = stringResource(RStrings.download_manager),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    },
                    modifier = Modifier
                        .padding(horizontal = 8.dp),
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Rounded.Download,
                            contentDescription = null
                        )
                    },
                )
            }
            // 应用数据
            item(key = KEY_APP_DATA) {
                ListItem(
                    onClick = rememberThrottleClick {
                        navigationManager.navigateToAppDataScreen()
                    },
                    shapes = ListItemDefaults.shapes(shape = RectangleShape),
                    content = {
                        Text(
                            text = stringResource(RStrings.app_data),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    },
                    modifier = Modifier
                        .padding(horizontal = 8.dp),
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Rounded.Storage,
                            contentDescription = null
                        )
                    },
                )
            }
            // 导出Token
            item(key = KEY_EXPORT_TOKEN) {
                Column {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                    ListItem(
                        onClick = rememberThrottleClick {
                            viewModel.dispatch(ProfileAction.ExportToken)
                        },
                        shapes = ListItemDefaults.shapes(shape = RectangleShape),
                        content = {
                            Text(
                                text = stringResource(RStrings.export_token),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        },
                        modifier = Modifier
                            .padding(horizontal = 8.dp),
                        leadingContent = {
                            Icon(
                                imageVector = Icons.Rounded.ImportExport,
                                contentDescription = null
                            )
                        },
                    )
                }
            }
            // 关于
            item(key = KEY_ABOUT) {
                ListItem(
                    onClick = rememberThrottleClick {
                        navigationManager.navigateToAboutScreen()
                    },
                    shapes = ListItemDefaults.shapes(shape = RectangleShape),
                    content = {
                        Text(
                            text = stringResource(RStrings.about),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    },
                    modifier = Modifier
                        .padding(horizontal = 8.dp),
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Rounded.Info,
                            contentDescription = null
                        )
                    },
                    trailingContent = {
                        if (hasNewVersion) {
                            Badge {
                                Text(
                                    text = stringResource(RStrings.new_version_available),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                )
                            }
                        }
                    },
                )
            }
            // 退出登录
            item(key = KEY_LOGOUT) {
                ListItem(
                    onClick = rememberThrottleClick {
                        viewModel.logout()
                        navigationManager.navigateToLoginOptionScreen()
                    },
                    shapes = ListItemDefaults.shapes(shape = RectangleShape),
                    content = {
                        Text(
                            text = stringResource(RStrings.sign_out),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    },
                    modifier = Modifier
                        .padding(horizontal = 8.dp),
                    leadingContent = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.Logout,
                            contentDescription = null
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun ProfileAppBar(
    onChangeAppTheme: (SettingTheme) -> Unit = {},
) {
    val userPreference by SettingRepository.userPreferenceFlow.collectAsStateWithLifecycle()
    var expanded by remember { mutableStateOf(false) }
    TopAppBar(
        title = {},
        actions = {
            IconButton(
                onClick = { expanded = true },
                shapes = IconButtonDefaults.shapes(),
            ) {
                Icon(imageVector = Icons.Rounded.Palette, contentDescription = null)
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = {
                    expanded = false
                }
            ) {
                options.forEach { (theme, resId) ->
                    DropdownMenuItem(
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = stringResource(resId),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                if (userPreference.theme == theme.name) {
                                    Icon(
                                        imageVector = Icons.Rounded.Check,
                                        contentDescription = null
                                    )
                                }
                            }
                        },
                        onClick = {
                            onChangeAppTheme(theme)
                            expanded = false
                        }
                    )
                }
            }
        }
    )
}
