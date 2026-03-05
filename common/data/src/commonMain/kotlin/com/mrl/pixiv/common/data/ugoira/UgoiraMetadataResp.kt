package com.mrl.pixiv.common.data.ugoira

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UgoiraMetadataResp(
    @SerialName("ugoira_metadata")
    val ugoiraMetadata: UgoiraMetadata
)

@Serializable
data class UgoiraMetadata(
    val frames: List<Frame>,

    @SerialName("zip_urls")
    val zipUrls: ZipUrls
)

@Serializable
data class Frame(
    val delay: Long,
    val file: String
)

@Serializable
data class ZipUrls(
    val medium: String
)
