package com.mrl.pixiv.common.repository

import com.mrl.pixiv.common.data.setting.UserPreference
import com.mrl.pixiv.common.network.NetworkFeature
import org.koin.core.annotation.Single

@Single(binds = [NetworkFeature::class])
class NetworkFeatureImpl : NetworkFeature {
    override fun provideUserPreference(): UserPreference =
        SettingRepository.userPreferenceFlow.value


    override suspend fun provideUserAccessToken() = AuthManager.requireUserAccessToken()
}