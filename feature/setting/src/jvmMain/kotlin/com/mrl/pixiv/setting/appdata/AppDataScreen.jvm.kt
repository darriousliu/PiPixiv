package com.mrl.pixiv.setting.appdata

import androidx.compose.runtime.Composable

@Composable
actual fun MigrationCard(
    state: AppDataState,
    migrateData: () -> Unit,
    viewModel: AppDataViewModel
) {
}