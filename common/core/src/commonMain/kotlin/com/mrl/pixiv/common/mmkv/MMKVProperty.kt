package com.mrl.pixiv.common.mmkv

import com.ctrip.flight.mmkv.MMKV_KMP
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class MMKVProperty<V>(
    private val decode: MMKV_KMP.(String, V) -> V,
    private val encode: MMKV_KMP.(String, V) -> Boolean,
    private val defaultValue: V
) : ReadWriteProperty<MMKVOwner, V>, MMKVField<V> {
    override fun getValue(thisRef: MMKVOwner, property: KProperty<*>): V =
        read(thisRef, property.name)

    override fun setValue(thisRef: MMKVOwner, property: KProperty<*>, value: V) {
        thisRef.kv.encode(property.name, value)
    }

    override fun read(owner: MMKVOwner, key: String): V = owner.kv.decode(key, defaultValue)

    override fun write(owner: MMKVOwner, key: String, value: V) {
        check(owner.kv.encode(key, value)) { "Could not persist MMKV field $key" }
    }
}

interface MMKVField<V> {
    fun read(owner: MMKVOwner, key: String): V
    fun write(owner: MMKVOwner, key: String, value: V)
}
