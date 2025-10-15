package com.mrl.pixiv.common.mmkv

import com.ctrip.flight.mmkv.MMKV_KMP
import com.ctrip.flight.mmkv.mmkvWithID
import kotlinx.serialization.serializer

interface MMKVOwner {
    val id: String
    val kv: MMKV_KMP
}

/**
 * 存储用户无关对数据
 */
interface MMKVApp : MMKVOwner {
    override val id get() = "pixiv.app"
    override val kv get() = mmkvWithID(id)
}

/**
 * 当前用户级别的key-value存储实现该接口
 * 退出登录后会被清除
 */
interface MMKVUser : MMKVOwner {
    override val id get() = "pixiv.user"
    override val kv get() = mmkvWithID(id)
}

internal operator fun MMKV_KMP.set(key: String, value: ByteArray?) = if (value == null) {
    removeValueForKey(key)
    true
} else {
    set(key, value)
}

fun MMKVOwner.mmkvInt(default: Int = 0) =
    MMKVProperty(MMKV_KMP::getInt, MMKV_KMP::set, default)

fun MMKVOwner.mmkvLong(default: Long = 0L) =
    MMKVProperty(MMKV_KMP::getLong, MMKV_KMP::set, default)

fun MMKVOwner.mmkvBool(default: Boolean = false) =
    MMKVProperty(MMKV_KMP::getBoolean, MMKV_KMP::set, default)

fun MMKVOwner.mmkvFloat(default: Float = 0f) =
    MMKVProperty(MMKV_KMP::getFloat, MMKV_KMP::set, default)

fun MMKVOwner.mmkvDouble(default: Double = 0.0) =
    MMKVProperty(MMKV_KMP::getDouble, MMKV_KMP::set, default)

fun MMKVOwner.mmkvString(default: String = "") =
    MMKVProperty(MMKV_KMP::getString, MMKV_KMP::set, default)

fun MMKVOwner.mmkvStringSet(default: Set<String>? = null) =
    MMKVProperty(MMKV_KMP::getStringSet, MMKV_KMP::set, default)

fun MMKVOwner.mmkvBytes(default: ByteArray = byteArrayOf()) =
    MMKVProperty(MMKV_KMP::getByteArray, MMKV_KMP::set, default)

inline fun <reified V : Any?> MMKVOwner.mmkvSerializable(defaultValue: V) =
    MMKVSerializableProperty(serializer(), defaultValue)

fun <V> MMKVProperty<V>.asMutableStateFlow() = MMKVStateFlowProperty(this)

fun <V> MMKVSerializableProperty<V>.asMutableStateFlow() = MMKVStateFlowSerializableProperty(this)

fun MMKVOwner.clearAll() = kv.clearAll()

