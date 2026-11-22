package com.mrl.pixiv.setting.network

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mrl.pixiv.common.network.NetworkDiagnosticResult
import com.mrl.pixiv.common.network.NetworkDiagnosticTarget
import com.mrl.pixiv.common.network.NetworkDiagnostics
import com.mrl.pixiv.common.network.NetworkSettings
import com.mrl.pixiv.common.network.networkSettings
import com.mrl.pixiv.common.repository.requireUserPreferenceValue
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel

internal enum class NetworkDiagnosticRun {
    IDLE,
    RUNNING,
    COMPLETED,
    CANCELED,
}

internal data class NetworkDiagnosticUiState(
    val run: NetworkDiagnosticRun = NetworkDiagnosticRun.IDLE,
    val settings: NetworkSettings? = null,
    val results: Map<NetworkDiagnosticTarget, NetworkDiagnosticResult> = emptyMap(),
)

@KoinViewModel
class NetworkDiagnosticViewModel(private val diagnostics: NetworkDiagnostics) : ViewModel() {
    private val mutableState = MutableStateFlow(NetworkDiagnosticUiState())
    internal val state = mutableState.asStateFlow()
    private var runJob: Job? = null

    fun start() {
        if (mutableState.value.run == NetworkDiagnosticRun.RUNNING) return
        val settings = requireUserPreferenceValue.networkSettings()
        mutableState.value = NetworkDiagnosticUiState(NetworkDiagnosticRun.RUNNING, settings)
        runJob = viewModelScope.launch {
            try {
                diagnostics.run(settings) { result ->
                    if (isActive) {
                        mutableState.update { it.copy(results = it.results + (result.target to result)) }
                    }
                }
            } finally {
                if (isActive) mutableState.update { it.copy(run = NetworkDiagnosticRun.COMPLETED) }
            }
        }
    }

    fun cancel() {
        if (mutableState.value.run != NetworkDiagnosticRun.RUNNING) return
        runJob?.cancel()
        mutableState.update { it.copy(run = NetworkDiagnosticRun.CANCELED) }
    }
}
