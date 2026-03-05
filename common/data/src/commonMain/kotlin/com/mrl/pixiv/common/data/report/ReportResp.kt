package com.mrl.pixiv.common.data.report

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ReportTopic(
    @SerialName("topic_id")
    val topicId: Int,
    @SerialName("topic_title")
    val topicTitle: String,
)

@Serializable
data class ReportTopicListResp(
    @SerialName("topic_list")
    val topicList: List<ReportTopic>,
)