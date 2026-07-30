package com.mrl.pixiv.latest

import com.mrl.pixiv.common.data.AppViewMode

enum class LatestPage {
    Trend,
    Collection,
    Following,
    NovelNew,
    NovelWatchlist;

    companion object {
        private val illustPages = listOf(Trend, Collection, Following)
        private val novelPages = listOf(
            Trend,
            Collection,
            Following,
            NovelNew,
            NovelWatchlist,
        )

        fun pagesFor(mode: AppViewMode): List<LatestPage> = when (mode) {
            AppViewMode.ILLUST -> illustPages
            AppViewMode.NOVEL -> novelPages
        }
    }
}
