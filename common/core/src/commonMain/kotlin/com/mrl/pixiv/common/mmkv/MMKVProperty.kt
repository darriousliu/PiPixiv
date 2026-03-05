package com.mrl.pixiv.common.mmkv

import com.ctrip.flight.mmkv.MMKV_KMP
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class MMKVProperty<V>(
    private val decode: MMKV_KMP.(String, V) -> V,
    private val encode: MMKV_KMP.(String, V) -> Boolean,
    private val defaultValue: V
) : ReadWriteProperty<MMKVOwner, V> {
    override fun getValue(thisRef: MMKVOwner, property: KProperty<*>): V =
        thisRef.kv.decode(property.name, defaultValue)

    override fun setValue(thisRef: MMKVOwner, property: KProperty<*>, value: V) {
        thisRef.kv.encode(property.name, value)
    }
}