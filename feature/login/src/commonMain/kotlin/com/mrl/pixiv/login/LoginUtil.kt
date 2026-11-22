package com.mrl.pixiv.login

import io.ktor.http.Url
import okio.ByteString.Companion.toByteString
import kotlin.io.encoding.Base64
import kotlin.random.Random

private lateinit var codeVerifier: String

private fun generateCodeVerifier(): String {
    val randomKeySet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-._~"
    val verifierLength = 128
    return buildString(verifierLength) {
        repeat(verifierLength) { append(randomKeySet[Random.nextInt(randomKeySet.length)]) }
    }
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
        val code = url.parameters["code"] ?: return null
        if (!::codeVerifier.isInitialized) return null
        return code to codeVerifier
    }
    return null
}
