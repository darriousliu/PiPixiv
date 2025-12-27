package com.mrl.pixiv.di

import com.mrl.pixiv.IosAppModule
import com.mrl.pixiv.common.util.PhotoUtil
import com.mrl.pixiv.common.util.ZipUtil
import org.koin.core.KoinApplication
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.bind
import org.koin.dsl.module
import org.koin.ksp.generated.*

actual val allModule = listOf(
    IosAppModule.module,
)

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