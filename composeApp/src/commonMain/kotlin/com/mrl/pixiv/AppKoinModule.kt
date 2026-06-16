package com.mrl.pixiv

import com.mrl.pixiv.artwork.ArtworkModule
import com.mrl.pixiv.collection.CollectionModule
import com.mrl.pixiv.comment.CommentModule
import com.mrl.pixiv.common.CommonCoreModule
import com.mrl.pixiv.common.repository.RepositoryModule
import com.mrl.pixiv.follow.FollowModule
import com.mrl.pixiv.history.HistoryModule
import com.mrl.pixiv.home.HomeModule
import com.mrl.pixiv.latest.LatestModule
import com.mrl.pixiv.login.LoginModule
import com.mrl.pixiv.novel.NovelModule
import com.mrl.pixiv.picture.PictureModule
import com.mrl.pixiv.profile.ProfileModule
import com.mrl.pixiv.ranking.RankingModule
import com.mrl.pixiv.report.ReportModule
import com.mrl.pixiv.search.SearchModule
import com.mrl.pixiv.setting.SettingModule
import com.mrl.pixiv.splash.SplashModule
import org.koin.core.annotation.Module

@Module(
    includes = [
        CommonCoreModule::class,
        RepositoryModule::class,
        SplashModule::class,
        ArtworkModule::class,
        CollectionModule::class,
        CommentModule::class,
        FollowModule::class,
        HistoryModule::class,
        HomeModule::class,
        LatestModule::class,
        LoginModule::class,
        NovelModule::class,
        PictureModule::class,
        ProfileModule::class,
        RankingModule::class,
        ReportModule::class,
        SearchModule::class,
        SettingModule::class,
    ]
)
object AppKoinModule
