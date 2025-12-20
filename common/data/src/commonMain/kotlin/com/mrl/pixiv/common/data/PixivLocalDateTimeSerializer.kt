package com.mrl.pixiv.common.data

import kotlinx.datetime.LocalDate.Formats.ISO
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.UtcOffset
import kotlinx.datetime.format
import kotlinx.datetime.format.DateTimeComponents
import kotlinx.datetime.format.alternativeParsing
import kotlinx.datetime.format.char
import kotlinx.datetime.format.optional
import kotlinx.datetime.format.parse
import kotlinx.datetime.toInstant
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

private val iso8601DateTimeFormat = DateTimeComponents.Format {
    date(ISO)
    alternativeParsing({
        char('t')
    }) {
        char('T')
    }
    hour()
    char(':')
    minute()
    alternativeParsing({}) {
        char(':')
        second()
        optional {
            char('.')
            secondFraction(1, 9)
        }
    }
    offset(UtcOffset.Formats.ISO)
}

class PixivLocalDateTimeSerializer : KSerializer<LocalDateTime> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("kotlinx.datetime.LocalDateTime", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): LocalDateTime =
        DateTimeComponents.parse(decoder.decodeString(), iso8601DateTimeFormat).toLocalDateTime()

    override fun serialize(encoder: Encoder, value: LocalDateTime) {
        encoder.encodeString(value.toInstant(TimeZone.UTC).format(iso8601DateTimeFormat))
    }
}