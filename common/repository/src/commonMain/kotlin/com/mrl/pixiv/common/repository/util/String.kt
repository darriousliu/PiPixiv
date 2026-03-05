package com.mrl.pixiv.common.repository.util

import io.ktor.http.Url
import io.ktor.util.toMap

val String.queryParams: Map<String, String>
    get() = Url(this).parameters.toMap().mapValues { it.value.joinToString(" ") }