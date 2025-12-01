package com.mrl.pixiv.common.util

import androidx.annotation.WorkerThread
import java.io.File

val File?.isExist: Boolean
    get() = this != null && exists()

fun joinPaths(vararg paths: String): String {
    var file = File(paths[0])
    for (i in 1 until paths.size) {
        file = file.resolve(paths[i])
    }
    return file.path
}

fun deleteFiles(path: String): Boolean {
    val file = File(path)
    if (file.isExist) {
        return deleteFiles(file)
    }

    return true
}

/**
 * 删除文件或者文件夹
 *
 * @param file 待删除的文件或文件夹
 * @return 删除是否成功
 */
@WorkerThread
fun deleteFiles(file: File): Boolean {
    if (file.isDirectory) {
        val children = file.list()
        if (children != null) {
            for (i in children.indices) {
                val success = deleteFiles(File(file, children[i]))
                if (!success) {
                    return false
                }
            }
        }
    }
    return file.delete()
}


/**
 * 计算文件或文件夹的大小。
 *
 * 如果是文件夹，则递归计算其所有子文件和子文件夹的大小之和；
 * 如果是文件，则直接返回该文件的大小（单位为B）。
 *
 * @return 文件或文件夹的大小，单位为KB。如果是空文件夹，返回值为0。
 */
fun File.calculateSize(): Long {
    if (isDirectory) {
        return listFiles()?.sumOf { it.calculateSize() } ?: 0
    }
    return length().coerceAtMost(
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