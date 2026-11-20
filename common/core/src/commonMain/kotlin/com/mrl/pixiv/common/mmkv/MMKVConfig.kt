package com.mrl.pixiv.common.mmkv

import com.ctrip.flight.mmkv.MMKV_KMP
import com.ctrip.flight.mmkv.mmkvWithID
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.absolutePath
import io.github.vinceglb.filekit.filesDir
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.serialization.serializer

interface MMKVOwner {
    val id: String
    val kv: MMKV_KMP
        get() = withAccountStorageLock {
            defaultInstances.getOrPut(id) { mmkvWithID(id) }
        }

    fun contains(key: String): Boolean = kv.containsKey(key)
    fun sync() = kv.sync()
}

/**
 * 存储设备级数据。
 */
interface MMKVApp : MMKVOwner {
    override val id get() = "pixiv.app"
}

interface MMKVUser : MMKVOwner {
    override val id get() = "pixiv.user"
    val userId: Long get() = AccountStore.activeUserId
    override val kv: MMKV_KMP get() = accountMMKVOwner(userId).kv
}

class AccountMMKVOwner(
    override val userId: Long,
    private val filesDirectory: String,
) : MMKVUser {
    override val kv: MMKV_KMP by lazy {
        require(userId > 0)
        val directory = Path(filesDirectory, "accounts", userId.toString(), "mmkv")
        SystemFileSystem.createDirectories(directory)
        mmkvWithID(id, rootPath = directory.toString())
    }
}

private val defaultInstances = mutableMapOf<String, MMKV_KMP>()
private val accountInstances = mutableMapOf<Long, AccountMMKVOwner>()

fun accountMMKVOwner(userId: Long): MMKVOwner = withAccountStorageLock {
    require(userId > 0)
    accountInstances.getOrPut(userId) {
        AccountMMKVOwner(userId, FileKit.filesDir.absolutePath())
    }
}

fun mmkvOwner(id: String, rootPath: String? = null): MMKVOwner = object : MMKVOwner {
    override val id = id
    override val kv by lazy {
        if (rootPath == null) super<MMKVOwner>.kv else mmkvWithID(id, rootPath = rootPath)
    }
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

