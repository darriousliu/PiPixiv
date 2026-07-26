package com.mrl.pixiv.setting.network

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.mrl.pixiv.common.ai.AiLocalNetworkAccessGate
import com.mrl.pixiv.common.ai.AiLocalNetworkAccessState
import com.mrl.pixiv.common.ai.validateAiEndpoint
import com.mrl.pixiv.common.data.setting.UserPreference

@Composable
actual fun LocalNetworkPermissionEffect(bypassSetting: UserPreference.BypassSetting) = Unit

@Composable
actual fun AiLocalNetworkPermissionEffect(endpoint: String) {
    val accessState by AiLocalNetworkAccessGate.state.collectAsState()
    val endpointIsLocal = validateAiEndpoint(endpoint).isLocalNetwork
    LaunchedEffect(endpoint, endpointIsLocal, accessState) {
        if (endpointIsLocal || accessState == AiLocalNetworkAccessState.REQUESTED) {
            AiLocalNetworkAccessGate.resolve(granted = true)
        }
    }
}
