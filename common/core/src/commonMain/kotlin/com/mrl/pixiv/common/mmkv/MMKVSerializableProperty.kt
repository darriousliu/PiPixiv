package com.mrl.pixiv.common.mmkv

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty
import kotlin.text.CharacterCodingException

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
        // 通过字节接口读取，让损坏的数据在 Kotlin 层完成校验。
        val bytes = thisRef.kv.getByteArray(property.name) ?: return defaultValue
        return try {
            mmkvJson.decodeFromString(serializer, bytes.decodeToString(throwOnInvalidSequence = true))
        } catch (_: CharacterCodingException) {
            defaultValue
        } catch (_: SerializationException) {
            defaultValue
        }
    }

    override fun setValue(thisRef: MMKVOwner, property: KProperty<*>, value: V) {
        if (value != null) {
            thisRef.kv[property.name] = mmkvJson.encodeToString(serializer, value).encodeToByteArray()
        } else {
            thisRef.kv.removeValueForKey(property.name)
        }
    }
}
