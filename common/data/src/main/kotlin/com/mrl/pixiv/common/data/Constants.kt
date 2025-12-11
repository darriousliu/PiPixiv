package com.mrl.pixiv.common.data

object Constants {
    const val CLIENT_ID = "MOBrBDS8blbauoSck0ZfDbtuzpyT"
    const val CLIENT_SECRET = "lsACyCD94FhDUtGTXi3QzcFE2uU1hqtDaKeqrdwj"
    const val PIXIV_LOGIN_REDIRECT_URL =
        "https://app-api.pixiv.net/web/v1/users/auth/pixiv/callback"
    const val PIXIV_API_BASE_URL = "https://app-api.pixiv.net/"

    const val HashSalt =
        "28c1fdd170a5204386cb1313c7077b34f83e4aaf4aa829ce78c231e05b0bae2c"
    const val API_HOST = "app-api.pixiv.net"
    const val IMAGE_HOST = "i.pximg.net"
    const val STATIC_IMAGE_HOST = "s.pximg.net"
    const val AUTH_HOST = "oauth.secure.pixiv.net"
    val hostMap: Map<String, String> = mapOf(
        API_HOST to "210.140.139.155",
        AUTH_HOST to "210.140.139.155",
        IMAGE_HOST to "210.140.92.144",
        STATIC_IMAGE_HOST to "210.140.92.143",
        "doh" to "doh.dns.sb",
    )
    const val GITHUB_URL = "https://github.com/darriousliu/PiPixiv"
    const val GITHUB_ISSUE_URL = "$GITHUB_URL/issues"
    const val GITHUB_UPDATE_API = "https://api.github.com/repos/darriousliu/PiPixiv/releases/latest"
}