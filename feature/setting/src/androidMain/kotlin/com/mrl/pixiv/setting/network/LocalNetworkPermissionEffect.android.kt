package com.mrl.pixiv.setting.network

import android.content.pm.PackageManager
import android.net.ConnectivityManager
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
    val context = LocalContext.current
    val systemProxy = context.getSystemService(ConnectivityManager::class.java)?.defaultProxy
    LocalNetworkPermissionRequestEffect(
        key = bypassSetting,
        needsLocalNetworkAccess = when (bypassSetting) {
            UserPreference.BypassSetting.System -> {
                // 代理软件常把系统代理设为 127.0.0.1，PAC 模式的主机名固定为 localhost，二者均为回环流量，不触发授权。
                systemProxy?.host?.requiresLocalNetworkPermission() == true ||
                    systemProxy?.pacFileUrl?.host?.requiresLocalNetworkPermission() == true
            }
            else -> bypassSetting.needsLocalNetworkAccess()
        },
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
        UserPreference.BypassSetting.System, UserPreference.BypassSetting.Direct -> false
        is UserPreference.BypassSetting.Proxy -> host.requiresLocalNetworkPermission()
        is UserPreference.BypassSetting.ECH -> parseUrl(url)?.host?.requiresLocalNetworkPermission() == true
    }
}
