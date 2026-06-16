package com.mrl.pixiv.common.repository

import com.mrl.pixiv.common.ai.CommonAiModule
import com.mrl.pixiv.common.datasource.local.LocalDataSourceModule
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Configuration
import org.koin.core.annotation.Module

@Module(includes = [CommonAiModule::class, LocalDataSourceModule::class])
@Configuration
@ComponentScan
object RepositoryModule
