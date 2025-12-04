package com.mrl.pixiv.di

import com.mrl.pixiv.AppModule
import org.koin.ksp.generated.module

val allModule = listOf(
    AppModule.module,
)
