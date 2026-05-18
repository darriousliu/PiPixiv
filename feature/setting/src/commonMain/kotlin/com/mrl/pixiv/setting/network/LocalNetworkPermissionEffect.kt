package com.mrl.pixiv.setting.network

import androidx.compose.runtime.Composable
import com.mrl.pixiv.common.data.setting.UserPreference

@Composable
expect fun LocalNetworkPermissionEffect(bypassSetting: UserPreference.BypassSetting)
