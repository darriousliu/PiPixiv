package com.mrl.pixiv.latest

import com.mrl.pixiv.common.data.AppViewMode

enum class LatestPage {
    Trend,
    Collection,
    Following,
    MangaWatchlist,
    NovelNew,
    NovelWatchlist;

    companion object {
        private val illustPages = listOf(Trend, MangaWatchlist, Following, Collection)
        private val novelPages = listOf(
            Trend,
            NovelWatchlist,
            Following,
            Collection,
            NovelNew,
        )

        fun pagesFor(mode: AppViewMode): List<LatestPage> = when (mode) {
            AppViewMode.ILLUST -> illustPages
            AppViewMode.NOVEL -> novelPages
        }
    }
}
