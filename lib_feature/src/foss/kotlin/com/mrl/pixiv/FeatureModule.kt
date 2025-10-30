package com.mrl.pixiv

import org.koin.core.module.dsl.viewModel
import org.koin.dsl.bind
import org.koin.dsl.binds
import org.koin.dsl.module

object FeatureModule {
    val module = module {
        viewModel() { _ -> com.mrl.pixiv.collection.CollectionViewModel(uid = get()) } bind (com.mrl.pixiv.common.viewmodel.BaseMviViewModel::class)
        viewModel() { _ -> com.mrl.pixiv.follow.FollowingViewModel(uid = get()) } bind (com.mrl.pixiv.common.viewmodel.BaseMviViewModel::class)
        viewModel() { _ -> com.mrl.pixiv.history.HistoryViewModel() } binds (arrayOf(
            com.mrl.pixiv.common.viewmodel.BaseMviViewModel::class,
            org.koin.core.component.KoinComponent::class
        ))
        viewModel() { _ -> com.mrl.pixiv.home.HomeViewModel() } binds (arrayOf(
            com.mrl.pixiv.common.viewmodel.BaseMviViewModel::class,
            org.koin.core.component.KoinComponent::class
        ))
        viewModel() { _ -> com.mrl.pixiv.latest.LatestViewModel() }
        viewModel() { _ -> com.mrl.pixiv.login.LoginViewModel() } bind (com.mrl.pixiv.common.viewmodel.BaseMviViewModel::class)
        viewModel() { _ -> com.mrl.pixiv.login.oauth.OAuthLoginViewModel() } bind (com.mrl.pixiv.common.viewmodel.BaseMviViewModel::class)
        viewModel() { _ ->
            com.mrl.pixiv.picture.PictureViewModel(
                illust = getOrNull(),
                illustId = getOrNull()
            )
        } binds (arrayOf(
            com.mrl.pixiv.common.viewmodel.BaseMviViewModel::class,
            org.koin.core.component.KoinComponent::class
        ))
        viewModel() { _ -> com.mrl.pixiv.profile.ProfileViewModel() } bind (com.mrl.pixiv.common.viewmodel.BaseMviViewModel::class)
        viewModel() { _ -> com.mrl.pixiv.profile.detail.ProfileDetailViewModel(uid = getOrNull()) } bind (com.mrl.pixiv.common.viewmodel.BaseMviViewModel::class)
        viewModel() { _ -> com.mrl.pixiv.search.SearchViewModel() } bind (com.mrl.pixiv.common.viewmodel.BaseMviViewModel::class)
        viewModel() { _ -> com.mrl.pixiv.search.preview.SearchPreviewViewModel() } bind (com.mrl.pixiv.common.viewmodel.BaseMviViewModel::class)
        viewModel() { _ -> com.mrl.pixiv.search.result.SearchResultViewModel(searchWords = get()) } binds (arrayOf(
            com.mrl.pixiv.common.viewmodel.BaseMviViewModel::class,
            org.koin.core.component.KoinComponent::class
        ))
        viewModel() { _ -> com.mrl.pixiv.setting.SettingViewModel() } bind (com.mrl.pixiv.common.viewmodel.BaseMviViewModel::class)
    }
}