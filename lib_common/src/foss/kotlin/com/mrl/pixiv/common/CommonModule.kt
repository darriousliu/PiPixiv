package com.mrl.pixiv.common

import org.koin.dsl.bind
import org.koin.dsl.module

object CommonModule {
    val module = module {
        factory() { _ -> com.mrl.pixiv.common.repository.paging.HistoryIllustPagingSource() } bind (androidx.paging.PagingSource::class)
        single() { _ -> com.mrl.pixiv.common.router.NavigationManager(initialBackStack = get()) }
        single(qualifier = org.koin.core.qualifier.StringQualifier("com.mrl.pixiv.common.network.ApiClient")) { _ -> com.mrl.pixiv.common.network.apiHttpClient() } bind (io.ktor.client.HttpClient::class)
        single(qualifier = org.koin.core.qualifier.StringQualifier("com.mrl.pixiv.common.network.AuthClient")) { _ -> com.mrl.pixiv.common.network.authHttpClient() } bind (io.ktor.client.HttpClient::class)
        single(qualifier = org.koin.core.qualifier.StringQualifier("com.mrl.pixiv.common.network.ImageClient")) { _ -> com.mrl.pixiv.common.network.imageHttpClient() } bind (io.ktor.client.HttpClient::class)
    }
}