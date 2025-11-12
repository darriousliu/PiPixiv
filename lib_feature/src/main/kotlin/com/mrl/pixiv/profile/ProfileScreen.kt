package com.mrl.pixiv.profile

import android.os.Build
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
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.ImportExport
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mrl.pixiv.common.compose.LocalSharedTransitionScope
import com.mrl.pixiv.common.compose.ui.SettingItem
import com.mrl.pixiv.common.compose.ui.image.UserAvatar
import com.mrl.pixiv.common.data.setting.SettingTheme
import com.mrl.pixiv.common.data.setting.getAppCompatDelegateThemeMode
import com.mrl.pixiv.common.datasource.local.mmkv.requireUserInfoFlow
import com.mrl.pixiv.common.datasource.local.mmkv.requireUserInfoValue
import com.mrl.pixiv.common.router.NavigationManager
import com.mrl.pixiv.common.util.RString
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

private val options = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
    mapOf(
        SettingTheme.SYSTEM to RString.theme_system,
        SettingTheme.LIGHT to RString.theme_light,
        SettingTheme.DARK to RString.theme_dark,
    )
} else {
    mapOf(
        SettingTheme.LIGHT to RString.theme_light,
        SettingTheme.DARK to RString.theme_dark,
    )
}

private const val KEY_USER_INFO = "user_info"
private const val KEY_DIVIDER = "divider"
private const val KEY_SETTINGS = "settings"

@Composable
fun ProfileScreen(
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel = koinViewModel(),
    navigationManager: NavigationManager = koinInject(),
) {
    val userInfo by requireUserInfoFlow.collectAsStateWithLifecycle()
    LifecycleResumeEffect(Unit) {
        viewModel.dispatch(ProfileAction.GetUserInfo)
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
                .padding(start = 16.dp, top = 16.dp, end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item(key = KEY_USER_INFO) {
                // 头像和昵称
                Row(
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
                HorizontalDivider()
            }
            item(key = KEY_SETTINGS) {
                Column {
                    // 偏好设置
                    SettingItem(
                        onClick = navigationManager::navigateToSettingScreen,
                        icon = {
                            Icon(imageVector = Icons.Rounded.Settings, contentDescription = null)
                        },
                    ) {
                        Text(
                            text = stringResource(RString.preference),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                    // 历史记录
                    SettingItem(
                        onClick = navigationManager::navigateToHistoryScreen,
                        icon = {
                            Icon(
                                imageVector = Icons.Rounded.History,
                                contentDescription = null
                            )
                        },
                    ) {
                        Text(
                            text = stringResource(RString.history),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                    // 收藏
                    SettingItem(
                        onClick = {
                            navigationManager.navigateToCollectionScreen(requireUserInfoValue.user.id)
                        },
                        icon = {
                            Icon(
                                imageVector = Icons.Rounded.Bookmarks,
                                contentDescription = null
                            )
                        },
                    ) {
                        Text(
                            text = stringResource(RString.collection),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                    // 屏蔽设定
                    SettingItem(
                        onClick = {
                            navigationManager.navigateToBlockSettings()
                        },
                        icon = {
                            Icon(
                                imageVector = Icons.Rounded.Block,
                                contentDescription = null
                            )
                        }
                    ) {
                        Text(
                            text = stringResource(RString.block_settings),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    // 导出Token
                    SettingItem(
                        onClick = {
                            viewModel.dispatch(ProfileAction.ExportToken)
                        },
                        icon = {
                            Icon(
                                imageVector = Icons.Rounded.ImportExport,
                                contentDescription = null
                            )
                        },
                    ) {
                        Text(
                            text = stringResource(RString.export_token),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                    // 退出登录
                    SettingItem(
                        onClick = {
                            viewModel.logout()
                            navigationManager.navigateToLoginOptionScreen()
                        },
                        icon = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.Logout,
                                contentDescription = null
                            )
                        },
                    ) {
                        Text(
                            text = stringResource(RString.sign_out),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
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