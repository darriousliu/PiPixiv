package com.mrl.pixiv.common.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract

fun getOldDownloadDirUri(): Uri {
    // 外部存储的主卷 ID 通常是 "primary"
    val authority = "com.android.externalstorage.documents"
    val documentId = "primary:${Environment.DIRECTORY_DCIM}/PiPixiv"
    // 构造出代表该目录的 Document URI
    return DocumentsContract.buildDocumentUri(authority, documentId)
}

fun getNewDownloadDirUri(): Uri {
    // 外部存储的主卷 ID 通常是 "primary"
    val authority = "com.android.externalstorage.documents"
    val documentId = "primary:${Environment.DIRECTORY_PICTURES}/PiPixiv"
    // 构造出代表该目录的 Document URI
    return DocumentsContract.buildDocumentUri(authority, documentId)
}


// 用户在文件选择器中点击“使用此文件夹”后的处理
fun handleTreeUriGranted(context: Context, uri: Uri) {
    // 1. 声明我们要永久保留这个读写权限
    val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
    context.contentResolver.takePersistableUriPermission(uri, flags)
}