package com.mrl.pixiv.di

import com.mrl.pixiv.common.util.PhotoUtil
import com.mrl.pixiv.common.util.ZipUtil
import org.koin.core.KoinApplication
import org.koin.dsl.module

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
