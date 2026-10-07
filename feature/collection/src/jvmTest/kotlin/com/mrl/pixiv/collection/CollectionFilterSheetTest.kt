package com.mrl.pixiv.collection

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import com.mrl.pixiv.common.data.collection.CollectionSearchQuery
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalTestApi::class)
class CollectionFilterSheetTest {
    @Test
    fun sheetEditsAndResetDoNotChangeTheCollectionUntilApply() {
        runDesktopComposeUiTest(testTimeout = 30.seconds) {
            var collection by mutableStateOf(CollectionState())
            var editor by mutableStateOf(CollectionSearchState(sync = CollectionSyncState.FAILED))
            setContent {
                MaterialTheme {
                    Column {
                        Text("results:${collection.illustQuery.bookmarkTag}")
                        CollectionFilterSheetContent(
                            state = editor,
                            isOwner = true,
                            bookmarkTags = emptyList(),
                            tagPage = CollectionTagPage(),
                            onLoadMoreTags = {},
                            onDraftChange = { editor = editor.copy(draft = it) },
                            onRetrySync = {},
                            onRetryOptions = {},
                            onMoreSuggestions = {},
                            onReset = { editor = resetCollectionFilterDraft(editor) },
                            onApply = {
                                editor = applyCollectionSearchDraft(editor)
                                collection = collection.withCollectionFilter(editor.query, true)
                            },
                        )
                    }
                }
            }
            onNodeWithTag("collection-bookmark-tag").performTextReplacement("cats")
            runOnIdle { assertEquals(CollectionSearchQuery(), collection.illustQuery) }
            onNodeWithTag("collection-filter-apply").assertIsEnabled().performClick()
            onNodeWithText("results:cats").assertExists()
            onNodeWithTag("collection-filter-reset").performClick()
            runOnIdle { assertEquals("cats", collection.illustQuery.bookmarkTag) }
            onNodeWithTag("collection-filter-apply").performClick()
            onNodeWithText("results:").assertExists()
            runOnIdle { assertEquals(CollectionSearchQuery(), collection.illustQuery) }
        }
    }
}
