package com.mrl.pixiv.common.repository

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mrl.pixiv.common.data.setting.SettingTheme
import com.mrl.pixiv.common.data.setting.UserPreference
import com.mrl.pixiv.common.mmkv.MMKVUser
import com.mrl.pixiv.common.mmkv.asMutableStateFlow
import com.mrl.pixiv.common.mmkv.mmkvSerializable
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

val requireUserPreferenceValue: UserPreference
    get() = SettingRepository.userPreferenceFlow.value

val requireUserPreferenceFlow
    get() = SettingRepository.userPreferenceFlow

object SettingRepository : MMKVUser {
    private val defaultUserPreference = UserPreference()
    private val userPreference by mmkvSerializable(defaultUserPreference).asMutableStateFlow()
    val userPreferenceFlow = userPreference.asStateFlow()

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
            defaultUserPreference.block(),
            lifecycleOwner,
            minActiveState,
            context
        )
    }

    fun setSettingTheme(theme: SettingTheme) = userPreference.update {
        it.copy(theme = theme.toString())
    }

    fun setPictureSourceHost(host: String) = userPreference.update {
        it.copy(imageHost = host)
    }

    fun setHasShowBookmarkTip(hasShow: Boolean) = userPreference.update {
        it.copy(hasShowBookmarkTip = hasShow)
    }

    fun setDownloadSubFolderByUser(enable: Boolean) = userPreference.update {
        it.copy(downloadSubFolderByUser = enable)
    }

    fun setSpanCountPortrait(count: Int) = userPreference.update {
        it.copy(spanCountPortrait = count)
    }

    fun setSpanCountLandscape(count: Int) = userPreference.update {
        it.copy(spanCountLandscape = count)
    }

    fun setIsR18Enabled(enable: Boolean) = userPreference.update {
        it.copy(isR18Enabled = enable)
    }

    fun setFileNameFormat(format: String) = userPreference.update {
        it.copy(fileNameFormat = format)
    }

    fun updateSettings(block: UserPreference.() -> UserPreference) {
        userPreference.update(block)
    }

    fun clear() {
        userPreference.value = UserPreference()
    }

    fun restore(preference: UserPreference) {
        userPreference.value = preference
    }
}