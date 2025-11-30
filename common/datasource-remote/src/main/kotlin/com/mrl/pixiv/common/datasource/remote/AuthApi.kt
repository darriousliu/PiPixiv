package com.mrl.pixiv.common.datasource.remote

import com.mrl.pixiv.common.data.auth.AuthTokenResp
import de.jensklingenberg.ktorfit.http.Field
import de.jensklingenberg.ktorfit.http.FormUrlEncoded
import de.jensklingenberg.ktorfit.http.POST

interface AuthApi {
    @FormUrlEncoded
    @POST("auth/token")
    suspend fun refreshToken(
        @Field("client_id") clientId: String,
        @Field("client_secret") clientSecret: String,
        @Field("grant_type") grantType: String,
        @Field("refresh_token") refreshToken: String,
        @Field("get_secure_url") getSecureUrl: Int,
        @Field("include_policy") includePolicy: Boolean,
    ): AuthTokenResp

    @FormUrlEncoded
    @POST("auth/token")
    suspend fun login(
        @Field("client_id") clientId: String,
        @Field("client_secret") clientSecret: String,
        @Field("grant_type") grantType: String,
        @Field("redirect_uri") redirectUri: String,
        @Field("code") code: String,
        @Field("code_verifier") codeVerifier: String,
        @Field("get_secure_url") getSecureUrl: Int,
        @Field("include_policy") includePolicy: Boolean,
    ): AuthTokenResp
}