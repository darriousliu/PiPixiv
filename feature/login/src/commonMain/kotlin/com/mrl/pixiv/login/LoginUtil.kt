package com.mrl.pixiv.login

import io.ktor.http.Url
import okio.ByteString.Companion.toByteString
import kotlin.io.encoding.Base64
import kotlin.random.Random

private var codeVerifier = generateCodeVerifier()

private fun generateCodeVerifier(): String {
    val randomKeySet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-._~"
    return (0..128).map {
        randomKeySet[Random.nextInt(randomKeySet.length)]
    }.joinToString("")
}

private fun getCodeChallenge(): String {
    val verifier = generateCodeVerifier()
    codeVerifier = verifier
    return Base64.UrlSafe.withPadding(Base64.PaddingOption.ABSENT).encode(
        verifier.encodeToByteArray().toByteString().sha256().toByteArray(),
    )
}

fun generateWebViewUrl(create: Boolean) =
    if (create) {
        "https://app-api.pixiv.net/web/v1/provisional-accounts/create?code_challenge=${getCodeChallenge()}&code_challenge_method=S256&client=pixiv-android"
    } else {
        "https://app-api.pixiv.net/web/v1/login?code_challenge=${getCodeChallenge()}&code_challenge_method=S256&client=pixiv-android"
    }

fun checkUri(url: String): Pair<String, String>? {
    val url = try {
        Url(url)
    } catch (_: Exception) {
        return null
    }
    if (url.protocol.name == "pixiv" && url.host == "account") {
        val code = url.parameters["code"]
        return if (code != null) code to codeVerifier else null
    }
    return null
}
