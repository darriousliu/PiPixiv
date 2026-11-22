package com.mrl.pixiv.setting.network

/**
 * 判断应用直接连接该主机时，是否需要 Android 17 的本地网络访问权限。
 *
 * 回环地址的流量不离开设备，不受本地网络保护限制；只有私有网段、链路本地地址和 mDNS 主机需要申请。
 */
internal fun String.requiresLocalNetworkPermission(): Boolean {
    val normalized = trim().trim('[', ']').lowercase()
    if (normalized.endsWith(".local")) return true

    val parts = normalized.split('.')
    if (parts.size != 4) return false
    val octets = parts.map { it.toIntOrNull() ?: return false }
    if (octets.any { it !in 0..255 }) return false

    return octets[0] == 10 ||
            octets[0] == 169 && octets[1] == 254 ||
            octets[0] == 172 && octets[1] in 16..31 ||
            octets[0] == 192 && octets[1] == 168
}
