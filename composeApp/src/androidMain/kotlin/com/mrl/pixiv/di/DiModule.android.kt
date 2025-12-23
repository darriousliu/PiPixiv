package com.mrl.pixiv.di

import com.mrl.pixiv.AndroidAppModule
import org.koin.ksp.generated.module

actual val allModule = listOf(
    AndroidAppModule.module,
)