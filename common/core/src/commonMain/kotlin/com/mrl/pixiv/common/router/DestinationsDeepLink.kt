package com.mrl.pixiv.common.router

object DestinationsDeepLink {
    val illustRegex = "http(s)?://(www\\.)?pixiv\\.(net|me)(/.*)?/artworks/(\\d+)".toRegex()
    val userRegex = "http(s)?://(www\\.)?pixiv\\.(net|me)(/.*)?/users/(\\d+)".toRegex()

    val novelRegex = "http(s)?://(www\\.)?pixiv\\.(net|me)(/.*)?/novel/show\\.php\\?id=(\\d+)".toRegex()
}