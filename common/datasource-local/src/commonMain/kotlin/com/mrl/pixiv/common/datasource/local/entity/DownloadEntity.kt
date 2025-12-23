package com.mrl.pixiv.common.datasource.local.entity

import androidx.compose.runtime.Stable
import androidx.room.Entity
import kotlinx.serialization.Serializable

/**
 * 下载实体类，用于表示与下载任务相关的数据。
 *
 * @property illustId 插画的唯一标识符。
 * @property index 插画的索引，适用于多页图片。
 * @property title 插画的标题。
 * @property userId 插画作者的用户ID。
 * @property userName 插画作者的用户名。
 * @property thumbnailUrl 缩略图的URL地址。
 * @property originalUrl 原始图片的URL地址。
 * @property subFolder 下载图片的子文件夹路径，可选。
 * @property status 下载状态：
 * - 0: 初始状态，尚未下载。
 * - 1: 下载进行中。
 * - 2: 下载成功。
 * - 3: 下载失败。
 * @property progress 下载进度，范围为0到1。
 * @property filePath 下载后的文件路径：
 * - Android: /storage/emulated/0/Pictures/PiPixiv/[userId]/[illustId]_p[index].jpg
 * - iOS: 空字符串
 * - Desktop: %APPDATA%/<app-id>/Downloads/[userId]/[illustId]_p[index].jpg
 * @property fileUri 下载后的文件URI：
 * - Android: content://格式。
 * - iOS: ph://<PHAsset.localIdentifier>
 * - Desktop: file:///格式。
 * @property createTime 创建时间的时间戳。
 */
@Entity(tableName = "download", primaryKeys = ["illustId", "index"])
@Stable
@Serializable
data class DownloadEntity(
    val illustId: Long,
    val index: Int,
    val title: String,
    val userId: Long,
    val userName: String,
    val thumbnailUrl: String,
    val originalUrl: String,
    val subFolder: String? = null,
    val status: Int, // 0: Pending, 1: Running, 2: Success, 3: Failed
    val progress: Float = 0f,
    val filePath: String = "",
    val fileUri: String = "",
    val createTime: Long,
)

enum class DownloadStatus(val value: Int) {
    PENDING(0),
    RUNNING(1),
    SUCCESS(2),
    FAILED(3),
}
