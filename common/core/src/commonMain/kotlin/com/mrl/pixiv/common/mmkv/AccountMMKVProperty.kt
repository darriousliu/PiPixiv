package com.mrl.pixiv.common.mmkv

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

interface AccountScoped {
    fun reload()
}

class AccountMMKVProperty<V>(
    private val field: MMKVField<V>,
    private val key: String,
    private val defaultValue: V,
) {
    private var loadedUserId = 0L
    private val state = MutableStateFlow(defaultValue)
    val flow = state.asStateFlow()

    fun read(userId: Long): V = withAccountStorageLock {
        when {
            userId <= 0 -> defaultValue
            userId == loadedUserId -> state.value
            else -> field.read(accountMMKVOwner(userId), key)
        }
    }

    fun write(userId: Long, value: V) = update(userId) { value }

    fun update(userId: Long, transform: (V) -> V) = withAccountStorageLock {
        if (userId <= 0) return@withAccountStorageLock
        val previous = read(userId)
        val next = transform(previous)
        if (next != previous) {
            field.write(accountMMKVOwner(userId), key, next)
            if (userId == loadedUserId) state.value = next
        }
    }

    fun reload(userId: Long) = withAccountStorageLock {
        val loaded = if (userId > 0) field.read(accountMMKVOwner(userId), key) else defaultValue
        loadedUserId = userId
        state.value = loaded
    }
}
