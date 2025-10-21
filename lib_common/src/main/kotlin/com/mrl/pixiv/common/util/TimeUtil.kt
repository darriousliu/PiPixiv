package com.mrl.pixiv.common.util

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.format.Padding
import kotlinx.datetime.format.char
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Instant

fun currentTimeMillis() = Clock.System.now().toEpochMilliseconds()


fun convertUtcStringToLocalDateTime(utcString: String): String {
    val currentTimeZone = TimeZone.currentSystemDefault()
    val instant = Instant.parse(utcString)
    val localDateTime = instant.toLocalDateTime(currentTimeZone)
    val formatter = LocalDateTime.Format {
        year()
        char('-')
        monthNumber()
        char('-')
        day(padding = Padding.ZERO)
        char(' ')
        hour()
        char(':')
        minute()
    }
    return localDateTime.format(formatter)
}