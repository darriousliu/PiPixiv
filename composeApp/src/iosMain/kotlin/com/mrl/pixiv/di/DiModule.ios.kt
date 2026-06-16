package com.mrl.pixiv.di

import com.mrl.pixiv.IosAppModule
import com.mrl.pixiv.common.util.PhotoUtil
import com.mrl.pixiv.common.util.ZipUtil
import org.koin.core.KoinApplication
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module
import org.koin.plugin.module.dsl.startKoin
import org.koin.core.annotation.KoinApplication as KoinApp

@KoinApp(modules = [IosAppModule::class])
private class IosKoinApplication

actual fun startApplicationKoin(appDeclaration: KoinAppDeclaration) {
    startKoin<IosKoinApplication> {
        appDeclaration()
    }
}

fun KoinApplication.initIOSKoin(
    di: List<Any>,
) {
    val zipUtil = di.find { it is ZipUtil } as? ZipUtil
    val photoUtil = di.find { it is PhotoUtil } as? PhotoUtil
    modules(
        module {
            zipUtil?.let { single<ZipUtil> { zipUtil } }
            photoUtil?.let { single<PhotoUtil> { photoUtil } }
        }
    )
}
