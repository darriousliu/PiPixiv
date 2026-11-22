package com.mrl.pixiv.common.datasource.local

import androidx.room3.Room
import androidx.room3.RoomDatabase

internal actual fun getGlobalDatabaseBuilder(path: String): RoomDatabase.Builder<PixivDatabase> =
    Room.databaseBuilder(name = path, factory = PixivDatabaseConstructor::initialize)

internal actual fun getAccountDatabaseBuilder(path: String): RoomDatabase.Builder<AccountDatabase> =
    Room.databaseBuilder(name = path, factory = AccountDatabaseConstructor::initialize)
