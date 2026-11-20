package com.mrl.pixiv.setting.recovery

import androidx.lifecycle.viewModelScope
import com.mrl.pixiv.common.repository.recovery.DataRecoveryRepository
import com.mrl.pixiv.common.repository.recovery.ItemStatus
import com.mrl.pixiv.common.repository.recovery.MigrationItem
import com.mrl.pixiv.common.repository.recovery.MigrationReason
import io.github.vinceglb.filekit.PlatformFile
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class DataRecoveryViewModelTest {
    @Test
    fun retryLocksActionsWhileRecoveringOnlyTheSelectedItem() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val repository = RecoveryFake()
        val viewModel = DataRecoveryViewModel(repository)
        try {
            viewModel.dispatch(RecoveryIntent.Retry(MigrationItem.BrowsingHistory, userId = 101))
            runCurrent()
            assertTrue(viewModel.uiState.value.isBusy)
            assertEquals(MigrationItem.BrowsingHistory, viewModel.uiState.value.activeItem)

            viewModel.dispatch(RecoveryIntent.Retry(MigrationItem.ReadingProgress, userId = 101))
            runCurrent()
            repository.retryGate.complete(Unit)
            runCurrent()

            assertEquals(listOf(MigrationItem.BrowsingHistory), repository.retried)
            assertEquals(ItemStatus.Done, viewModel.uiState.value.items[MigrationItem.BrowsingHistory])
            assertEquals(ItemStatus.Pending(), viewModel.uiState.value.items[MigrationItem.ReadingProgress])
            assertFalse(viewModel.uiState.value.isBusy)
        } finally {
            viewModel.viewModelScope.cancel()
            Dispatchers.resetMain()
        }
    }

    private class RecoveryFake : DataRecoveryRepository {
        override val items = MutableStateFlow(
            mapOf(
                MigrationItem.BrowsingHistory to ItemStatus.Failed(1, MigrationReason.Io),
                MigrationItem.ReadingProgress to ItemStatus.Pending(),
            ),
        )
        override val hasPending = MutableStateFlow(true)
        override val recoverAfterLogin = MutableStateFlow(false)
        val retried = mutableListOf<MigrationItem>()
        val retryGate = CompletableDeferred<Unit>()

        override suspend fun needsTargetAccount(item: MigrationItem) = false

        override suspend fun retry(item: MigrationItem, targetUserId: Long?): ItemStatus {
            retried += item
            retryGate.await()
            items.value += item to ItemStatus.Done
            return ItemStatus.Done
        }

        override suspend fun exportLegacyData(target: PlatformFile): Unit = error("Unexpected export")
        override suspend fun importLegacyPackage(source: PlatformFile, targetUserId: Long): Unit = error("Unexpected import")
        override suspend fun importLegacyPackage(source: PlatformFile, targetUserId: Long, item: MigrationItem): ItemStatus =
            error("Unexpected import")
        override suspend fun abandonAll(): Unit = error("Unexpected abandon")
    }
}
