package com.mrl.pixiv.common.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

object VersionManager {
    private val _hasNewVersion = MutableStateFlow(false)
    val hasNewVersion = _hasNewVersion.asStateFlow()

    fun checkUpdate() {
        // TODO: Implement update check
        _hasNewVersion.value = true // Mock update available for now
    }
}
