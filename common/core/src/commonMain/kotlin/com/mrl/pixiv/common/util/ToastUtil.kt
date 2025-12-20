package com.mrl.pixiv.common.util

import com.dokar.sonner.Toast
import com.dokar.sonner.ToastType
import com.dokar.sonner.ToasterDefaults
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import kotlin.time.Duration

object ToastUtil : CoroutineScope by MainScope() {
    private val _toastFlow = Channel<Toast>()
    val toastFlow: Flow<Toast> = _toastFlow.receiveAsFlow()

    fun safeShortToast(strId: Int, vararg params: Any) {
        val text = ""
        launch {
            _toastFlow.send(
                Toast(
                    message = text,
                    duration = ToasterDefaults.DurationShort,
                )
            )
        }
    }

    fun safeShortToast(strId: StringResource, vararg params: Any) {
        val text = AppUtil.getString(strId, *params)
        launch {
            _toastFlow.send(
                Toast(
                    message = text,
                    duration = ToasterDefaults.DurationShort,
                )
            )
        }
    }

    fun safeShortToast(
        message: Any,
        icon: Any? = null,
        action: Any? = null,
        type: ToastType = ToastType.Normal,
        duration: Duration = ToasterDefaults.DurationShort,
    ) {
        launch {
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
}