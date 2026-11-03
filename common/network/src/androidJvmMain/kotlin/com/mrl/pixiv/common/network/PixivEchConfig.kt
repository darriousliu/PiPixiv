package com.mrl.pixiv.common.network

import com.mrl.pixiv.common.serialize.JSON
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.io.IOException
import java.util.Base64
import java.util.concurrent.TimeUnit

internal data class PixivEchConfig(
    val bytes: ByteArray,
    val ttlSeconds: Long,
)

internal class PixivEchConfigCache(
    private val lookup: () -> PixivEchConfig,
    private val nanoTime: () -> Long = System::nanoTime,
) {
    private var cached: PixivEchConfig? = null
    private var loadedAtNanos = 0L

    @Synchronized
    fun get(): ByteArray {
        val current = cached
        if (current != null) {
            val elapsed = nanoTime() - loadedAtNanos
            if (elapsed >= 0 && elapsed < TimeUnit.SECONDS.toNanos(current.ttlSeconds)) {
                return current.bytes.copyOf()
            }
        }

        // 过期配置不能在查询失败后继续使用，否则会反复触发 ECH 握手拒绝。
        cached = null
        val refreshed = lookup()
        cached = refreshed.copy(bytes = refreshed.bytes.copyOf())
        loadedAtNanos = nanoTime()
        return refreshed.bytes.copyOf()
    }

    @Synchronized
    fun invalidate() {
        cached = null
    }
}

internal fun decodePixivEchConfig(body: String): PixivEchConfig {
    val response = try {
        JSON.decodeFromString<EchDnsResponse>(body)
    } catch (exception: IllegalArgumentException) {
        throw IOException("Invalid ECH DNS response", exception)
    }
    if (response.status != 0) {
        throw IOException("ECH DNS query failed with status ${response.status}")
    }

    for (answer in response.answers) {
        if (answer.type != HTTPS_RECORD_TYPE) continue
        val match = ECH_PARAMETER.find(answer.data) ?: continue
        val encoded = match.groups[1]?.value ?: match.groupValues[2]
        val bytes = try {
            Base64.getDecoder().decode(encoded)
        } catch (exception: IllegalArgumentException) {
            throw IOException("Invalid ECH config encoding", exception)
        }
        if (!bytes.isSupportedEchConfigList() || answer.ttlSeconds < 0) {
            throw IOException("Invalid or unsupported ECH config list")
        }
        return PixivEchConfig(bytes, answer.ttlSeconds)
    }

    throw IOException("ECH DNS response contains no HTTPS ECH config")
}

private fun ByteArray.isSupportedEchConfigList(): Boolean {
    if (size < 6 || unsignedShort(0) != size - 2) return false
    var offset = 2
    var hasSupportedVersion = false
    while (offset < size) {
        if (size - offset < 4) return false
        val version = unsignedShort(offset)
        val length = unsignedShort(offset + 2)
        offset += 4
        if (length == 0 || length > size - offset) return false
        if (version == ECH_VERSION) hasSupportedVersion = true
        offset += length
    }
    return hasSupportedVersion
}

private fun ByteArray.unsignedShort(offset: Int): Int =
    ((this[offset].toInt() and 0xff) shl 8) or (this[offset + 1].toInt() and 0xff)

@Serializable
private data class EchDnsResponse(
    @SerialName("Status") val status: Int = -1,
    @SerialName("Answer") val answers: List<EchDnsAnswer> = emptyList(),
)

@Serializable
private data class EchDnsAnswer(
    val type: Int,
    val data: String,
    @SerialName("TTL") val ttlSeconds: Long = 0,
)

private const val HTTPS_RECORD_TYPE = 65
private const val ECH_VERSION = 0xfe0d
private val ECH_PARAMETER = Regex("""(?:^|\s)ech=(?:"([A-Za-z0-9+/=]+)"|([A-Za-z0-9+/=]+))(?=\s|$)""")
