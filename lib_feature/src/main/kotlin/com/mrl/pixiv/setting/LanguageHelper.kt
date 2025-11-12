package com.mrl.pixiv.setting

import android.content.Context
import com.mrl.pixiv.common.util.LocaleHelper
import com.mrl.pixiv.common.util.RString
import com.mrl.pixiv.feature.R
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import org.xmlpull.v1.XmlPullParser

internal fun getLangs(context: Context): ImmutableList<Language> {
    val langs = mutableListOf<Language>()
    val parser = context.resources.getXml(R.xml.locales_config)
    var eventType = parser.eventType
    while (eventType != XmlPullParser.END_DOCUMENT) {
        if (eventType == XmlPullParser.START_TAG && parser.name == "locale") {
            for (i in 0..<parser.attributeCount) {
                if (parser.getAttributeName(i) == "name") {
                    val langTag = parser.getAttributeValue(i)
                    val displayName = LocaleHelper.getLocalizedDisplayName(langTag)
                    if (displayName.isNotEmpty()) {
                        langs.add(
                            Language(
                                langTag,
                                displayName,
                                LocaleHelper.getDisplayName(langTag)
                            )
                        )
                    }
                }
            }
        }
        eventType = parser.next()
    }

    langs.sortBy { it.displayName }
    langs.add(
        0,
        Language(
            context.getString(RString.label_default),
            context.getString(RString.label_default),
            null
        )
    )

    return langs.toImmutableList()
}

internal data class Language(
    val langTag: String,
    val displayName: String,
    val localizedDisplayName: String?,
)