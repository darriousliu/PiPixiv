package com.mrl.pixiv.di

import com.mrl.pixiv.IosAppModule
import org.koin.ksp.generated.module

actual val allModule = listOf(
    IosAppModule.module
)