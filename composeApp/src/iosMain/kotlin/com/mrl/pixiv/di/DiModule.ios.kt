package com.mrl.pixiv.di

import com.mrl.pixiv.IosAppModule
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.bind
import org.koin.dsl.module
import org.koin.ksp.generated.*

actual val allModule = listOf(
    IosAppModule.module,
)

public val com_mrl_pixiv_IosAppModule : Module get() = module {
    defineComMrlPixivArtworkArtworkViewModel()
    defineComMrlPixivCollectionCollectionViewModel()
    defineComMrlPixivCommentCommentViewModel()
    defineComMrlPixivCommonDatasourceLocalgetDatabaseBuilder()
    defineComMrlPixivCommonDatasourceLocalprovideDatabase()
    defineComMrlPixivCommonDatasourceLocalprovideDownloadDao()
    defineComMrlPixivCommonNetworkapiHttpClient()
    defineComMrlPixivCommonNetworkauthHttpClient()
    defineComMrlPixivCommonNetworkimageHttpClient()
    defineComMrlPixivCommonRepositoryDownloadManager()
    defineComMrlPixivCommonRepositoryIosDownloadStrategy()
    defineComMrlPixivCommonRepositoryNetworkFeatureImpl()
    defineComMrlPixivCommonRepositoryPagingHistoryIllustPagingSource()
    defineComMrlPixivCommonRouterNavigationManager()
    defineComMrlPixivFollowFollowingViewModel()
    defineComMrlPixivHistoryHistoryViewModel()
    defineComMrlPixivHomeHomeViewModel()
    defineComMrlPixivLatestLatestViewModel()
    defineComMrlPixivLoginBrowserDownloadBrowserViewModel()
    defineComMrlPixivLoginLoginViewModel()
    defineComMrlPixivLoginOauthOAuthLoginViewModel()
    defineComMrlPixivPicturePictureViewModel()
    defineComMrlPixivProfileDetailProfileDetailViewModel()
    defineComMrlPixivProfileProfileViewModel()
    defineComMrlPixivRankingRankingViewModel()
    defineComMrlPixivReportReportCommentViewModel()
    defineComMrlPixivSearchPreviewSearchPreviewViewModel()
    defineComMrlPixivSearchResultSearchResultViewModel()
    defineComMrlPixivSearchSearchViewModel()
    defineComMrlPixivSettingAppdataAppDataViewModel()
    defineComMrlPixivSettingBlockBlockSettingsViewModel()
    defineComMrlPixivSettingDownloadDownloadViewModel()
    defineComMrlPixivSettingSettingViewModel()
    defineComMrlPixivSplashSplashViewModel()
    viewModel() { _ -> com.mrl.pixiv.splash.SplashViewModel()} bind(com.mrl.pixiv.common.viewmodel.BaseMviViewModel::class)
}
public val com.mrl.pixiv.IosAppModule.module : org.koin.core.module.Module get() = com_mrl_pixiv_IosAppModule