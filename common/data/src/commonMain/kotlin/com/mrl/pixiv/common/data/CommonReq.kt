package com.mrl.pixiv.common.data

enum class Filter(val value: String) {
    ANDROID("for_android"),
    IOS("for_ios"),
}

enum class Restrict(val value: String) {
    PUBLIC("public"),
    PRIVATE("private"),
    ALL("all");

    companion object {
        fun fromValue(value: String): Restrict {
            return entries.find { it.value == value } ?: PUBLIC
        }
    }
}
