package com.mrl.pixiv.common.data.setting

import kotlinx.serialization.Serializable

@Serializable
data class NovelReaderSettings(
    val fontSize: Int = DEFAULT_FONT_SIZE,
    val lineSpacingSp: Int = DEFAULT_LINE_SPACING_SP,
) {
    fun normalized(): NovelReaderSettings = copy(
        fontSize = fontSize.coerceIn(MIN_FONT_SIZE, MAX_FONT_SIZE),
        lineSpacingSp = lineSpacingSp.coerceIn(MIN_LINE_SPACING_SP, MAX_LINE_SPACING_SP),
    )

    companion object {
        const val DEFAULT_FONT_SIZE = 16
        const val MIN_FONT_SIZE = 10
        const val MAX_FONT_SIZE = 32

        const val DEFAULT_LINE_SPACING_SP = 0
        const val MIN_LINE_SPACING_SP = -10
        const val MAX_LINE_SPACING_SP = 10
    }
}
