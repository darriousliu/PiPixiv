package com.mrl.pixiv.common.network

import io.ktor.client.HttpClient
import org.koin.core.qualifier.named
import org.koin.dsl.module

val networkModule = module {
    single<HttpClient>(qualifier = named<ApiClient>()) {
        apiHttpClient()
    }
    single<HttpClient>(qualifier = named<AuthClient>()) {
        authHttpClient()
    }
    single<HttpClient>(qualifier = named<ImageClient>()) {
        imageHttpClient()
    }
}
