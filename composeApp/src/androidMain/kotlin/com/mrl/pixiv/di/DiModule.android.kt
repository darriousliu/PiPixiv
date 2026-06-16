package com.mrl.pixiv.di

import com.mrl.pixiv.AndroidAppModule
import org.koin.core.annotation.KoinApplication
import org.koin.dsl.KoinAppDeclaration
import org.koin.plugin.module.dsl.startKoin

@KoinApplication(modules = [AndroidAppModule::class])
private class AndroidKoinApplication

actual fun startApplicationKoin(appDeclaration: KoinAppDeclaration) {
    startKoin<AndroidKoinApplication> {
        appDeclaration()
    }
}
