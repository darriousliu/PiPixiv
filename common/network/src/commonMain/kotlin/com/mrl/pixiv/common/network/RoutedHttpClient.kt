package com.mrl.pixiv.common.network

import com.mrl.pixiv.common.data.setting.UserPreference
import com.mrl.pixiv.common.data.setting.UserPreference.BypassSetting
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

data class NetworkSettings(
    val bypassSetting: BypassSetting,
    val imageHost: String,
    val appApiProxyHost: String = "",
)

fun UserPreference.networkSettings(): NetworkSettings =
    NetworkSettings(bypassSetting, imageHost, appApiProxyHost)

class RoutedHttpClient<T : Any>(
    private val readSetting: () -> BypassSetting,
    private val create: (BypassSetting) -> T,
    private val close: (T) -> Unit,
    private val readPreference: (() -> NetworkSettings)? = null,
) {
    private class Entry<T>(val setting: BypassSetting, val value: T, var users: Int = 0)

    private val mutex = Mutex()
    private val entries = mutableMapOf<BypassSetting, Entry<T>>()
    private var active: Entry<T>? = null

    suspend fun <R> withClient(block: suspend (T) -> R): R {
        if (readPreference != null) {
            return withNetworkPreference(readPreference) { preference ->
                useClient(preference.bypassSetting, block)
            }
        }
        return useClient(currentNetworkPreference()?.bypassSetting, block)
    }

    private suspend fun <R> useClient(setting: BypassSetting?, block: suspend (T) -> R): R {
        var retired: T? = null
        val entry = mutex.withLock {
            val latestSetting = readSetting()
            val requestedSetting = setting ?: latestSetting
            val previous = active
            val selected = entries.getOrPut(requestedSetting) {
                Entry(requestedSetting, create(requestedSetting))
            }
            // A nested call can use an older snapshot without publishing it as the current client.
            if (requestedSetting == latestSetting && selected !== previous) {
                active = selected
                if (previous?.users == 0) {
                    entries.remove(previous.setting)
                    retired = previous.value
                }
            }
            selected.users++
            selected
        }

        try {
            retired?.let(close)
            return block(entry.value)
        } finally {
            withContext(NonCancellable) {
                val unused = mutex.withLock {
                    entry.users--
                    if (entry !== active && entry.users == 0) {
                        entries.remove(entry.setting)
                        true
                    } else {
                        false
                    }
                }
                if (unused) close(entry.value)
            }
        }
    }
}

private class NetworkPreferenceSnapshot(val preference: NetworkSettings) :
    AbstractCoroutineContextElement(Key) {
    companion object Key : CoroutineContext.Key<NetworkPreferenceSnapshot>
}

internal suspend fun currentNetworkPreference(): NetworkSettings? =
    currentCoroutineContext()[NetworkPreferenceSnapshot]?.preference

suspend fun <R> withNetworkPreference(
    readPreference: () -> NetworkSettings,
    block: suspend (NetworkSettings) -> R,
): R {
    currentNetworkPreference()?.let { return block(it) }
    val preference = readPreference()
    return withContext(NetworkPreferenceSnapshot(preference)) { block(preference) }
}
