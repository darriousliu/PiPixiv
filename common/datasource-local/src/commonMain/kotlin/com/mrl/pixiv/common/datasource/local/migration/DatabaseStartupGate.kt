package com.mrl.pixiv.common.datasource.local.migration

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first

object DatabaseStartupGate {
    private val ready = MutableStateFlow(false)
    private val prepare = MutableStateFlow<(suspend () -> Boolean)?>(null)

    fun install(prepareInBackground: suspend () -> Boolean) {
        prepare.value = prepareInBackground
    }

    suspend fun prepareInBackground(): Boolean =
        ready.value || prepare.filterNotNull().first().invoke()

    suspend fun awaitReady() {
        ready.first { it }
    }

    fun markReady() {
        ready.value = true
    }
}
