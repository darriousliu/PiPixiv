package com.mrl.pixiv.setting.appdata

import com.mrl.pixiv.common.viewmodel.SideEffect

actual fun androidCheckOldData(updateState: (AppDataState.() -> AppDataState) -> Unit) {
}

actual fun androidMigrateData(
    updateState: (AppDataState.() -> AppDataState) -> Unit,
    sendEffect: (SideEffect) -> Unit,
    checkOldData: () -> Unit
) {
}