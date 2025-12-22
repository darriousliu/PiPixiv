package com.mrl.pixiv.common.datasource.local

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import org.koin.core.annotation.Single

@Single
fun getDatabaseBuilder(context: Context): RoomDatabase.Builder<PixivDatabase> {
    val appContext = context.applicationContext
    return Room.databaseBuilder(
        context = appContext,
        PixivDatabase::class.java,
        "pixiv_db"
    )
}