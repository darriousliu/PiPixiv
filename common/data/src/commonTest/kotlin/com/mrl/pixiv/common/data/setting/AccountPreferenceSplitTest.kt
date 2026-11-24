package com.mrl.pixiv.common.data.setting

import com.mrl.pixiv.common.data.AppViewMode
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AccountPreferenceSplitTest {
    private val json = Json { encodeDefaults = true }

    @Test
    fun deviceAndAccountSettingsCoverEverySerializedKeyExactlyOnce() {
        val allKeys = json.encodeToJsonElement(UserPreference.serializer(), UserPreference()).jsonObject.keys
        val deviceKeys = json.encodeToJsonElement(DevicePreference.serializer(), DevicePreference()).jsonObject.keys
        val accountKeys = json.encodeToJsonElement(AccountPreference.serializer(), AccountPreference()).jsonObject.keys

        assertEquals(allKeys, deviceKeys + accountKeys)
        assertTrue(deviceKeys.intersect(accountKeys).isEmpty())
    }

    @Test
    fun splittingAndMergingPreservesEveryNonDefaultValue() {
        val preference = UserPreference(
            appLanguage = "ja",
            theme = SettingTheme.DARK.name,
            enableBypassSniffing = true,
            bypassSetting = UserPreference.BypassSetting.Proxy("example.invalid", 1080),
            isR18Enabled = true,
            imageHost = "images.example.invalid",
            hasShowBookmarkTip = true,
            downloadSubFolderByUser = true,
            spanCountPortrait = 3,
            spanCountLandscape = 5,
            fileNameFormat = "{title}",
            defaultPrivateBookmark = true,
            readClipboardOnSearch = true,
            appViewMode = AppViewMode.NOVEL,
            collectionViewMode = AppViewMode.NOVEL,
            historyViewMode = AppViewMode.NOVEL,
            readingTab = ReadingTab.MANGA_WATCHLIST,
            aiTranslationConfig = AiTranslationConfig(apiKey = "fixture-key"),
            browsingSettings = BrowsingSettings(previewImageQuality = PreviewImageQuality.HIGH),
            searchSettings = SearchSettings(searchResultDisplayMode = SearchResultDisplayMode.PAGED),
            historySettings = HistorySettings(maxEntries = 300),
            novelReaderSettings = NovelReaderSettings(fontSize = 22),
            useLiquidGlassNavigation = false,
            appApiProxyHost = "api.example.invalid",
            appearance = AppearanceSettings(
                themeColor = ThemeColor.CUSTOM.name,
                customColor = 0xFF207585,
                paletteStyle = AppPaletteStyle.FIDELITY.name,
                amoledDark = true,
            ),
        )
        val defaults = json.encodeToJsonElement(UserPreference.serializer(), UserPreference()).jsonObject
        val populated = json.encodeToJsonElement(UserPreference.serializer(), preference).jsonObject
        assertTrue(defaults.all { (key, value) -> populated[key] != value })

        assertEquals(preference.appearance, preference.toDevicePreference().appearance)
        assertEquals(preference, preference.toDevicePreference().toUserPreference(preference.toAccountPreference()))
    }
}
