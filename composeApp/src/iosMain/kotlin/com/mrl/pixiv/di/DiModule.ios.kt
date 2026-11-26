package com.mrl.pixiv.di

import coil3.PlatformContext
import com.mrl.pixiv.common.analytics.FLAVOR
import com.mrl.pixiv.common.util.AppUtil
import com.mrl.pixiv.common.util.PhotoUtil
import com.mrl.pixiv.common.util.ZipUtil
import org.koin.core.KoinApplication
import org.koin.dsl.module

fun KoinApplication.initIOSKoin(
    di: List<Any>,
) {
    AppUtil.init(PlatformContext.INSTANCE, FLAVOR)
    val zipUtil = di.find { it is ZipUtil } as? ZipUtil
    val photoUtil = di.find { it is PhotoUtil } as? PhotoUtil
    modules(
        module {
            zipUtil?.let { single<ZipUtil> { zipUtil } }
            photoUtil?.let { single<PhotoUtil> { photoUtil } }
        }
    )
}
