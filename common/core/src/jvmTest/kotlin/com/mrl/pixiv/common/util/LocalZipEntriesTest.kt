package com.mrl.pixiv.common.util

import java.io.ByteArrayInputStream
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class LocalZipEntriesTest {
    @Test
    fun decompressedDataCannotWritePastItsDeclaredSize() {
        val input = object : ByteArrayInputStream(byteArrayOf(1, 2, 3, 4)) {
            override fun read(buffer: ByteArray, offset: Int, length: Int): Int =
                super.read(buffer, offset, minOf(length, 2))
        }
        var written = 0
        val error = assertFailsWith<IOException> {
            input.copyZipEntry(size = 3, expectedCrc = 0) { _, count -> written += count }
        }
        assertEquals("ZIP entry exceeds the declared size", error.message)
        assertEquals(2, written)
    }
}
