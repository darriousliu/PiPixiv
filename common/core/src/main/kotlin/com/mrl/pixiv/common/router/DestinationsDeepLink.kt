package com.mrl.pixiv.common.router

import androidx.core.net.toUri

object DestinationsDeepLink {
    val illustRegex = "http(s)?://(www\\.)?pixiv\\.(net|me)(/.*)?/artworks/(\\d+)".toRegex()
    val userRegex = "http(s)?://(www\\.)?pixiv\\.(net|me)(/.*)?/users/(\\d+)".toRegex()

    private val BaseUri = listOf(
        "https://www.pixiv.net".toUri(),
        "http://www.pixiv.net".toUri(),
        "https://pixiv.net".toUri(),
        "http://pixiv.net".toUri(),
        "https://www.pixiv.me".toUri(),
        "http://www.pixiv.me".toUri(),
        "https://pixiv.me".toUri(),
        "http://pixiv.me".toUri(),
    )


    val ProfileDetailPattern = BaseUri.map {
        "$it/users"
    }
    val PicturePattern = BaseUri.map {
        "$it/artworks"
    }
}