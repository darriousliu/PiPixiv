package com.mrl.pixiv.common.repository

import com.mrl.pixiv.common.data.setting.UserPreference
import com.mrl.pixiv.common.network.NetworkFeature
import kotlinx.coroutines.currentCoroutineContext
import org.koin.core.annotation.Single
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

@Single(binds = [NetworkFeature::class])
class NetworkFeatureImpl : NetworkFeature {
    override fun provideUserPreference(): UserPreference =
        SettingRepository.userPreferenceFlow.value

    override suspend fun provideUserAccessToken(): String {
        val userId = currentCoroutineContext()[RequestAccount]?.userId
        return if (userId != null) {
            AuthManager.requireUserAccessToken(userId)
        } else {
            AuthManager.requireUserAccessToken()
        }
    }
}

internal class RequestAccount(val userId: Long) : AbstractCoroutineContextElement(Key) {
    companion object Key : CoroutineContext.Key<RequestAccount>
}
