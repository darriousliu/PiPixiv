package com.mrl.pixiv.di

import com.mrl.pixiv.JvmAppModule
import org.koin.ksp.generated.module

actual val allModule = listOf(JvmAppModule.module)