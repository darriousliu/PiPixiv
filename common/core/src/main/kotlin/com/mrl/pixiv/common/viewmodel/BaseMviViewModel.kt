package com.mrl.pixiv.common.viewmodel

import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.mrl.pixiv.common.coroutine.CancelException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.compose.currentKoinScope
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.ParametersDefinition
import org.koin.core.qualifier.Qualifier
import org.koin.core.scope.Scope
import org.koin.viewmodel.defaultExtras
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.cancellation.CancellationException


// 定义副作用（包括错误）
interface SideEffect {
    data class Error(val throwable: Throwable) : SideEffect
    // 可扩展其他副作用如导航事件等
}

// 事件入口基类
interface ViewIntent

val <S, I : ViewIntent> BaseMviViewModel<S, I>.state: S
    get() = uiState.value

@Composable
fun <S, I : ViewIntent> BaseMviViewModel<S, I>.asState(
    lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current,
    minActiveState: Lifecycle.State = Lifecycle.State.STARTED,
    context: CoroutineContext = EmptyCoroutineContext
): S {
    val state by uiState.collectAsStateWithLifecycle(
        lifecycleOwner = lifecycleOwner,
        minActiveState = minActiveState,
        context = context
    )
    return state
}

/**
 * MVI 架构基础 ViewModel
 * @param initialState 初始状态
 */
abstract class BaseMviViewModel<State, Intent : ViewIntent>(
    initialState: State
) : ViewModel() {

    // 使用 StateFlow 管理状态
    private val _uiState = MutableStateFlow(initialState)
    val uiState = _uiState.asStateFlow()

    // 使用 SharedFlow 管理副作用（包括错误）
    private val _sideEffect = MutableSharedFlow<SideEffect>()
    val sideEffect = _sideEffect.asSharedFlow()

    /**
     * 处理来自 UI 的意图
     */
    fun dispatch(intent: Intent) {
        viewModelScope.launch {
            try {
                handleIntent(intent)
            } catch (e: Exception) {
                // 自动捕获未处理异常并发送错误
                handleError(e)
            }
        }
    }

    /**
     * 抽象方法 - 具体业务逻辑处理
     */
    protected abstract suspend fun handleIntent(intent: Intent)

    /**
     * 更新状态（使用 reducer 模式保证不可变性）
     */
    protected fun updateState(reducer: State.() -> State) {
        _uiState.update { it.reducer() }
    }

    /**
     * 发送副作用（包括错误）
     */
    protected fun sendEffect(effect: SideEffect) {
        viewModelScope.launch {
            _sideEffect.emit(effect)
        }
    }

    /**
     * 统一错误处理
     */
    protected fun handleError(throwable: Throwable) {
        sendEffect(SideEffect.Error(throwable))
    }

    protected fun launchIO(
        start: CoroutineStart = CoroutineStart.DEFAULT,
        onError: (Throwable) -> Unit = { it.printStackTrace() },
        block: suspend CoroutineScope.() -> Unit
    ): Job =
        launchCatch(
            context = Dispatchers.IO,
            start = start,
            onError = onError,
            block = block
        )


    protected fun launchUI(
        start: CoroutineStart = CoroutineStart.DEFAULT,
        onError: (Throwable) -> Unit = { it.printStackTrace() },
        block: suspend CoroutineScope.() -> Unit
    ): Job =
        launchCatch(
            context = Dispatchers.Main,
            start = start,
            onError = onError,
            block = block
        )

    protected fun launchCatch(
        context: CoroutineContext = EmptyCoroutineContext,
        start: CoroutineStart = CoroutineStart.DEFAULT,
        onError: (Throwable) -> Unit = { it.printStackTrace() },
        block: suspend CoroutineScope.() -> Unit,
    ): Job {
        val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
            throwable.printStackTrace()
            onError(throwable)
        }
        return viewModelScope.launch(context + exceptionHandler, start) {
            runCatching { block.invoke(this) }
                .onFailure {
                    throw if (it is CancellationException) {
                        CancelException(it)
                    } else {
                        it
                    }
                }
        }
    }
}

@Suppress("UndeclaredKoinUsage")
@Composable
inline fun <reified VM: ViewModel> activityKoinViewModel(
    qualifier: Qualifier? = null,
    viewModelStoreOwner: ViewModelStoreOwner = LocalActivity.current as ComponentActivity,
    key: String? = null,
    extras: CreationExtras = defaultExtras(viewModelStoreOwner),
    scope: Scope = currentKoinScope(),
    noinline parameters: ParametersDefinition? = null,
) = koinViewModel<VM>(
    qualifier = qualifier,
    viewModelStoreOwner = viewModelStoreOwner,
    key = key,
    extras = extras,
    scope = scope,
    parameters = parameters
)