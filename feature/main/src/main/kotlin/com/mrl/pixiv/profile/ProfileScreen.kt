package com.mrl.pixiv.profile

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
import androidx.compose.material.icons.rounded.Bookmarks
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.ImportExport
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.Style
import androidx.compose.material3.Badge
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mrl.pixiv.common.compose.LocalSharedTransitionScope
import com.mrl.pixiv.common.compose.ui.image.UserAvatar
import com.mrl.pixiv.common.data.setting.SettingTheme
import com.mrl.pixiv.common.data.setting.getAppCompatDelegateThemeMode
import com.mrl.pixiv.common.repository.VersionManager
import com.mrl.pixiv.common.repository.requireUserInfoFlow
import com.mrl.pixiv.common.router.NavigationManager
import com.mrl.pixiv.common.util.RString
import com.mrl.pixiv.common.util.throttleClick
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

private val options =
    mapOf(
        SettingTheme.SYSTEM to RString.theme_system,
        SettingTheme.LIGHT to RString.theme_light,
        SettingTheme.DARK to RString.theme_dark,
    )

private const val KEY_USER_INFO = "user_info"
private const val KEY_DIVIDER = "divider"
private const val KEY_PREFERENCE = "preference"
private const val KEY_HISTORY = "history"
private const val KEY_COLLECTION = "collection"
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
    navigationManager: NavigationManager = koinInject(),
) {
    val userInfo by requireUserInfoFlow.collectAsStateWithLifecycle()
    val hasNewVersion by VersionManager.hasNewVersion.collectAsStateWithLifecycle()
    LifecycleResumeEffect(Unit) {
        viewModel.dispatch(ProfileAction.GetUserInfo)
        VersionManager.checkUpdate()
        onPauseOrDispose {}
    }
    Scaffold(
        topBar = {
            ProfileAppBar(
                onChangeAppTheme = { theme ->
                    viewModel.dispatch(ProfileAction.ChangeAppTheme(theme = theme))
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
                    headlineContent = {
                        Text(
                            text = stringResource(RString.preference),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    },
                    modifier = Modifier
                        .throttleClick(
                            indication = ripple()
                        ) {
                            navigationManager.navigateToSettingScreen()
                        }
                        .padding(horizontal = 8.dp),
                    leadingContent = {
                        Icon(imageVector = Icons.Rounded.Settings, contentDescription = null)
                    }
                )
            }
            // 历史记录
            item(key = KEY_HISTORY) {
                ListItem(
                    headlineContent = {
                        Text(
                            text = stringResource(RString.history),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    },
                    modifier = Modifier
                        .throttleClick(
                            indication = ripple()
                        ) {
                            navigationManager.navigateToHistoryScreen()
                        }
                        .padding(horizontal = 8.dp),
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Rounded.History,
                            contentDescription = null
                        )
                    },
                )
            }
            // 收藏
            item(key = KEY_COLLECTION) {
                ListItem(
                    headlineContent = {
                        Text(
                            text = stringResource(RString.collection),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    },
                    modifier = Modifier
                        .throttleClick(
                            indication = ripple()
                        ) {
                            navigationManager.navigateToCollectionScreen(userInfo.user.id)
                        }
                        .padding(horizontal = 8.dp),
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Rounded.Bookmarks,
                            contentDescription = null
                        )
                    },
                )
            }
            // 收藏标签
            item(key = KEY_BOOKMARK_TAGS) {
                ListItem(
                    headlineContent = {
                        Text(
                            text = stringResource(RString.bookmark_tags),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    },
                    modifier = Modifier
                        .throttleClick(
                            indication = ripple()
                        ) {
                            navigationManager.navigateToBookmarkedTagsScreen()
                        }
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
                    headlineContent = {
                        Text(
                            text = stringResource(RString.block_settings),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    },
                    modifier = Modifier
                        .throttleClick(
                            indication = ripple()
                        ) {
                            navigationManager.navigateToBlockSettings()
                        }
                        .padding(horizontal = 8.dp),
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Rounded.Block,
                            contentDescription = null
                        )
                    }
                )
            }
            // 下载管理
            item(key = KEY_DOWNLOAD_MANAGER) {
                ListItem(
                    headlineContent = {
                        Text(
                            text = stringResource(RString.download_manager),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    },
                    modifier = Modifier
                        .throttleClick(
                            indication = ripple()
                        ) {
                            navigationManager.navigateToDownloadScreen()
                        }
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
                    headlineContent = {
                        Text(
                            text = stringResource(RString.app_data),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    },
                    modifier = Modifier
                        .throttleClick(
                            indication = ripple()
                        ) {
                            navigationManager.navigateToAppDataScreen()
                        }
                        .padding(horizontal = 8.dp),
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Rounded.Storage,
                            contentDescription = null
                        )
                    }
                )
            }
            // 导出Token
            item(key = KEY_EXPORT_TOKEN) {
                Column {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                    ListItem(
                        headlineContent = {
                            Text(
                                text = stringResource(RString.export_token),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        },
                        modifier = Modifier
                            .throttleClick(
                                indication = ripple()
                            ) {
                                viewModel.dispatch(ProfileAction.ExportToken)
                            }
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
                    headlineContent = {
                        Text(
                            text = stringResource(RString.about),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    },
                    modifier = Modifier
                        .throttleClick(
                            indication = ripple()
                        ) {
                            navigationManager.navigateToAboutScreen()
                        }
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
                                Text(text = "New")
                            }
                        }
                    }
                )
            }
            // 退出登录
            item(key = KEY_LOGOUT) {
                ListItem(
                    headlineContent = {
                        Text(
                            text = stringResource(RString.sign_out),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    },
                    modifier = Modifier
                        .throttleClick(
                            indication = ripple()
                        ) {
                            viewModel.logout()
                            navigationManager.navigateToLoginOptionScreen()
                        }
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
                                if (getAppCompatDelegateThemeMode() == theme) {
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
