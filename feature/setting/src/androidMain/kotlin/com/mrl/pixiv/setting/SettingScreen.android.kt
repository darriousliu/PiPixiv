package com.mrl.pixiv.setting

import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddLink
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import androidx.core.os.LocaleListCompat
import com.mrl.pixiv.common.util.RStrings
import com.mrl.pixiv.common.util.throttleClick
import com.mrl.pixiv.strings.allow_open_link
import com.mrl.pixiv.strings.default_open
import org.jetbrains.compose.resources.stringResource

actual fun getInitialLanguages(): String? {
    return AppCompatDelegate.getApplicationLocales().get(0)?.toLanguageTag()
}

actual fun triggerLocaleChange(currentLanguage: String, labelDefault: String) {
    val locale = if (currentLanguage == labelDefault) {
        LocaleListCompat.getEmptyLocaleList()
    } else {
        LocaleListCompat.forLanguageTags(currentLanguage)
    }
    AppCompatDelegate.setApplicationLocales(locale)
}

@Composable
actual fun LazyListScope.AppLinkItem() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        item(KEY_DEFAULT_OPEN_LINK) {
            val context = LocalContext.current
            ListItem(
                headlineContent = {
                    Text(
                        text = stringResource(RStrings.default_open),
                    )
                },
                modifier = Modifier
                    .height(IntrinsicSize.Min)
                    .throttleClick(
                        indication = ripple()
                    ) {
                        try {
                            val intent = Intent().apply {
                                action = Settings.ACTION_APP_OPEN_BY_DEFAULT_SETTINGS
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
                        text = stringResource(RStrings.allow_open_link),
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