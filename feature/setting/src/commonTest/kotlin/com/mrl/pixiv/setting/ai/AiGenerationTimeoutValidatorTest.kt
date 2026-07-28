package com.mrl.pixiv.setting.ai

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AiGenerationTimeoutValidatorTest {
    @Test
    fun rejectsEmptyNonNumericOutOfRangeAndOverflowInput() {
        listOf(
            "",
            "seconds",
            "29",
            "1801",
            "999999999999999999999",
        ).forEach { input ->
            assertNull(
                actual = parseGenerationTimeoutSeconds(input),
                message = "Expected \"$input\" to be invalid",
            )
        }
    }

    @Test
    fun acceptsRangeBoundariesDefaultAndLeadingZeros() {
        mapOf(
            "30" to 30,
            "180" to 180,
            "1800" to 1800,
            "0030" to 30,
        ).forEach { (input, expected) ->
            assertEquals(
                expected = expected,
                actual = parseGenerationTimeoutSeconds(input),
            )
        }
    }
}
