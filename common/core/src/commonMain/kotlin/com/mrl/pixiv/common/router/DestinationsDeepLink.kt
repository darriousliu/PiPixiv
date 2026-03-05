package com.mrl.pixiv.common.router

object DestinationsDeepLink {
    val illustRegex = "http(s)?://(www\\.)?pixiv\\.(net|me)(/.*)?/artworks/(\\d+)".toRegex()
    val userRegex = "http(s)?://(www\\.)?pixiv\\.(net|me)(/.*)?/users/(\\d+)".toRegex()
}