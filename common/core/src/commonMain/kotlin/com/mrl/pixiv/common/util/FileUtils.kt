package com.mrl.pixiv.common.util

import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.isDirectory
import io.github.vinceglb.filekit.list
import io.github.vinceglb.filekit.size
import net.sergeych.sprintf.format


/**
 * 计算文件或文件夹的大小。
 *
 * 如果是文件夹，则递归计算其所有子文件和子文件夹的大小之和；
 * 如果是文件，则直接返回该文件的大小（单位为B）。
 *
 * @return 文件或文件夹的大小，单位为KB。如果是空文件夹，返回值为0。
 */
fun PlatformFile.calculateSize(): Long {
    if (isDirectory()) {
        return list().sumOf { it.calculateSize() }
    }
    return size().coerceAtMost(
        Long.MAX_VALUE
    )
}

private const val KB = 1024
private const val MB = KB * 1024
private const val GB = MB * 1024

fun Long.adaptiveFileSize(): String {
    return when (this) {
        in 0L..<KB -> "%.2f B".format(toFloat())
        in KB..<MB -> "%.2f KB".format(toFloat() / KB)
        in MB..<GB -> "%.2f MB".format(toFloat() / MB)
        else -> "%.2f GB".format(toFloat() / GB)
    }
}

fun PlatformFile.listFilesRecursively(): List<PlatformFile> {
    val result = mutableListOf<PlatformFile>()
    if (isDirectory()) {
        val children = list()
        for (child in children) {
            result.addAll(child.listFilesRecursively())
        }
    } else {
        result.add(this)
    }
    return result
}