package com.mrl.pixiv.setting.about

import org.intellij.markdown.IElementType
import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.MarkdownTokenTypes
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.flavours.gfm.GFMElementTypes
import org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor
import org.intellij.markdown.parser.CancellationToken
import org.intellij.markdown.parser.MarkdownParser
import kotlin.test.Test
import kotlin.test.assertEquals

class ReleaseNotesMarkdownTest {
    @Test
    fun releaseNotesWithCrLfRetainHeadingTableAndHorizontalRule() {
        assertStructuredReleaseNotes("\r\n")
    }

    @Test
    fun releaseNotesWithLfRetainHeadingTableAndHorizontalRule() {
        assertStructuredReleaseNotes("\n")
    }

    @Test
    fun releaseNotesWithStandaloneCrRetainHeadingTableAndHorizontalRule() {
        assertStructuredReleaseNotes("\r")
    }

    private fun assertStructuredReleaseNotes(lineEnding: String) {
        val source = listOf(
            "# 更新说明",
            "",
            "> 下载对应平台的安装包。",
            "",
            "---",
            "",
            "| 平台 | 下载 |",
            "| --- | --- |",
            "| macOS | [安装包](https://example.com/PiPixiv.dmg) |",
            "",
        ).joinToString(lineEnding)
        val content: CharSequence = normalizeReleaseNotesLineEndings(source)
        val tree = MarkdownParser(
            flavour = GFMFlavourDescriptor(),
            cancellationToken = CancellationToken.NonCancellable,
        ).buildMarkdownTreeFromString(content)

        assertEquals(1, tree.countNodes(MarkdownElementTypes.ATX_1), "Release heading")
        assertEquals(1, tree.countNodes(MarkdownElementTypes.BLOCK_QUOTE), "Download note")
        assertEquals(1, tree.countNodes(GFMElementTypes.TABLE), "Download table")
        assertEquals(1, tree.countNodes(GFMElementTypes.HEADER), "Table header")
        assertEquals(1, tree.countNodes(GFMElementTypes.ROW), "Table body row")
        assertEquals(1, tree.countNodes(MarkdownTokenTypes.HORIZONTAL_RULE), "Section divider")
        assertEquals(1, tree.countNodes(MarkdownElementTypes.INLINE_LINK), "Download link")
    }

    private fun ASTNode.countNodes(type: IElementType): Int =
        (if (this.type == type) 1 else 0) + children.sumOf { it.countNodes(type) }
}
