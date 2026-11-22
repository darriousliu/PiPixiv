package com.mrl.pixiv.picture

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitLongPressOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Deselect
import androidx.compose.material.icons.rounded.SelectAll
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.mrl.pixiv.common.util.RStrings
import com.mrl.pixiv.common.util.getScreenHeight
import com.mrl.pixiv.strings.artwork_download_all_originals
import com.mrl.pixiv.strings.artwork_download_clear_selection
import com.mrl.pixiv.strings.artwork_download_page
import com.mrl.pixiv.strings.artwork_download_pages
import com.mrl.pixiv.strings.artwork_download_selected
import com.mrl.pixiv.strings.artwork_download_selection_count
import com.mrl.pixiv.strings.select_all
import org.jetbrains.compose.resources.stringResource
import kotlin.math.ceil

internal data class ArtworkDownloadPage(val index: Int, val thumbnailUrl: String)

@Composable
internal fun ArtworkDownloadPagePicker(
    pages: List<ArtworkDownloadPage>,
    onDismissRequest: () -> Unit,
    onDownload: (List<Int>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val pageIndices = remember(pages) { pages.map { it.index } }
    var selection by rememberSaveable { mutableStateOf(arrayListOf<Int>()) }
    val selectedPages = remember(selection, pageIndices) { selection.toSet().intersect(pageIndices.toSet()) }
    val sheetState = rememberBottomSheetState(
        initialValue = SheetValue.Hidden,
        enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded),
    )
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        sheetState = sheetState,
    ) {
        ArtworkDownloadPagePickerContent(
            pages = pages,
            selectedPages = selectedPages,
            onPageSelectionChange = { index, selected ->
                if (selected != (index in selection)) {
                    selection = ArrayList(if (selected) selection + index else selection - index)
                }
            },
            onSelectAll = { selection = ArrayList(pageIndices) },
            onClearSelection = { selection = arrayListOf() },
            onDownload = onDownload,
            modifier = Modifier.heightIn(max = getScreenHeight() * 0.85f),
        )
    }
}

