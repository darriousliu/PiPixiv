package com.mrl.pixiv.di

import com.mrl.pixiv.common.analytics.initKotzilla
import com.mrl.pixiv.common.util.isDebug
import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration

object Initialization {
    fun initKoin(platformKoinAppDeclaration: KoinAppDeclaration = {}) {
        startKoin {
            platformKoinAppDeclaration()
            initKotzilla(isDebug)
            modules(allModule)
        }
    }
}