package com.mrl.pixiv.login

import android.net.Uri
import okio.ByteString.Companion.toByteString
import kotlin.io.encoding.Base64
import kotlin.random.Random

private var codeVerifier = getCodeVer()

private fun getCodeVer(): String {
    val randomKeySet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-._~"
    val result = (0..128).map {
        randomKeySet[Random.nextInt(randomKeySet.length)]
    }.joinToString("")
    codeVerifier = result
    return codeVerifier
}

private fun getCodeChallenge(): String {
    return Base64.UrlSafe.encode(
        getCodeVer().toByteArray(Charsets.UTF_8).toByteString().sha256().toByteArray(),
    ).replace("=", "")
}

fun generateWebViewUrl(create: Boolean) =
    if (create) {
        "https://app-api.pixiv.net/web/v1/provisional-accounts/create?code_challenge=${getCodeChallenge()}&code_challenge_method=S256&client=pixiv-android"
    } else {
        "https://app-api.pixiv.net/web/v1/login?code_challenge=${getCodeChallenge()}&code_challenge_method=S256&client=pixiv-android"
    }

fun checkUri(uri: Uri): Pair<String, String>? {
    if (uri.scheme == "pixiv" && uri.host == "account") {
        val code = uri.getQueryParameter("code")
        return if (code != null) code to codeVerifier else null
    }
    return null
}