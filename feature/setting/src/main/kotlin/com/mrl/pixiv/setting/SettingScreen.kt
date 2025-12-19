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
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material.icons.rounded.ViewModule
import androidx.compose.material.icons.rounded._18UpRating
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mrl.pixiv.common.repository.SettingRepository
import com.mrl.pixiv.common.router.NavigationManager
import com.mrl.pixiv.common.util.RString
import com.mrl.pixiv.common.util.throttleClick
import com.mrl.pixiv.setting.components.DropDownSelector
import org.koin.compose.koinInject

const val KEY_LANGUAGE = "language"
const val KEY_NETWORK_SETTING = "network_setting"
const val KEY_DEFAULT_OPEN_LINK = "default_open_link"
const val KEY_DIVIDER_1 = "divider_1"
const val KEY_PORTRAIT_SPAN_COUNT = "portrait_span_count"
const val KEY_LANDSCAPE_SPAN_COUNT = "landscape_span_count"
const val KEY_DIVIDER_2 = "divider_2"
const val KEY_DOWNLOAD_SINGLE_FOLDER_BY_USER = "download_single_folder_by_user"
const val KEY_FILE_NAME_FORMAT = "file_name_format"
const val KEY_R18_ENABLED = "r18_enabled"

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
    val userPreference by SettingRepository.userPreferenceFlow.collectAsStateWithLifecycle()

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
            modifier = modifier
                .padding(it)
                .padding(horizontal = 8.dp),
        ) {
            item(key = KEY_LANGUAGE) {
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
            item(key = KEY_NETWORK_SETTING) {
                // 网络设置
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
                            navigationManager.navigateToNetworkSettingScreen()
                        },
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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                item(KEY_DEFAULT_OPEN_LINK) {
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
                            },
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
            item(key = KEY_DIVIDER_1) {
                HorizontalDivider(modifier = Modifier.padding(8.dp))
            }
            item(key = KEY_PORTRAIT_SPAN_COUNT) {
                SpanCountSetting(
                    title = stringResource(RString.span_count_portrait),
                    currentSpanCount = userPreference.spanCountPortrait,
                    onSpanCountChange = SettingRepository::setSpanCountPortrait,
                )
            }
            item(key = KEY_LANDSCAPE_SPAN_COUNT) {
                SpanCountSetting(
                    title = stringResource(RString.span_count_landscape),
                    currentSpanCount = userPreference.spanCountLandscape,
                    onSpanCountChange = SettingRepository::setSpanCountLandscape,
                )
            }
            item(key = KEY_DIVIDER_2) {
                HorizontalDivider(modifier = Modifier.padding(8.dp))
            }
            item(key = KEY_DOWNLOAD_SINGLE_FOLDER_BY_USER) {
                ListItem(
                    headlineContent = {
                        Text(
                            text = stringResource(RString.download_single_folder_by_user_title),
                        )
                    },
                    modifier = Modifier
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
                        Column(
                            modifier = Modifier.fillMaxHeight(),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Switch(
                                checked = userPreference.downloadSubFolderByUser,
                                onCheckedChange = { SettingRepository.setDownloadSubFolderByUser(it) }
                            )
                        }
                    }
                )
            }
            item(key = KEY_FILE_NAME_FORMAT) {
                ListItem(
                    headlineContent = {
                        Text(
                            text = stringResource(RString.file_name_format_title),
                        )
                    },
                    modifier = Modifier
                        .throttleClick(
                            indication = ripple()
                        ) {
                            navigationManager.navigateToFileNameFormatScreen()
                        },
                    leadingContent = {
                        Icon(imageVector = Icons.Rounded.Save, contentDescription = null)
                    },
                    trailingContent = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowForwardIos,
                            contentDescription = null
                        )
                    }
                )
            }
            item(key = KEY_R18_ENABLED) {
                var showWarningDialog by rememberSaveable { mutableStateOf(false) }

                if (showWarningDialog) {
                    AlertDialog(
                        onDismissRequest = { showWarningDialog = false },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    SettingRepository.setIsR18Enabled(true)
                                    showWarningDialog = false
                                }
                            ) {
                                Text(text = stringResource(RString.confirm))
                            }
                        },
                        title = { Text(text = stringResource(RString.tips)) },
                        text = { Text(text = AnnotatedString.fromHtml(stringResource(RString.r18_alert_message))) },
                        dismissButton = {
                            TextButton(onClick = { showWarningDialog = false }) {
                                Text(text = stringResource(RString.cancel))
                            }
                        }
                    )
                }

                ListItem(
                    headlineContent = {
                        Text(
                            text = stringResource(RString.r18),
                        )
                    },
                    modifier = Modifier
                        .throttleClick(
                            indication = ripple()
                        ) {
                            if (!userPreference.isR18Enabled) {
                                showWarningDialog = true
                            } else {
                                SettingRepository.setIsR18Enabled(false)
                            }
                        },
                    leadingContent = {
                        Icon(imageVector = Icons.Rounded._18UpRating, contentDescription = null)
                    },
                    trailingContent = {
                        Switch(
                            checked = userPreference.isR18Enabled,
                            onCheckedChange = {
                                if (it) {
                                    showWarningDialog = true
                                } else {
                                    SettingRepository.setIsR18Enabled(false)
                                }
                            }
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun SpanCountSetting(
    title: String,
    currentSpanCount: Int,
    onSpanCountChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val options = listOf(
        2 to "2",
        3 to "3",
        4 to "4",
        -1 to stringResource(RString.span_count_adaptive),
    )

    val currentLabel = options.find { it.first == currentSpanCount }?.second
        ?: options.find { it.first == -1 }?.second ?: ""

    var expanded by remember { mutableStateOf(false) }

    ListItem(
        headlineContent = { Text(text = title) },
        modifier = modifier,
        leadingContent = { Icon(Icons.Rounded.ViewModule, contentDescription = null) },
        trailingContent = {
            DropDownSelector(
                modifier = Modifier.throttleClick {
                    expanded = !expanded
                },
                expanded = expanded,
                onDismissRequest = { expanded = false },
                current = currentLabel,
            ) {
                options.forEach { (count, label) ->
                    DropdownMenuItem(
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = label,
                                    modifier = Modifier.padding(16.dp),
                                )
                                if (currentSpanCount == count) {
                                    Icon(
                                        imageVector = Icons.Rounded.Check,
                                        contentDescription = null
                                    )
                                }
                            }
                        },
                        onClick = {
                            onSpanCountChange(count)
                            expanded = false
                        }
                    )
                }
            }
        }
    )
}
