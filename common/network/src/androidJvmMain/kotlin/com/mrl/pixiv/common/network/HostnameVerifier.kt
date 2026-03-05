package com.mrl.pixiv.common.network

import com.mrl.pixiv.common.data.Constants.hostMap
import com.mrl.pixiv.common.network.NetworkUtil.imageHost
import javax.net.ssl.HostnameVerifier

internal val hostnameVerifier = HostnameVerifier { hostname, session ->
    // 检查主机名是否是你期望连接的IP地址或域名
    hostname in hostMap.keys || hostname in hostMap.values || hostname == imageHost || hostname == "doh.dns.sb"
}