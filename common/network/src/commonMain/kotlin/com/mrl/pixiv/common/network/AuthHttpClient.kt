package com.mrl.pixiv.common.network

import com.mrl.pixiv.common.data.setting.UserPreference.BypassSetting
import io.ktor.client.plugins.HttpSend
import io.ktor.client.plugins.plugin
import org.koin.core.annotation.Single

@Single
@AuthClient
fun authHttpClient() = routedPixivClient(::createAuthClient)

private fun createAuthClient(setting: BypassSetting) = baseHttpClient(setting).apply {
    plugin(HttpSend).intercept { request ->
        NetworkUtil.addAuthHeader(request)
        request.apply {
            headers.remove("Authorization")
        }
        execute(request)
    }
}
