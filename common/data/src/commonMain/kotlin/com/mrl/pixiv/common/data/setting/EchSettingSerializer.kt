package com.mrl.pixiv.common.data.setting

import com.mrl.pixiv.common.data.setting.UserPreference.BypassSetting.ECH
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonIgnoreUnknownKeys
import kotlinx.serialization.protobuf.ProtoNumber

internal object EchSettingSerializer : KSerializer<ECH> {
    override val descriptor: SerialDescriptor = StoredEchSetting.serializer().descriptor

    override fun deserialize(decoder: Decoder): ECH {
        val stored = decoder.decodeSerializableValue(StoredEchSetting.serializer())
        return ECH(normalizeDohUrl(stored.url))
    }

    override fun serialize(encoder: Encoder, value: ECH) {
        encoder.encodeSerializableValue(
            StoredEchSetting.serializer(),
            StoredEchSetting(normalizeDohUrl(value.url)),
        )
    }
}

// JSON 丢弃旧 IP 池、非严格 SSL 和超时字段；ProtoBuf 按原字段号读取 URL，跳过其他字段。
@OptIn(ExperimentalSerializationApi::class)
@Serializable
@SerialName("sni")
@JsonIgnoreUnknownKeys
private data class StoredEchSetting(
    @ProtoNumber(1) val url: String = ECH.DEFAULT_DOH_URL,
)

private fun normalizeDohUrl(value: String): String {
    val url = value.trim()
    return if (
        !url.startsWith("https://", ignoreCase = true) ||
        url.removeSuffix("/").equals("https://doh.pub/dns-query", ignoreCase = true)
    ) {
        ECH.DEFAULT_DOH_URL
    } else {
        url
    }
}
