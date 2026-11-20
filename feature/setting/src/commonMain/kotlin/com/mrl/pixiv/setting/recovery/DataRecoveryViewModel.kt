package com.mrl.pixiv.setting.recovery

import androidx.lifecycle.viewModelScope
import com.mrl.pixiv.common.repository.recovery.DataRecoveryRepository
import com.mrl.pixiv.common.repository.recovery.ItemStatus
import com.mrl.pixiv.common.repository.recovery.MigrationItem
import com.mrl.pixiv.common.repository.recovery.isResolved
import com.mrl.pixiv.common.util.BackupSpaceException
import com.mrl.pixiv.common.util.RStrings
import com.mrl.pixiv.common.util.ZipEntryTooLargeException
import com.mrl.pixiv.common.viewmodel.BaseMviViewModel
import com.mrl.pixiv.common.viewmodel.SideEffect
import com.mrl.pixiv.common.viewmodel.ViewIntent
import com.mrl.pixiv.common.viewmodel.state
import com.mrl.pixiv.strings.backup_export_insufficient_space
import com.mrl.pixiv.strings.backup_import_file_too_large
import com.mrl.pixiv.strings.backup_import_insufficient_space
import com.mrl.pixiv.strings.export_success
import com.mrl.pixiv.strings.import_success
import com.mrl.pixiv.strings.recovery_operation_failed
import com.mrl.pixiv.strings.recovery_status_skipped
import io.github.vinceglb.filekit.PlatformFile
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.koin.android.annotation.KoinViewModel

internal data class RetryTarget(val item: MigrationItem, val userId: Long)

internal data class DataRecoveryState(
    val items: Map<MigrationItem, ItemStatus>,
    val activeItem: MigrationItem? = null,
    val isBusy: Boolean = false,
    val retryTarget: RetryTarget? = null,
) {
    val hasPending get() = items.values.any { !it.isResolved }
}

internal data class RecoveryMessage(val text: StringResource) : SideEffect

internal sealed interface RecoveryIntent : ViewIntent {
    data class Retry(val item: MigrationItem, val userId: Long?) : RecoveryIntent
    data class ConfirmRetry(val target: RetryTarget) : RecoveryIntent
    data object DismissRetry : RecoveryIntent
    data class Export(val file: PlatformFile) : RecoveryIntent
    data class Import(val file: PlatformFile, val item: MigrationItem, val userId: Long) : RecoveryIntent
    data object Abandon : RecoveryIntent
}

@KoinViewModel
internal class DataRecoveryViewModel(
    private val repository: DataRecoveryRepository,
) : BaseMviViewModel<DataRecoveryState, RecoveryIntent>(DataRecoveryState(repository.items.value)) {
    init {
        viewModelScope.launch {
            repository.items.collect { latest ->
                updateState { copy(items = latest) }
            }
        }
    }

    override suspend fun handleIntent(intent: RecoveryIntent) {
        if (state.isBusy) return
        when (intent) {
            is RecoveryIntent.Retry -> {
                if (state.retryTarget != null || state.items[intent.item]?.isResolved != false) return
                runOperation(intent.item) {
                    if (repository.needsTargetAccount(intent.item) && intent.userId != null) {
                        updateState { copy(retryTarget = RetryTarget(intent.item, intent.userId)) }
                    } else {
                        val target = intent.userId.takeIf { intent.item == MigrationItem.Session }
                        repository.retry(intent.item, target)
                    }
                }
            }

            is RecoveryIntent.ConfirmRetry -> {
                if (state.retryTarget != intent.target) return
                updateState { copy(retryTarget = null) }
                runOperation(intent.target.item) { repository.retry(intent.target.item, intent.target.userId) }
            }

            RecoveryIntent.DismissRetry -> updateState { copy(retryTarget = null) }
            is RecoveryIntent.Export -> runOperation(spaceFailure = RStrings.backup_export_insufficient_space) {
                repository.exportLegacyData(intent.file)
                sendEffect(RecoveryMessage(RStrings.export_success))
            }

            is RecoveryIntent.Import -> runOperation(spaceFailure = RStrings.backup_import_insufficient_space) {
                val status = repository.importLegacyPackage(intent.file, intent.userId, intent.item)
                val message = if (status == ItemStatus.Skipped) RStrings.recovery_status_skipped else RStrings.import_success
                sendEffect(RecoveryMessage(message))
            }

            RecoveryIntent.Abandon -> runOperation { repository.abandonAll() }
        }
    }

    private suspend fun runOperation(
        item: MigrationItem? = null,
        spaceFailure: StringResource = RStrings.recovery_operation_failed,
        operation: suspend () -> Unit,
    ) {
        updateState { copy(isBusy = true, activeItem = item) }
        try {
            operation()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: BackupSpaceException) {
            sendEffect(RecoveryMessage(spaceFailure))
        } catch (_: ZipEntryTooLargeException) {
            sendEffect(RecoveryMessage(RStrings.backup_import_file_too_large))
        } catch (error: Exception) {
            handleError(error)
        } finally {
            updateState { copy(isBusy = false, activeItem = null) }
        }
    }
}
