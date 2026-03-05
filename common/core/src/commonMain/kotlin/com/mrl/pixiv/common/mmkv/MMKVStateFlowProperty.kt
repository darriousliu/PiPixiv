@file:OptIn(ExperimentalForInheritanceCoroutinesApi::class)

package com.mrl.pixiv.common.mmkv

import kotlinx.coroutines.ExperimentalForInheritanceCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

class MMKVStateFlowProperty<V>(
    private val mmkvProperty: MMKVProperty<V>
) : ReadOnlyProperty<MMKVOwner, MutableStateFlow<V>> {
    private var cache: MutableStateFlow<V>? = null

    override fun getValue(thisRef: MMKVOwner, property: KProperty<*>): MutableStateFlow<V> =
        cache ?: MMKVFlow(
            { mmkvProperty.getValue(thisRef, property) },
            { mmkvProperty.setValue(thisRef, property, it) }
        ).also { cache = it }
}

class MMKVStateFlowSerializableProperty<V>(
    private val mmkvSerializableProperty: MMKVSerializableProperty<V>
) : ReadOnlyProperty<MMKVOwner, MutableStateFlow<V>> {
    private var cache: MutableStateFlow<V>? = null

    override fun getValue(thisRef: MMKVOwner, property: KProperty<*>): MutableStateFlow<V> =
        cache ?: MMKVFlow(
            { mmkvSerializableProperty.getValue(thisRef, property) },
            { mmkvSerializableProperty.setValue(thisRef, property, it) }
        ).also { cache = it }
}

class MMKVFlow<V>(
    private val getValue: () -> V,
    private val setValue: (V) -> Unit,
    private val flow: MutableStateFlow<V> = MutableStateFlow(getValue())
) : MutableStateFlow<V> by flow {
    override var value: V
        get() = getValue()
        set(value) {
            val origin = flow.value
            flow.value = value
            if (origin != value) {
                setValue(value)
            }
        }

    override fun compareAndSet(expect: V, update: V): Boolean =
        flow.compareAndSet(expect, update).also { setSuccess ->
            if (setSuccess) setValue(update)
        }
}