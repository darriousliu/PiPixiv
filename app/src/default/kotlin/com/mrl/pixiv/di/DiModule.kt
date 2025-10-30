package com.mrl.pixiv.di

import com.mrl.pixiv.AppModule
import com.mrl.pixiv.FeatureModule
import com.mrl.pixiv.common.CommonModule
import org.koin.ksp.generated.module

val allModule = listOf(
    AppModule.module,
    CommonModule.module,
    FeatureModule.module,
)
