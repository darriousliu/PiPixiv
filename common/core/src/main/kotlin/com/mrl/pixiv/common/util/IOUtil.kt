package com.mrl.pixiv.common.util

import java.io.Closeable
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.io.Reader
import java.io.Writer
import java.net.Socket

fun closeQuietly(input: Reader?) {
    closeQuietly(input as Closeable?)
}

fun closeQuietly(output: Writer?) {
    closeQuietly(output as Closeable?)
}

fun closeQuietly(input: InputStream?) {
    closeQuietly(input as Closeable?)
}

fun closeQuietly(output: OutputStream?) {
    closeQuietly(output as Closeable?)
}

fun closeQuietly(closeable: Closeable?) {
    try {
        closeable?.close()
    } catch (_: IOException) {
    }
}

fun closeQuietly(sock: Socket?) {
    if (sock != null) {
        try {
            sock.close()
        } catch (_: IOException) {
        }
    }
}