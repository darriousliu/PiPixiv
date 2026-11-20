package com.mrl.pixiv.common.mmkv

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty
import kotlin.text.CharacterCodingException

val mmkvJson = Json {
    encodeDefaults = true
    ignoreUnknownKeys = true
    isLenient = true
}

class MMKVSerializableProperty<V>(
    private val serializer: KSerializer<V>,
    private val defaultValue: V
) : ReadWriteProperty<MMKVOwner, V>, MMKVField<V> {
    override fun getValue(thisRef: MMKVOwner, property: KProperty<*>): V =
        read(thisRef, property.name)

    override fun read(owner: MMKVOwner, key: String): V {
        // 通过字节接口读取，让损坏的数据在 Kotlin 层完成校验。
        return try {
            readStrict(owner, key)
        } catch (_: CharacterCodingException) {
            defaultValue
        } catch (_: SerializationException) {
            defaultValue
        }
    }

    fun readStrict(owner: MMKVOwner, key: String): V {
        val bytes = owner.kv.getByteArray(key) ?: return defaultValue
        return mmkvJson.decodeFromString(serializer, bytes.decodeToString(throwOnInvalidSequence = true))
    }

    override fun setValue(thisRef: MMKVOwner, property: KProperty<*>, value: V) {
        persist(thisRef, property.name, value)
    }

    override fun write(owner: MMKVOwner, key: String, value: V) {
        check(persist(owner, key, value)) { "Could not persist MMKV field $key" }
    }

    private fun persist(owner: MMKVOwner, key: String, value: V): Boolean =
        owner.kv.set(key, value?.let { mmkvJson.encodeToString(serializer, it).encodeToByteArray() })
}
