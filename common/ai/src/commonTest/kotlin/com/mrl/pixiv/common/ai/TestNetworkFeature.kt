package com.mrl.pixiv.common.ai

import com.mrl.pixiv.common.data.setting.UserPreference
import com.mrl.pixiv.common.network.NetworkFeature

internal object TestNetworkFeature : NetworkFeature {
    override fun provideUserPreference(): UserPreference = UserPreference()

    override suspend fun provideUserAccessToken(): String = error("AI 测试不应请求 Pixiv 登录令牌")
}
