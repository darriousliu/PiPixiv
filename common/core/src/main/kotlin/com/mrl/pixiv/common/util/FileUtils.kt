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