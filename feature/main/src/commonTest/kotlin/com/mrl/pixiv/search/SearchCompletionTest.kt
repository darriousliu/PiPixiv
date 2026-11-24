package com.mrl.pixiv.search

import androidx.compose.ui.text.TextRange
import kotlin.test.Test
import kotlin.test.assertEquals

class SearchCompletionTest {
    @Test
    fun additionalKeywordIsSuggestedAndReplacedWithoutLosingTheFirstKeyword() {
        val query = "初音 かわ"
        val token = completionToken(query, TextRange(5))
        assertEquals("かわ", token.text)
        assertEquals("初音 かわいい", token.replaceIn(query, "かわいい"))
    }

    @Test
    fun editingAMiddleKeywordPreservesTheSuffixAndExclusionOperator() {
        val query = "猫 -犬 manga"
        val token = completionToken(query, TextRange(4))
        assertEquals("犬", token.text)
        assertEquals("猫 -狗 manga", token.replaceIn(query, "狗"))
    }

    @Test
    fun trailingWhitespaceDoesNotRequestTheEntireQueryAgain() {
        assertEquals("", completionToken("猫　", TextRange(2)).text)
        assertEquals("", completionToken("", TextRange.Zero).text)
    }

    @Test
    fun reverseSelectionStillReplacesTheSelectedKeyword() {
        val query = "猫 dog 鳥"
        assertEquals("猫 犬 鳥", completionToken(query, TextRange(5, 2)).replaceIn(query, "犬"))
    }
}
