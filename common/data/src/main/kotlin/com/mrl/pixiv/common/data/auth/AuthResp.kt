package com.mrl.pixiv.common.data.auth

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AuthTokenResp(
    @SerialName("access_token")
    val accessToken: String = "",

    @SerialName("expires_in")
    val expiresIn: Long = 0,

    @SerialName("token_type")
    val tokenType: String = "",

    val scope: String = "",

    @SerialName("refresh_token")
    val refreshToken: String = "",

    val user: AuthUser? = null,
    val response: AuthTokenResp? = null
)

@Serializable
data class AuthUser(
    @SerialName("profile_image_urls")
    val profileImageUrls: ProfileImageUrls = ProfileImageUrls(),

    val id: String = "",
    val name: String = "",
    val account: String = "",

    @SerialName("mail_address")
    val mailAddress: String = "",

    @SerialName("is_premium")
    val isPremium: Boolean = false,

    @SerialName("x_restrict")
    val xRestrict: Long = 0,

    @SerialName("is_mail_authorized")
    val isMailAuthorized: Boolean = false,

    @SerialName("require_policy_agreement")
    val requirePolicyAgreement: Boolean = true
)

@Serializable
data class ProfileImageUrls(
    @SerialName("px_16x16")
    val px16X16: String = "",

    @SerialName("px_50x50")
    val px50X50: String = "",

    @SerialName("px_170x170")
    val px170X170: String = ""
)
