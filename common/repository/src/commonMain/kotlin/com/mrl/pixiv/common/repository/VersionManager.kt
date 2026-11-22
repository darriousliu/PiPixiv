package com.mrl.pixiv.common.repository

import co.touchlab.kermit.Logger
import com.mrl.pixiv.common.data.Constants
import com.mrl.pixiv.common.network.RoutedHttpClient
import com.mrl.pixiv.common.network.configureNetworkProxy
import com.mrl.pixiv.common.network.httpEngineFactory
import com.mrl.pixiv.common.serialize.JSON
import com.mrl.pixiv.common.util.AppUtil
import com.mrl.pixiv.common.util.Platform
import com.mrl.pixiv.common.util.RStrings
import com.mrl.pixiv.common.util.ToastUtil
import com.mrl.pixiv.common.util.platform
import com.mrl.pixiv.strings.already_updated
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.cacheDir
import io.github.vinceglb.filekit.delete
import io.github.vinceglb.filekit.sink
import io.github.vinceglb.filekit.write
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.timeout
import io.ktor.client.request.get
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLProtocol
import io.ktor.http.Url
import io.ktor.http.contentLength
import io.ktor.serialization.kotlinx.json.json
import io.ktor.utils.io.readTo
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.io.RawSink
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.uuid.Uuid

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
    private val clients = RoutedHttpClient(
        readSetting = { requireUserPreferenceValue.bypassSetting },
        create = { setting ->
            HttpClient(httpEngineFactory) {
                configureNetworkProxy(setting)
                install(HttpTimeout) {
                    connectTimeoutMillis = 30_000
                    socketTimeoutMillis = 30_000
                }
                install(ContentNegotiation) { json(JSON) }
            }
        },
        close = HttpClient::close,
    )

    private val _hasNewVersion = MutableStateFlow(false)
    val hasNewVersion: StateFlow<Boolean> = _hasNewVersion

    private val _latestVersionInfo = MutableStateFlow<GitHubRelease?>(null)
    val latestVersionInfo: StateFlow<GitHubRelease?> = _latestVersionInfo

    fun checkUpdate(showToast: Boolean = false) {
        scope.launch {
            try {
                val release = clients.withClient { client ->
                    client.get(Constants.GITHUB_UPDATE_API) {
                        timeout { requestTimeoutMillis = 30_000 }
                    }.body<GitHubRelease>()
                }
                val latestVersion = release.tagName.removePrefix("v")
                val currentVersion = AppUtil.versionName

                if (compareVersions(latestVersion, currentVersion) > 0) {
                    _hasNewVersion.value = true
                    _latestVersionInfo.value = release
                } else {
                    _hasNewVersion.value = false
                    _latestVersionInfo.value = null
                    if (showToast) {
                        ToastUtil.safeShortToast(RStrings.already_updated)
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (e: Exception) {
                Logger.e(throwable = e, tag = "VersionManager") { "Failed to check update" }
            }
        }
    }

    suspend fun saveUpdateAsset(
        asset: GithubAsset,
        selectDestination: suspend (suggestedName: String) -> PlatformFile?,
    ): Boolean {
        val name = asset.name
        require(name.isNotBlank() && name != "." && name != ".." && '/' !in name && '\\' !in name)
        val temporaryFile = PlatformFile(FileKit.cacheDir, "update-${Uuid.random()}.part")
        try {
            withContext(Dispatchers.IO) {
                clients.withClient { client ->
                    streamUpdateAsset(client, asset.downloadUrl) { temporaryFile.sink() }
                }
            }
            val destination = selectDestination(name) ?: return false
            destination write temporaryFile
            return true
        } finally {
            withContext(NonCancellable) {
                runCatching { temporaryFile.delete(mustExist = false) }.onFailure {
                    Logger.w(tag = "VersionManager") { "Failed to remove temporary update file" }
                }
            }
        }
    }

    fun GitHubRelease.getCurrentFlavorAsset(): GithubAsset? {
        return when (val platform = platform) {
            is Platform.Android -> assets.find {
                it.name.contains(AppUtil.flavor) && it.name.contains(".apk")
            }

            is Platform.Apple -> assets.find { it.name.contains(".ipa") }
            is Platform.Desktop -> when (platform) {
                Platform.Desktop.Windows -> assets.find { it.name.contains(".msi") }
                Platform.Desktop.MacOS -> assets.find { it.name.contains(".dmg") }
                Platform.Desktop.Linux -> assets.find { it.name.contains(".tar.gz") }
            }
        }
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

internal suspend fun streamUpdateAsset(
    client: HttpClient,
    downloadUrl: String,
    openSink: () -> RawSink,
) {
    val url = Url(downloadUrl)
    require(url.protocol == URLProtocol.HTTPS && url.host.isNotBlank() && url.user == null && url.password == null)
    client.prepareGet(url).execute { response ->
        check(response.status == HttpStatusCode.OK) { "Update download returned HTTP ${response.status.value}" }
        val expectedLength = response.contentLength()
        val receivedLength = openSink().use { sink -> response.bodyAsChannel().readTo(sink) }
        check(receivedLength > 0 && (expectedLength == null || receivedLength == expectedLength)) {
            "Incomplete update download"
        }
    }
}
