package com.mrl.pixiv.di

import com.mrl.pixiv.JvmAppModule
import org.koin.core.annotation.KoinApplication
import org.koin.dsl.KoinAppDeclaration
import org.koin.plugin.module.dsl.startKoin

@KoinApplication(modules = [JvmAppModule::class])
private class JvmKoinApplication

actual fun startApplicationKoin(appDeclaration: KoinAppDeclaration) {
    startKoin<JvmKoinApplication> {
        appDeclaration()
    }
}
