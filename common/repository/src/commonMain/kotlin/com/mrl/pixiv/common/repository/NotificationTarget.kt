package com.mrl.pixiv.common.repository

import io.ktor.http.Url

sealed interface NotificationTarget {
    data class Illust(val id: Long) : NotificationTarget
    data class Novel(val id: Long) : NotificationTarget
    data class User(val id: Long) : NotificationTarget
    data class Web(val url: String) : NotificationTarget
}

/** Resolve complete trusted URLs, not a matching URL embedded inside an unrelated string. */
fun resolveNotificationTarget(raw: String?): NotificationTarget? {
    if (raw.isNullOrBlank()) return null
    val url = runCatching { Url(raw) }.getOrNull() ?: return null
    if (!url.user.isNullOrEmpty() || !url.password.isNullOrEmpty()) return null
    val segments = url.encodedPath.split('/').filter { it.isNotEmpty() }
    fun id(value: String?) = value?.toLongOrNull()?.takeIf { it > 0 }
    if (url.protocol.name == "pixiv") {
        val targetId = id(segments.singleOrNull()) ?: return null
        return when (url.host) {
            "illusts", "illust" -> NotificationTarget.Illust(targetId)
            "novels", "novel" -> NotificationTarget.Novel(targetId)
            "users", "user" -> NotificationTarget.User(targetId)
            else -> null
        }
    }
    if (url.protocol.name != "https" || url.port != 443) return null
    val host = url.host.lowercase()
    if (host != "pixiv.net" && !host.endsWith(".pixiv.net") &&
        host != "pixivision.net" && host != "www.pixivision.net"
    ) return null
    if (host == "pixiv.net" || host == "www.pixiv.net") {
        val path = if (segments.firstOrNull()?.matches(Regex("[a-z]{2}(?:-[a-z]{2})?")) == true) {
            segments.drop(1)
        } else segments
        if (path.size == 2) {
            when (path[0]) {
                "artworks" -> id(path[1])?.let { return NotificationTarget.Illust(it) }
                "users" -> id(path[1])?.let { return NotificationTarget.User(it) }
                "novel" -> if (path[1] == "show.php") {
                    id(url.parameters["id"])?.let { return NotificationTarget.Novel(it) }
                }
            }
        }
        if (path.singleOrNull() == "member_illust.php") {
            id(url.parameters["illust_id"])?.let { return NotificationTarget.Illust(it) }
        }
        if (path.singleOrNull() == "member.php") {
            id(url.parameters["id"])?.let { return NotificationTarget.User(it) }
        }
    }
    return NotificationTarget.Web(raw)
}
