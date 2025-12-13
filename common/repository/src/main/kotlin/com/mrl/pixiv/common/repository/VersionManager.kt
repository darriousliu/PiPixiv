package com.mrl.pixiv.common.repository

import co.touchlab.kermit.Logger
import com.mrl.pixiv.common.data.Constants
import com.mrl.pixiv.common.serialize.JSON
import com.mrl.pixiv.common.util.AppUtil
import com.mrl.pixiv.common.util.RString
import com.mrl.pixiv.common.util.ToastUtil
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GitHubRelease(
    @SerialName("tag_name")
    val tagName: String,
    @SerialName("html_url")
    val htmlUrl: String,
    val body: String?,
    val assets: List<GithubAsset> = emptyList(),
)

@Serializable
data class GithubAsset(
    val name: String,
    @SerialName("updated_at")
    val updateAt: String,
    @SerialName("browser_download_url")
    val downloadUrl: String,
)

object VersionManager {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val client = HttpClient {
        install(ContentNegotiation) {
            json(JSON)
        }
    }

    private val _hasNewVersion = MutableStateFlow(false)
    val hasNewVersion: StateFlow<Boolean> = _hasNewVersion

    private val _latestVersionInfo = MutableStateFlow<GitHubRelease?>(null)
    val latestVersionInfo: StateFlow<GitHubRelease?> = _latestVersionInfo

    fun checkUpdate(showToast: Boolean = false) {
        scope.launch {
            try {
                val release = client.get(Constants.GITHUB_UPDATE_API).body<GitHubRelease>()
                val latestVersion = release.tagName.removePrefix("v")
                val currentVersion = AppUtil.versionName

                if (compareVersions(latestVersion, currentVersion) > 0) {
                    _hasNewVersion.value = true
                    _latestVersionInfo.value = release
                } else {
                    _hasNewVersion.value = false
                    _latestVersionInfo.value = null
                    if (showToast) {
                        ToastUtil.safeShortToast(RString.already_updated)
                    }
                }
            } catch (e: Exception) {
                Logger.e("VersionManager", e) { "Failed to check update" }
            }
        }
    }

    fun GitHubRelease.getCurrentFlavorAsset(): GithubAsset? {
        return assets.find { it.name.contains(AppUtil.flavor) }
    }

    private fun compareVersions(v1: String, v2: String): Int {
        val parts1 = v1.split(".").map { it.toIntOrNull() ?: 0 }
        val parts2 = v2.split(".").map { it.toIntOrNull() ?: 0 }
        val length = maxOf(parts1.size, parts2.size)

        for (i in 0 until length) {
            val part1 = parts1.getOrElse(i) { 0 }
            val part2 = parts2.getOrElse(i) { 0 }
            if (part1 != part2) {
                return part1 - part2
            }
        }
        return 0
    }
}
