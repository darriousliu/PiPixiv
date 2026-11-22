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

    override fun read(owner: MMKVOwner, key: String): V = readRecoverable(owner, key) { defaultValue }

    fun readRecoverable(owner: MMKVOwner, key: String, onCorrupted: () -> V): V = try {
        readStrict(owner, key)
    } catch (_: CharacterCodingException) {
        onCorrupted()
    } catch (_: SerializationException) {
        onCorrupted()
    }

    fun readStrict(owner: MMKVOwner, key: String): V {
        val bytes = owner.accessMMKV { getByteArray(key) }
        if (bytes == null) {
            if (owner.contains(key)) throw SerializationException("Could not read field $key")
            return defaultValue
        }
        return decode(bytes)
    }

    internal fun decode(bytes: ByteArray): V =
        mmkvJson.decodeFromString(serializer, bytes.decodeToString(throwOnInvalidSequence = true))

    override fun setValue(thisRef: MMKVOwner, property: KProperty<*>, value: V) {
        persist(thisRef, property.name, value)
    }

    override fun write(owner: MMKVOwner, key: String, value: V) {
        if (!persist(owner, key, value)) {
            throw MMKVStorageException("Could not persist MMKV field $key")
        }
    }

    private fun persist(owner: MMKVOwner, key: String, value: V): Boolean {
        val bytes = value?.let { mmkvJson.encodeToString(serializer, it).encodeToByteArray() }
        return owner.accessMMKV { set(key, bytes) }
    }
}
