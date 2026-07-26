package com.mrl.pixiv.setting.network

import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.mrl.pixiv.common.ai.AiLocalNetworkAccessGate
import com.mrl.pixiv.common.ai.AiLocalNetworkAccessState
import com.mrl.pixiv.common.ai.validateAiEndpoint
import com.mrl.pixiv.common.data.setting.UserPreference
import io.ktor.http.parseUrl

private const val ACCESS_LOCAL_NETWORK = "android.permission.ACCESS_LOCAL_NETWORK"

@Composable
actual fun LocalNetworkPermissionEffect(bypassSetting: UserPreference.BypassSetting) {
    LocalNetworkPermissionRequestEffect(
        key = bypassSetting,
        needsLocalNetworkAccess = bypassSetting.needsLocalNetworkAccess(),
    )
}

@Composable
actual fun AiLocalNetworkPermissionEffect(endpoint: String) {
    val validation = validateAiEndpoint(endpoint)
    val accessState by AiLocalNetworkAccessGate.state.collectAsState()

    LaunchedEffect(endpoint, validation.isLocalNetwork) {
        if (validation.isLocalNetwork) {
            AiLocalNetworkAccessGate.requestAccess(retryDenied = true)
        }
    }

    LocalNetworkPermissionRequestEffect(
        key = endpoint to accessState,
        needsLocalNetworkAccess = accessState == AiLocalNetworkAccessState.REQUESTED,
        onResult = AiLocalNetworkAccessGate::resolve,
    )
}

@Composable
private fun LocalNetworkPermissionRequestEffect(
    key: Any,
    needsLocalNetworkAccess: Boolean,
    onResult: (Boolean) -> Unit = {},
) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.CINNAMON_BUN) {
        LaunchedEffect(key, needsLocalNetworkAccess) {
            if (needsLocalNetworkAccess) onResult(true)
        }
        return
    }

    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        onResult(granted)
    }

    LaunchedEffect(key, needsLocalNetworkAccess) {
        if (!needsLocalNetworkAccess) return@LaunchedEffect
        val granted = ContextCompat.checkSelfPermission(
            context,
            ACCESS_LOCAL_NETWORK
        ) == PackageManager.PERMISSION_GRANTED

        if (granted) {
            onResult(true)
        } else {
            permissionLauncher.launch(ACCESS_LOCAL_NETWORK)
        }
    }
}

private fun UserPreference.BypassSetting.needsLocalNetworkAccess(): Boolean {
    return when (this) {
        UserPreference.BypassSetting.None -> false
        is UserPreference.BypassSetting.Proxy -> host.isLocalNetworkHost()
        is UserPreference.BypassSetting.SNI -> {
            parseUrl(url)?.host?.isLocalNetworkHost() == true ||
                    fallback.values.any { it.isLocalNetworkHost() }
        }
    }
}

private fun String.isLocalNetworkHost(): Boolean {
    val normalized = trim().trim('[', ']').lowercase()
    if (
        normalized == "localhost" ||
        normalized == "::1" ||
        normalized.endsWith(".local")
    ) {
        return true
    }

    val parts = normalized.split('.')
    if (parts.size != 4) return false
    val octets = parts.map { it.toIntOrNull() ?: return false }

    return octets[0] == 10 ||
            octets[0] == 127 ||
            octets[0] == 169 && octets[1] == 254 ||
            octets[0] == 172 && octets[1] in 16..31 ||
            octets[0] == 192 && octets[1] == 168
}
