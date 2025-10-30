package com.mrl.pixiv.setting

import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForwardIos
import androidx.compose.material.icons.rounded.AddLink
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.NetworkWifi
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.mrl.pixiv.common.compose.ui.SettingItem
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
    val languages = remember { getLangs(context) }
    var currentLanguage by remember {
        mutableStateOf(
            AppCompatDelegate.getApplicationLocales().get(0)?.toLanguageTag() ?: "Default"
        )
    }
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
            contentPadding = PaddingValues(horizontal = 16.dp)
        ) {
            item {
                var expanded by remember { mutableStateOf(false) }
                // 语言
                SettingItem(
                    icon = {
                        Icon(Icons.Rounded.Translate, contentDescription = null)
                    },
                    content = {
                        LaunchedEffect(currentLanguage) {
                            val locale = if (currentLanguage == "Default") {
                                LocaleListCompat.getEmptyLocaleList()
                            } else {
                                LocaleListCompat.forLanguageTags(currentLanguage)
                            }
                            AppCompatDelegate.setApplicationLocales(locale)
                        }

                        Text(
                            text = stringResource(RString.app_language),
                            style = MaterialTheme.typography.bodyLarge
                        )
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
                                                style = MaterialTheme.typography.bodyMedium,
                                            )
                                            if (currentLanguage == it.langTag) {
                                                Icon(
                                                    imageVector = Icons.Rounded.Check,
                                                    contentDescription = null
                                                )
                                            }
                                        }

                                    }, onClick = {
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
                SettingItem(
                    onClick = { navigationManager.navigate(route = Destination.NetworkSetting) },
                    icon = {
                        Icon(imageVector = Icons.Rounded.NetworkWifi, contentDescription = null)
                    },
                    content = {
                        Text(
                            text = stringResource(RString.network_setting),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowForwardIos,
                            contentDescription = null
                        )
                    }
                )
            }

            item {
                SettingItem(
                    onClick = {
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
                    },
                    icon = {
                        Icon(imageVector = Icons.Rounded.AddLink, contentDescription = null)
                    },
                    content = {
                        Column {
                            Text(
                                text = stringResource(RString.default_open),
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = stringResource(RString.allow_open_link),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                )
            }
        }
    }
}

