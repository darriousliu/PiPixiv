package com.mrl.pixiv.common.datasource.local

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.mrl.pixiv.common.datasource.local.entity.DownloadEntity
import com.mrl.pixiv.common.datasource.local.entity.DownloadStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class DownloadAttemptTest {
    @Test
    fun `background callbacks cannot overwrite a retry or completed download`() = runTest {
        val database = Room.inMemoryDatabaseBuilder<PixivDatabase>()
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .build()
        try {
            val dao = database.downloadDao()
            val old = DownloadEntity(
                illustId = 11,
                index = 0,
                title = "Illustration",
                userId = 2,
                userName = "Artist",
                thumbnailUrl = "",
                originalUrl = "https://example.com/image.jpg",
                status = DownloadStatus.RUNNING.value,
                createTime = 100,
            )
            val retry = old.copy(status = DownloadStatus.PENDING.value, createTime = 101)
            dao.insert(old)
            dao.insert(retry)

            assertEquals(0, dao.updateActiveAttempt(11, 0, 100, DownloadStatus.FAILED.value))
            assertEquals(retry, dao.getDownload(11, 0))
            assertEquals(
                1,
                dao.updateActiveAttempt(
                    illustId = 11,
                    index = 0,
                    createTime = 101,
                    status = DownloadStatus.SUCCESS.value,
                    progress = 1f,
                    fileUri = "ph://saved-image",
                ),
            )
            assertEquals(0, dao.updateActiveAttempt(11, 0, 101, DownloadStatus.RUNNING.value, progress = 0.5f))
            assertEquals(
                retry.copy(status = DownloadStatus.SUCCESS.value, progress = 1f, fileUri = "ph://saved-image"),
                dao.getDownload(11, 0),
            )
        } finally {
            database.close()
        }
    }
}
