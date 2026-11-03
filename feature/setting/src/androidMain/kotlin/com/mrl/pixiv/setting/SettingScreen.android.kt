package com.mrl.pixiv.setting

import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import androidx.core.os.LocaleListCompat

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

actual fun LazyListScope.appLinkItem() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        item(KEY_DEFAULT_OPEN_LINK) {
            val context = LocalContext.current
            AppLinkSettingItem(
                onClick = {
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
            )
        }
    }
}
