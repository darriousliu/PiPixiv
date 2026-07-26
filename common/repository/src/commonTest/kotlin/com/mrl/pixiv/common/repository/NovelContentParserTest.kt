package com.mrl.pixiv.common.repository

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class NovelContentParserTest {
    @Test
    fun `extracts novel payload with unknown fields`() {
        val html = """
            <script>
              const preload = {
                novel: {
                  "id":"123",
                  "title":"Title",
                  "userId":"9",
                  "coverUrl":"",
                  "tags":[],
                  "caption":"",
                  "cdate":"",
                  "rating":{"like":0,"bookmark":0,"view":0},
                  "text":"line 1\nline 2",
                  "unknown":{"future":true}
                },
                isOwnWork: false
              };
            </script>
        """.trimIndent()

        val result = NovelContentParser.extract(html)

        assertEquals("123", result?.id)
        assertEquals("line 1\nline 2", result?.text)
    }

    @Test
    fun `returns null for malformed or missing payload`() {
        assertNull(NovelContentParser.extract("<html />"))
        assertNull(
            NovelContentParser.extract(
                "novel: {not-json}, isOwnWork: false"
            )
        )
    }
}
