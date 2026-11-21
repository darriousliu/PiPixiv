package com.mrl.pixiv.common.mmkv

import kotlinx.coroutines.ExperimentalForInheritanceCoroutinesApi
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

interface AccountScoped {
    fun reload()
}

class AccountMMKVProperty<V>(
    private val field: MMKVField<V>,
    private val key: String,
    private val defaultValue: V,
    private val loadOnAccess: Boolean = false,
) {
    private sealed interface Binding<out V> {
        val userId: Long
    }

    private data class Loaded<V>(override val userId: Long, val value: V) : Binding<V>
    private class Unloaded(override val userId: Long) : Binding<Nothing>
    private object NotEmitted

    private val state = MutableStateFlow<Binding<V>>(Loaded(0L, defaultValue))
    val flow: StateFlow<V> = PropertyFlow()

    @OptIn(ExperimentalForInheritanceCoroutinesApi::class)
    private inner class PropertyFlow : StateFlow<V> {
        override val value: V
            get() {
                val binding = state.value
                return if (binding is Loaded) binding.value else withAccountStorageLock { read(state.value.userId) }
            }

        override val replayCache: List<V>
            get() = listOf(value)

        override suspend fun collect(collector: FlowCollector<V>): Nothing = state.collect(ValueCollector(collector))
    }

    private inner class ValueCollector(private val collector: FlowCollector<V>) : FlowCollector<Binding<V>> {
        private var previous: Any? = NotEmitted

        override suspend fun emit(value: Binding<V>) {
            val current = flow.value
            if (previous != current) {
                previous = current
                collector.emit(current)
            }
        }
    }

    fun read(userId: Long): V = withAccountStorageLock {
        val binding = state.value
        when {
            userId <= 0 -> defaultValue
            userId != binding.userId -> field.read(accountMMKVOwner(userId), key)
            binding is Loaded -> binding.value
            else -> field.read(accountMMKVOwner(userId), key).also { value ->
                if (state.value === binding) state.value = Loaded(userId, value)
            }
        }
    }

    fun write(userId: Long, value: V) = update(userId) { value }

    fun update(userId: Long, transform: (V) -> V) = withAccountStorageLock {
        if (userId <= 0) return@withAccountStorageLock
        val previous = read(userId)
        val next = transform(previous)
        if (next != previous) {
            field.write(accountMMKVOwner(userId), key, next)
            if (userId == state.value.userId) state.value = Loaded(userId, next)
        }
    }

    fun reload(userId: Long) = withAccountStorageLock {
        state.value = when {
            userId <= 0 -> Loaded(0L, defaultValue)
            loadOnAccess -> Unloaded(userId)
            else -> Loaded(userId, field.read(accountMMKVOwner(userId), key))
        }
    }

    fun prefetch(userId: Long) {
        val (binding, owner) = withAccountStorageLock {
            val current = state.value
            if (current !is Unloaded || current.userId != userId) return@withAccountStorageLock null
            current to accountMMKVOwner(userId)
        } ?: return

        // Native reads still take the account lock through the owner. JSON decoding can finish
        // after a switch or a foreground write has replaced this binding.
        val value = field.read(owner, key)
        withAccountStorageLock {
            if (state.value === binding) state.value = Loaded(userId, value)
        }
    }
}
