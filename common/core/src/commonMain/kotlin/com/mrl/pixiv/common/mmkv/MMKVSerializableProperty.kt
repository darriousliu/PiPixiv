package com.mrl.pixiv.common.mmkv

import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.protobuf.ProtoBuf
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

private val mmkvProtobuf = ProtoBuf {
    encodeDefaults = false
}

private val mmkvJson = Json {
    encodeDefaults = true
    ignoreUnknownKeys = true
    isLenient = true
}

class MMKVSerializableProperty<V>(
    private val serializer: KSerializer<V>,
    private val defaultValue: V
) : ReadWriteProperty<MMKVOwner, V> {
    override fun getValue(thisRef: MMKVOwner, property: KProperty<*>): V {
        // 使用一个辅助 Key 来标记当前存储的数据类型是否为 JSON
        // 这样可以避免在 ByteArray 类型上调用 getString 导致的 Native 崩溃
        val flagKey = "${property.name}_is_json"
        val isJson = thisRef.kv.getBoolean(flagKey, false)

        if (isJson) {
            val jsonString = thisRef.kv.getString(property.name, "")
            if (jsonString.isNotEmpty()) {
                return mmkvJson.decodeFromString(serializer, jsonString)
            }
        } else {
            // 如果标记为 false，说明可能是旧的 Protobuf 数据，或者是新数据但还未写入过
            thisRef.kv.getByteArray(property.name)?.let {
                val value = mmkvProtobuf.decodeFromByteArray(serializer, it)
                // 迁移：读取到旧数据后，以 JSON 格式写回，并更新标记
                setValue(thisRef, property, value)
                return value
            }
        }
        return defaultValue
    }

    override fun setValue(thisRef: MMKVOwner, property: KProperty<*>, value: V) {
        val flagKey = "${property.name}_is_json"
        if (value != null) {
            thisRef.kv[property.name] = mmkvJson.encodeToString(serializer, value)
            thisRef.kv[flagKey] = true
        } else {
            thisRef.kv.removeValueForKey(property.name)
            thisRef.kv.removeValueForKey(flagKey)
        }
    }
}