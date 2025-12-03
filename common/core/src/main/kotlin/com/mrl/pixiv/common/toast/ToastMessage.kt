package com.mrl.pixiv.common.toast

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable

sealed class ToastMessage {
    data class Resource(
        @StringRes
        val resId: Int,
        val args: Array<Any> = arrayOf()
    ) : ToastMessage() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

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