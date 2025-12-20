package com.mrl.pixiv.common.toast

import androidx.compose.runtime.Composable

sealed class ToastMessage {
    data class Resource(
        val resId: Int,
        val args: Array<Any> = arrayOf()
    ) : ToastMessage() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || this::class != other::class) return false

            other as Resource

            if (resId != other.resId) return false
            if (!args.contentEquals(other.args)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = resId
            result = 31 * result + args.contentHashCode()
            return result
        }
    }

    data class Compose(val content: @Composable () -> Unit) : ToastMessage()
}