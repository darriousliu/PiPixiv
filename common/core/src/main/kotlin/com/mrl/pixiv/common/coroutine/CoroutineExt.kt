package com.mrl.pixiv.common.coroutine

import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.mrl.pixiv.common.util.ToastUtil
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

private val ioScope by lazy { CoroutineScope(Dispatchers.IO) }
private val mainScope by lazy { CoroutineScope(Dispatchers.Main) }

/**
 * ProcessLifecycleScope
 */
val ProcessLifecycleScope: LifecycleCoroutineScope
    get() = ProcessLifecycleOwner.get().lifecycleScope


fun launchIO(block: suspend CoroutineScope.() -> Unit) {
    ioScope.launch(start = CoroutineStart.DEFAULT, block = block)
}

fun launchNetwork(
    onError: (Throwable) -> Unit = {},
    onComplete: () -> Unit = {},
    block: suspend CoroutineScope.() -> Unit,
) {
    try {
        launchIO(block)
    } catch (e: Throwable) {
        onError(e)
    } finally {
        onComplete()
    }
}

fun launchUI(block: suspend CoroutineScope.() -> Unit): Job =
    mainScope.launch(start = CoroutineStart.DEFAULT, block = block)

fun launchNow(block: suspend CoroutineScope.() -> Unit): Job =
    mainScope.launch(start = CoroutineStart.UNDISPATCHED, block = block)

fun CoroutineScope.launchUI(block: suspend CoroutineScope.() -> Unit): Job =
    launch(Dispatchers.Main, block = block)

fun CoroutineScope.launchIO(block: suspend CoroutineScope.() -> Unit): Job =
    launch(Dispatchers.IO, block = block)

/**
 * 全局进程
 */
fun launchProcess(
    context: CoroutineContext = EmptyCoroutineContext,
    start: CoroutineStart = CoroutineStart.DEFAULT,
    loading: MutableStateFlow<Boolean>? = null,
    errorToast: Boolean = false,
    error: (Throwable) -> Unit = {},
    block: suspend CoroutineScope.() -> Unit,
): Job {
    return ProcessLifecycleScope.launchCatch(context, start, loading, errorToast, error, block)
}

/**
 * CoroutineScope.launchCatch()
 */
fun CoroutineScope.launchCatch(
    context: CoroutineContext = EmptyCoroutineContext,
    start: CoroutineStart = CoroutineStart.DEFAULT,
    loading: MutableStateFlow<Boolean>? = null,
    errorToast: Boolean = false,
    error: (Throwable) -> Unit = { it.printStackTrace() },
    block: suspend CoroutineScope.() -> Unit,
): Job {
    val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        throwable.printStackTrace()
        error.invoke(throwable)
        loading?.update { false }

        if (errorToast && throwable !is CancelException) ToastUtil.safeShortToast(throwable.message.toString())
    }
    return launch(context + exceptionHandler, start) {
        loading?.update { true }

        // CancellationException 类型的异常不会走 CoroutineExceptionHandler，Kotlin 协程内部
        // 会视为正常，这里为了防止作用域下的异常丢失，手动捕获 CancellationException 并重新包装抛出。
        runCatching { block.invoke(this) }
            .onFailure { if (it is CancellationException) throw CancelException(it) else throw it }

        loading?.update { false }
    }
}

fun CoroutineScope.launchNonCancellable(block: suspend CoroutineScope.() -> Unit): Job =
    launchIO { withContext(NonCancellable, block) }

suspend inline fun <T> withUIContext(noinline block: suspend CoroutineScope.() -> T) = withContext(
    Dispatchers.Main,
    block,
)

suspend inline fun <T> withIOContext(noinline block: suspend CoroutineScope.() -> T) = withContext(
    Dispatchers.IO,
    block,
)

suspend inline fun <T> withNonCancellableContext(noinline block: suspend CoroutineScope.() -> T) =
    withContext(NonCancellable, block)
