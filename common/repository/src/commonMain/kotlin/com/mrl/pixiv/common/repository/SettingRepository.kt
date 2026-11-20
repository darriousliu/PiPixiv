package com.mrl.pixiv.common.repository

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mrl.pixiv.common.data.AppViewMode
import com.mrl.pixiv.common.data.setting.AccountPreference
import com.mrl.pixiv.common.data.setting.AiTranslationConfig
import com.mrl.pixiv.common.data.setting.BrowsingSettings
import com.mrl.pixiv.common.data.setting.DevicePreference
import com.mrl.pixiv.common.data.setting.HistorySettings
import com.mrl.pixiv.common.data.setting.NovelReaderSettings
import com.mrl.pixiv.common.data.setting.SearchFilterPreferences
import com.mrl.pixiv.common.data.setting.SearchSettings
import com.mrl.pixiv.common.data.setting.SettingTheme
import com.mrl.pixiv.common.data.setting.UserPreference
import com.mrl.pixiv.common.data.setting.toAccountPreference
import com.mrl.pixiv.common.data.setting.toDevicePreference
import com.mrl.pixiv.common.data.setting.toUserPreference
import com.mrl.pixiv.common.mmkv.AccountMMKVProperty
import com.mrl.pixiv.common.mmkv.AccountScoped
import com.mrl.pixiv.common.mmkv.AccountStore
import com.mrl.pixiv.common.mmkv.MMKVApp
import com.mrl.pixiv.common.mmkv.mmkvSerializable
import com.mrl.pixiv.common.mmkv.withAccountStorageLock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

val requireUserPreferenceValue: UserPreference
    get() = SettingRepository.userPreferenceFlow.value

val requireUserPreferenceFlow
    get() = SettingRepository.userPreferenceFlow

internal const val DEVICE_PREFERENCE_KEY = "devicePreference"
internal const val ACCOUNT_PREFERENCE_KEY = "accountPreference"

object SettingRepository : MMKVApp, AccountScoped {
    private val deviceField = mmkvSerializable(DevicePreference())
    private val accountPreference = AccountMMKVProperty(
        mmkvSerializable(AccountPreference()), ACCOUNT_PREFERENCE_KEY, AccountPreference(),
    )
    private var devicePreference = deviceField.read(this, DEVICE_PREFERENCE_KEY)
    private val userPreference = MutableStateFlow(devicePreference.toUserPreference(AccountPreference()))
    val userPreferenceFlow = userPreference.asStateFlow()

    override fun reload() = withAccountStorageLock {
        devicePreference = deviceField.read(this, DEVICE_PREFERENCE_KEY)
        accountPreference.reload(AccountStore.activeUserId)
        publish()
    }

    fun preferenceForAccount(userId: Long): UserPreference = withAccountStorageLock {
        devicePreference.toUserPreference(accountPreference.read(userId))
    }

    private fun publish() {
        userPreference.value = devicePreference.toUserPreference(accountPreference.flow.value)
    }

    private fun update(userId: Long = AccountStore.activeUserId, transform: (UserPreference) -> UserPreference) {
        withAccountStorageLock {
            val updated = transform(preferenceForAccount(userId))
            val nextDevicePreference = updated.toDevicePreference()
            if (nextDevicePreference != devicePreference) {
                deviceField.write(this, DEVICE_PREFERENCE_KEY, nextDevicePreference)
                devicePreference = nextDevicePreference
            }
            accountPreference.write(userId, updated.toAccountPreference())
            publish()
        }
    }

    val settingTheme
        get() = enumValueOf<SettingTheme>(
            userPreferenceFlow.value.theme.ifEmpty {
                SettingTheme.SYSTEM.toString()
            }
        )

    @Composable
    fun <T> StateFlow<UserPreference>.collectAsStateWithLifecycle(
        lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current,
        minActiveState: Lifecycle.State = Lifecycle.State.STARTED,
        context: CoroutineContext = EmptyCoroutineContext,
        block: UserPreference.() -> T
    ): State<T> {
        return map { it.block() }.collectAsStateWithLifecycle(
            userPreferenceFlow.value.block(),
            lifecycleOwner,
            minActiveState,
            context
        )
    }

    fun setSettingTheme(theme: SettingTheme) = update {
        it.copy(theme = theme.toString())
    }

    fun setPictureSourceHost(host: String) = update {
        it.copy(imageHost = host)
    }

    fun setHasShowBookmarkTip(hasShow: Boolean) = update {
        it.copy(hasShowBookmarkTip = hasShow)
    }

    fun setDownloadSubFolderByUser(enable: Boolean) = update {
        it.copy(downloadSubFolderByUser = enable)
    }

    fun setSpanCountPortrait(count: Int) = update {
        it.copy(spanCountPortrait = count)
    }

    fun setSpanCountLandscape(count: Int) = update {
        it.copy(spanCountLandscape = count)
    }

    fun setIsR18Enabled(enable: Boolean) = update {
        it.copy(isR18Enabled = enable)
    }

    fun setFileNameFormat(format: String) = update {
        it.copy(fileNameFormat = format)
    }

    fun setDefaultPrivateBookmark(enable: Boolean) = update {
        it.copy(defaultPrivateBookmark = enable)
    }

    fun setReadClipboardOnSearch(enable: Boolean) = update {
        it.copy(readClipboardOnSearch = enable)
    }

    fun setAppViewMode(mode: AppViewMode) = update {
        it.copy(appViewMode = mode)
    }

    fun setAiTranslationConfig(config: AiTranslationConfig) = update {
        it.copy(aiTranslationConfig = config)
    }

    fun setBrowsingSettings(settings: BrowsingSettings) {
        val previous = userPreference.value.browsingSettings
        update {
            it.copy(browsingSettings = settings)
        }
        NovelFilterSettingsChanges.notifyIfChanged(
            previous = previous,
            current = settings,
        )
    }

    fun setSearchSettings(settings: SearchSettings) = update {
        it.copy(searchSettings = settings)
    }

    fun updateSearchFilterDefaults(
        transform: SearchFilterPreferences.() -> SearchFilterPreferences,
    ) = update {
        it.copy(searchSettings = it.searchSettings.updateFilterDefaults(transform))
    }

    fun rememberSearchFilter(mode: AppViewMode, filter: SearchFilterPreferences) =
        update {
            it.copy(searchSettings = it.searchSettings.rememberFilter(mode, filter))
        }

    fun setHistorySettings(settings: HistorySettings) = update {
        it.copy(historySettings = settings.normalized())
    }

    fun setNovelReaderSettings(settings: NovelReaderSettings) = update {
        it.copy(novelReaderSettings = settings.normalized())
    }

    fun updateSettings(block: UserPreference.() -> UserPreference) {
        update(transform = block)
    }

    fun restore(preference: UserPreference, userId: Long = AccountStore.activeUserId) {
        update(userId) { current ->
            preference.copy(appApiProxyHost = current.appApiProxyHost)
        }
    }
}
