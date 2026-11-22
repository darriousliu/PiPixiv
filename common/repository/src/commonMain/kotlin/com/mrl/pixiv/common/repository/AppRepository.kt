package com.mrl.pixiv.common.repository

import com.mrl.pixiv.common.analytics.isUsageAnalyticsAvailable
import com.mrl.pixiv.common.mmkv.MMKVApp
import com.mrl.pixiv.common.mmkv.asMutableStateFlow
import com.mrl.pixiv.common.mmkv.mmkvBool
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.mrl.pixiv.common.analytics.setUsageAnalyticsEnabled as applyUsageAnalyticsEnabled

object AppRepository : MMKVApp {
    var isSafGranted by mmkvBool(false)

    private val usageAnalyticsConsent by mmkvBool(default = isUsageAnalyticsAvailable).asMutableStateFlow()
    val usageAnalyticsEnabled: StateFlow<Boolean> by lazy { usageAnalyticsConsent.asStateFlow() }

    fun setUsageAnalyticsEnabled(enabled: Boolean) {
        if (!isUsageAnalyticsAvailable || usageAnalyticsConsent.value == enabled) return

        usageAnalyticsConsent.value = enabled
        applyUsageAnalyticsEnabled(enabled)
    }
}
