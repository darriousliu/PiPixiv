package com.mrl.pixiv.common.network

import com.mrl.pixiv.common.data.setting.UserPreference.BypassSetting
import io.ktor.client.plugins.HttpSend
import io.ktor.client.plugins.plugin
import io.ktor.http.HttpStatusCode
import org.koin.core.annotation.Single

@Single
@ApiClient
fun apiHttpClient() = routedPixivClient(::createApiClient)

private fun createApiClient(setting: BypassSetting) = baseHttpClient(setting).apply {
    plugin(HttpSend).apply {
        intercept { request ->
            NetworkUtil.addAuthHeader(request)
            execute(request)
        }
        intercept { request ->
            var call = execute(request)
            if (call.response.status in HttpStatusCode.BadRequest..HttpStatusCode.RequestHeaderFieldTooLarge) {
                NetworkUtil.requireUserAccessToken()
                NetworkUtil.addAuthHeader(request)
                call = execute(request)
            }
            call
        }
    }
}
