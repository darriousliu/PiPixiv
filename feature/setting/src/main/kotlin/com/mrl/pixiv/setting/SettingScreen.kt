package com.mrl.pixiv.setting

import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForwardIos
import androidx.compose.material.icons.rounded.AddLink
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.NetworkWifi
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.core.os.LocaleListCompat
import com.mrl.pixiv.common.repository.SettingRepository
import com.mrl.pixiv.common.router.Destination
import com.mrl.pixiv.common.router.NavigationManager
import com.mrl.pixiv.common.util.RString
import com.mrl.pixiv.common.util.throttleClick
import com.mrl.pixiv.setting.components.DropDownSelector
import org.koin.compose.koinInject

@Composable
fun SettingScreen(
    modifier: Modifier = Modifier,
    navigationManager: NavigationManager = koinInject()
) {
    val context = LocalContext.current
    val labelDefault = stringResource(RString.label_default)
    val languages = remember { getLangs(context) }
    var currentLanguage by remember(labelDefault) {
        mutableStateOf(
            AppCompatDelegate.getApplicationLocales().get(0)?.toLanguageTag() ?: labelDefault
        )
    }
    val userPreference by SettingRepository.userPreferenceFlow.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(text = stringResource(RString.setting))
                },
                navigationIcon = {
                    IconButton(
                        onClick = navigationManager::popBackStack,
                        shapes = IconButtonDefaults.shapes(),
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null)
                    }
                },
            )
        },
    ) {
        LazyColumn(
            modifier = modifier.padding(it),
        ) {
            val itemModifier = Modifier.padding(horizontal = 8.dp)
            item {
                var expanded by remember { mutableStateOf(false) }
                // 语言
                ListItem(
                    headlineContent = {
                        LaunchedEffect(currentLanguage, labelDefault) {
                            val locale = if (currentLanguage == labelDefault) {
                                LocaleListCompat.getEmptyLocaleList()
                            } else {
                                LocaleListCompat.forLanguageTags(currentLanguage)
                            }
                            AppCompatDelegate.setApplicationLocales(locale)
                        }

                        Text(
                            text = stringResource(RString.app_language),
                        )
                    },
                    modifier = itemModifier,
                    leadingContent = {
                        Icon(Icons.Rounded.Translate, contentDescription = null)
                    },
                    trailingContent = {
                        DropDownSelector(
                            modifier = Modifier.throttleClick {
                                expanded = !expanded
                            },
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                            current = currentLanguage,
                        ) {
                            languages.forEach {
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = it.displayName,
                                                modifier = Modifier.padding(16.dp),
                                            )
                                            if (currentLanguage == it.langTag) {
                                                Icon(
                                                    imageVector = Icons.Rounded.Check,
                                                    contentDescription = null
                                                )
                                            }
                                        }
                                    },
                                    onClick = {
                                        currentLanguage = it.langTag
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                )
            }

            item {
                ListItem(
                    headlineContent = {
                        Text(
                            text = stringResource(RString.network_setting),
                        )
                    },
                    modifier = Modifier
                        .throttleClick(
                            indication = ripple()
                        ) {
                            navigationManager.navigate(route = Destination.NetworkSetting)
                        }
                        .then(itemModifier),
                    leadingContent = {
                        Icon(imageVector = Icons.Rounded.NetworkWifi, contentDescription = null)
                    },
                    trailingContent = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowForwardIos,
                            contentDescription = null
                        )
                    }
                )
            }

            item {
                ListItem(
                    headlineContent = {
                        Text(
                            text = stringResource(RString.download_single_folder_by_user_title),
                        )
                    },
                    modifier = itemModifier
                        .height(IntrinsicSize.Min)
                        .throttleClick(
                            indication = ripple()
                        ) {
                            SettingRepository.setDownloadSubFolderByUser(!userPreference.downloadSubFolderByUser)
                        },
                    supportingContent = {
                        Text(
                            text = stringResource(RString.download_single_folder_by_user_desc),
                        )
                    },
                    leadingContent = {
                        Column(
                            modifier = Modifier.fillMaxHeight(),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(imageVector = Icons.Rounded.Folder, contentDescription = null)
                        }
                    },
                    trailingContent = {
                        Switch(
                            checked = userPreference.downloadSubFolderByUser,
                            onCheckedChange = { SettingRepository.setDownloadSubFolderByUser(it) }
                        )
                    }
                )
            }

            item {
                ListItem(
                    headlineContent = {
                        Text(
                            text = stringResource(RString.default_open),
                        )
                    },
                    modifier = Modifier
                        .height(IntrinsicSize.Min)
                        .throttleClick(
                            indication = ripple()
                        ) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                try {
                                    val intent = Intent().apply {
                                        action =
                                            Settings.ACTION_APP_OPEN_BY_DEFAULT_SETTINGS
                                        addCategory(Intent.CATEGORY_DEFAULT)
                                        data = "package:${context.packageName}".toUri()
                                        addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
                                        addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
                                    }
                                    context.startActivity(intent)
                                } catch (_: Throwable) {
                                }
                            }
                        }
                        .then(itemModifier),
                    supportingContent = {
                        Text(
                            text = stringResource(RString.allow_open_link),
                        )
                    },
                    leadingContent = {
                        Column(
                            modifier = Modifier.fillMaxHeight(),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(imageVector = Icons.Rounded.AddLink, contentDescription = null)
                        }
                    },
                )
            }
        }
    }
}
