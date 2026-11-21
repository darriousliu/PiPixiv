package com.mrl.pixiv.common.mmkv

import io.github.vinceglb.filekit.FileKit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AccountMMKVPropertyTest {
    private val directory = createTempDirectory("account-mmkv-property").toFile()

    @BeforeTest
    fun setUp() {
        FileKit.init(filesDir = directory.resolve("files"), cacheDir = directory.resolve("cache"))
    }

    @AfterTest
    fun tearDown() {
        closeAccountMMKV(firstUserId)
        closeAccountMMKV(secondUserId)
        directory.deleteRecursively()
    }

    @Test
    fun reloadDefersTheReadUntilFirstCollection() = runBlocking {
        val field = CountingField(mapOf(firstUserId to listOf("saved")))
        val property = lazyHistory(field)

        property.reload(firstUserId)
        assertEquals(0, field.readCount.get())

        assertEquals(listOf("saved"), property.flow.first())
        assertEquals(listOf("saved"), property.flow.value)
        assertEquals(1, field.readCount.get())
    }

    @Test
    fun updateBeforeFirstReadKeepsPersistedHistory() {
        val field = CountingField(mapOf(firstUserId to listOf("saved")))
        val property = lazyHistory(field)

        property.reload(firstUserId)
        property.update(firstUserId) { it + "new" }

        assertEquals(listOf("saved", "new"), field.values[firstUserId])
        assertEquals(listOf("saved", "new"), property.flow.value)
        assertEquals(1, field.readCount.get())
    }

    @Test
    fun cachedFlowFollowsTheNewAccountAndDiscardsTheOldAccountsPrefetch() = runBlocking {
        val readStarted = CountDownLatch(1)
        val finishRead = CountDownLatch(1)
        val field = CountingField(
            mapOf(firstUserId to listOf("first"), secondUserId to listOf("second")),
            beforeRead = { userId ->
                if (userId == firstUserId) {
                    readStarted.countDown()
                    check(finishRead.await(5, TimeUnit.SECONDS))
                }
            },
        )
        val property = lazyHistory(field)
        property.reload(firstUserId)
        val retainedFlow = property.flow

        val prefetch = launch(Dispatchers.Default) { property.prefetch(firstUserId) }
        try {
            assertTrue(readStarted.await(5, TimeUnit.SECONDS))
            property.reload(secondUserId)
            assertEquals(listOf("second"), retainedFlow.value)
        } finally {
            finishRead.countDown()
        }
        prefetch.join()

        assertEquals(listOf("second"), retainedFlow.first())
        assertEquals(listOf("second"), retainedFlow.value)
        assertEquals(2, field.readCount.get())
    }

    private fun lazyHistory(field: CountingField) =
        AccountMMKVProperty(field, "history", emptyList(), loadOnAccess = true)

    private class CountingField(
        initialValues: Map<Long, List<String>>,
        private val beforeRead: (Long) -> Unit = {},
    ) : MMKVField<List<String>> {
        val values = ConcurrentHashMap(initialValues)
        val readCount = AtomicInteger()

        override fun read(owner: MMKVOwner, key: String): List<String> {
            val userId = (owner as MMKVUser).userId
            readCount.incrementAndGet()
            beforeRead(userId)
            return values.getValue(userId)
        }

        override fun write(owner: MMKVOwner, key: String, value: List<String>) {
            values[(owner as MMKVUser).userId] = value
        }
    }

    private companion object {
        const val firstUserId = 1L
        const val secondUserId = 2L
    }
}
