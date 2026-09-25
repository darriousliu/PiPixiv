package com.mrl.pixiv.common.network

import co.touchlab.kermit.Logger
import com.mrl.pixiv.common.data.setting.UserPreference.BypassSetting
import platform.Foundation.NSURLSessionConfiguration

// API、图片与后台下载共用同一配置，避免原生下载会话绕过应用内网络设置。
fun NSURLSessionConfiguration.configureNetworkProxy(
    setting: BypassSetting = NetworkUtil.bypassSetting,
) {
    if (setting is BypassSetting.SNI) {
        Logger.w(tag = "HttpClient") { "iOS 不支持 SNI，使用直连" }
    }
    connectionProxyDictionary = when (setting) {
        BypassSetting.System -> null
        // 空字典表示显式直连；全部 Enable 设为 0 在 iOS 27 上仍会继承系统代理。
        BypassSetting.Direct, is BypassSetting.SNI -> emptyMap<Any?, Any>()
        is BypassSetting.Proxy -> buildMap<Any?, Any> {
            put("HTTPEnable", 0)
            put("HTTPSEnable", 0)
            put("SOCKSEnable", 0)
            put("ProxyAutoConfigEnable", 0)
            put("ProxyAutoDiscoveryEnable", 0)
            when (setting.proxyType) {
                BypassSetting.Proxy.ProxyType.HTTP -> {
                    put("HTTPEnable", 1)
                    put("HTTPProxy", setting.host)
                    put("HTTPPort", setting.port)
                    put("HTTPSEnable", 1)
                    put("HTTPSProxy", setting.host)
                    put("HTTPSPort", setting.port)
                }
                BypassSetting.Proxy.ProxyType.SOCKS -> {
                    put("SOCKSEnable", 1)
                    put("SOCKSProxy", setting.host)
                    put("SOCKSPort", setting.port)
                }
            }
        }
    }
}
