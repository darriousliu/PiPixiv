package com.mrl.pixiv

import org.koin.core.module.dsl.viewModel
import org.koin.dsl.bind
import org.koin.dsl.module

object AppModule {
    val module = module {
        viewModel { _ -> com.mrl.pixiv.splash.SplashViewModel() } bind (com.mrl.pixiv.common.viewmodel.BaseMviViewModel::class)
    }
}