@Composable
internal fun ArtworkDownloadPagePickerContent(
    pages: List<ArtworkDownloadPage>,
    selectedPages: Set<Int>,
    onPageSelectionChange: (Int, Boolean) -> Unit,
    onSelectAll: () -> Unit,
    onClearSelection: () -> Unit,
    onDownload: (List<Int>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val gridState = rememberLazyGridState()
    val currentSelection by rememberUpdatedState(selectedPages)
    val changeSelection by rememberUpdatedState(onPageSelectionChange)
    val allIndices = remember(pages) { pages.map { it.index } }
    val selectedIndices = remember(allIndices, selectedPages) { allIndices.filter { it in selectedPages } }
    val fontScale = LocalDensity.current.fontScale

    val allSelected = pages.isNotEmpty() && selectedIndices.size == pages.size
    val selectionActionLabel = stringResource(
        if (allSelected) RStrings.artwork_download_clear_selection else RStrings.select_all
    )
    val header: @Composable () -> Unit = {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = stringResource(RStrings.artwork_download_pages),
                    modifier = Modifier.semantics { heading() },
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    text = stringResource(RStrings.artwork_download_selection_count, selectedIndices.size, pages.size),
                    modifier = Modifier.padding(top = 4.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            TooltipBox(
                positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
                tooltip = { PlainTooltip { Text(selectionActionLabel) } },
                state = rememberTooltipState(),
                enableUserInput = pages.isNotEmpty(),
            ) {
                IconButton(
                    onClick = if (allSelected) onClearSelection else onSelectAll,
                    enabled = pages.isNotEmpty(),
                ) {
                    Icon(
                        imageVector = if (allSelected) Icons.Rounded.Deselect else Icons.Rounded.SelectAll,
                        contentDescription = selectionActionLabel,
                    )
                }
            }
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, bottom = 16.dp),
    ) {
        val compactHeight = maxHeight < 380.dp * fontScale.coerceIn(1f, 1.5f)
        val showButtonsSideBySide = maxWidth >= 400.dp * fontScale.coerceIn(1f, 1.4f)
        val scrollHeader = compactHeight && !showButtonsSideBySide
        val thumbnailWidth = if (compactHeight && showButtonsSideBySide) 84.dp else {
            84.dp * fontScale.coerceIn(1f, 1.5f)
        }
        Column {
            if (!scrollHeader) header()
            LazyVerticalGrid(
                columns = GridCells.Adaptive(thumbnailWidth),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .pointerInput(gridState) {
                        val sampleDistance = 12.dp.toPx()

                        fun pageAt(position: Offset): Int? {
                            if (position.x < 0 || position.x >= size.width ||
                                position.y < 0 || position.y >= size.height
                            ) return null
                            val item = gridState.layoutInfo.visibleItemsInfo.firstOrNull {
                                position.x >= it.offset.x && position.x < it.offset.x + it.size.width &&
                                    position.y >= it.offset.y && position.y < it.offset.y + it.size.height
                            }
                            return item?.key as? Int
                        }

                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            val press = awaitLongPressOrCancellation(down.id) ?: return@awaitEachGesture
                            val firstIndex = pageAt(press.position) ?: return@awaitEachGesture
                            val selectOnDrag = firstIndex !in currentSelection
                            val visited = mutableSetOf(firstIndex)
                            changeSelection(firstIndex, selectOnDrag)

                            while (true) {
                                // Once selection starts, take moves before the grid's scroll and tile clicks.
                                val event = awaitPointerEvent(PointerEventPass.Initial)
                                val change = event.changes.firstOrNull { it.id == press.id } ?: break
                                if (change.isConsumed) break
                                val dragAmount = change.position - change.previousPosition
                                // Fast pointer moves can cross a thumbnail between events.
                                val steps = ceil(dragAmount.getDistance() / sampleDistance).toInt().coerceIn(1, 64)
                                for (step in 1..steps) {
                                    val position = change.previousPosition + dragAmount * (step.toFloat() / steps)
                                    val index = pageAt(position)
                                    if (index != null && visited.add(index)) changeSelection(index, selectOnDrag)
                                }
                                change.consume()
                                if (!change.pressed) break
                            }
                        }
                    },
                state = gridState,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (scrollHeader) {
                    item(key = "header", span = { GridItemSpan(maxLineSpan) }) { header() }
                }
                items(pages, key = { it.index }) { page ->
                    ArtworkDownloadThumbnail(
                        page = page,
                        selected = page.index in selectedPages,
                        onSelectionChange = { selected -> onPageSelectionChange(page.index, selected) },
                    )
                }
            }
            Box(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
                val downloadAll: @Composable (Modifier) -> Unit = { buttonModifier ->
                    FilledTonalButton(
                        onClick = { onDownload(allIndices) },
                        modifier = buttonModifier,
                        enabled = allIndices.isNotEmpty(),
                    ) {
                        Text(stringResource(RStrings.artwork_download_all_originals), textAlign = TextAlign.Center)
                    }
                }
                val downloadSelected: @Composable (Modifier) -> Unit = { buttonModifier ->
                    Button(
                        onClick = { onDownload(selectedIndices) },
                        modifier = buttonModifier,
                        enabled = selectedIndices.isNotEmpty(),
                    ) {
                        Text(stringResource(RStrings.artwork_download_selected), textAlign = TextAlign.Center)
                    }
                }
                if (showButtonsSideBySide) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        downloadAll(Modifier.weight(1f))
                        downloadSelected(Modifier.weight(1f))
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        downloadAll(Modifier.fillMaxWidth())
                        downloadSelected(Modifier.fillMaxWidth())
                    }
                }
            }
        }
    }
}

@Composable
private fun ArtworkDownloadThumbnail(
    page: ArtworkDownloadPage,
    selected: Boolean,
    onSelectionChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = MaterialTheme.shapes.medium
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .toggleable(value = selected, role = Role.Checkbox, onValueChange = onSelectionChange),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(shape)
                .background(colors.surfaceContainerHighest)
                .border(2.dp, if (selected) colors.primary else colors.outlineVariant, shape),
        ) {
            AsyncImage(
                model = page.thumbnailUrl,
                contentDescription = null,
                modifier = Modifier.matchParentSize(),
                contentScale = ContentScale.Crop,
            )
            Surface(
                modifier = Modifier.align(Alignment.TopEnd).padding(6.dp).size(24.dp),
                shape = CircleShape,
                color = if (selected) colors.primary else colors.surface,
                contentColor = colors.onPrimary,
                border = if (selected) null else BorderStroke(1.dp, colors.outline),
            ) {
                if (selected) {
                    Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.padding(3.dp))
                }
            }
        }
        Text(
            text = stringResource(RStrings.artwork_download_page, page.index + 1),
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium,
            textAlign = TextAlign.Center,
        )
    }
}
