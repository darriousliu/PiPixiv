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
    private const val PIXIV_ORIGIN_FALLBACK =
        "210.140.139.152,210.140.139.155,210.140.139.158,210.140.139.161"
    private const val PIXIV_IMAGE_FALLBACK =
        "210.140.139.129,210.140.139.130,210.140.139.131,210.140.139.132," +
                "210.140.139.133,210.140.139.134,210.140.139.135,210.140.139.136," +
                "210.140.139.137,210.140.139.138"
    val hostMap: Map<String, String> = mapOf(
        API_HOST to PIXIV_ORIGIN_FALLBACK,
        AUTH_HOST to PIXIV_ORIGIN_FALLBACK,
        IMAGE_HOST to PIXIV_IMAGE_FALLBACK,
        STATIC_IMAGE_HOST to PIXIV_IMAGE_FALLBACK,
        "doh" to "doh.dns.sb",
    )
    const val GITHUB_URL = "https://github.com/darriousliu/PiPixiv"
    const val GITHUB_ISSUE_URL = "$GITHUB_URL/issues"
    const val GITHUB_RELEASE_URL = "$GITHUB_URL/releases"
    const val GITHUB_UPDATE_API = "https://pipixiv-update.kiritowdnmd.workers.dev/pipixiv/latest"
}
