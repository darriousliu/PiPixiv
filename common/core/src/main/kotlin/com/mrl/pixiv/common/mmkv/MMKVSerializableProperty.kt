package com.mrl.pixiv.common.mmkv

import kotlinx.serialization.KSerializer
import kotlinx.serialization.protobuf.ProtoBuf
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

private val mmkvProtobuf = ProtoBuf {
    encodeDefaults = false
}

class MMKVSerializableProperty<V>(
    private val serializer: KSerializer<V>,
    private val defaultValue: V
) : ReadWriteProperty<MMKVOwner, V> {
    override fun getValue(thisRef: MMKVOwner, property: KProperty<*>): V =
        thisRef.kv.getByteArray(property.name)?.let {
            mmkvProtobuf.decodeFromByteArray(serializer, it)
        } ?: defaultValue

    override fun setValue(thisRef: MMKVOwner, property: KProperty<*>, value: V) {
        if (value != null) {
            thisRef.kv[property.name] = mmkvProtobuf.encodeToByteArray(serializer, value)
        } else {
            thisRef.kv.removeValueForKey(property.name)
        }
    }
}