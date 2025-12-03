package com.mrl.pixiv.common.util

import androidx.annotation.StringRes
import com.dokar.sonner.Toast
import com.dokar.sonner.ToastType
import com.dokar.sonner.ToasterDefaults
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlin.time.Duration

object ToastUtil : CoroutineScope by MainScope() {
    private val _toastFlow = Channel<Toast>()
    val toastFlow: Flow<Toast> = _toastFlow.receiveAsFlow()
    fun safeShortToast(@StringRes strId: Int, vararg params: Any) {
        val text = AppUtil.appContext.getString(strId, *params)
        _toastFlow.trySend(
            Toast(
                message = text,
                duration = ToasterDefaults.DurationShort,
            )
        )
    }

    fun safeShortToast(
        message: Any,
        icon: Any? = null,
        action: Any? = null,
        type: ToastType = ToastType.Normal,
        duration: Duration = ToasterDefaults.DurationDefault,
    ) {
        _toastFlow.trySend(
            Toast(
                message = message,
                icon = icon,
                action = action,
                type = type,
                duration = duration,
            )
        )
    }
}