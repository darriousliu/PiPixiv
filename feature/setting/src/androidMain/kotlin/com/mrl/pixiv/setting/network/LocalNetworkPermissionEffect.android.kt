package com.mrl.pixiv.setting.network

import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.mrl.pixiv.common.data.setting.UserPreference
import io.ktor.http.parseUrl

private const val ACCESS_LOCAL_NETWORK = "android.permission.ACCESS_LOCAL_NETWORK"

@Composable
actual fun LocalNetworkPermissionEffect(bypassSetting: UserPreference.BypassSetting) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.CINNAMON_BUN) return

    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {}

    LaunchedEffect(bypassSetting) {
        if (!bypassSetting.needsLocalNetworkAccess()) return@LaunchedEffect
        val granted = ContextCompat.checkSelfPermission(
            context,
            ACCESS_LOCAL_NETWORK
        ) == PackageManager.PERMISSION_GRANTED

        if (!granted) {
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
