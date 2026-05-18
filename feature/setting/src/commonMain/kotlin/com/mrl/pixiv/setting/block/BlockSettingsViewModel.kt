package com.mrl.pixiv.setting.block

import androidx.compose.runtime.Stable
import com.mrl.pixiv.common.viewmodel.BaseMviViewModel
import com.mrl.pixiv.common.viewmodel.ViewIntent
import org.koin.android.annotation.KoinViewModel

@Stable
data class BlockSettingsState(
    val unused: Boolean = false,
)

@KoinViewModel
class BlockSettingsViewModel : BaseMviViewModel<BlockSettingsState, ViewIntent>(
    initialState = BlockSettingsState(),
) {
    override suspend fun handleIntent(intent: ViewIntent) = Unit
}
