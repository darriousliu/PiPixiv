package com.mrl.pixiv.common.datasource.local

import androidx.room.Room
import androidx.room.RoomDatabase
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.databasesDir
import io.github.vinceglb.filekit.div
import io.github.vinceglb.filekit.path
import org.koin.core.annotation.Single

@Single
fun getDatabaseBuilder(): RoomDatabase.Builder<PixivDatabase> {
    val dbFilePath = FileKit.databasesDir / "pixiv_db"
    return Room.databaseBuilder<PixivDatabase>(
        name = dbFilePath.path,
    )
}