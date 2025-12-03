package com.mrl.pixiv.search.result.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CollectionsBookmark
import androidx.compose.material.icons.rounded.FilterAlt
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.DateRangePickerDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mrl.pixiv.common.util.RString
import com.mrl.pixiv.common.util.throttleClick
import kotlinx.datetime.LocalDateRange
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SearchResultAppBar(
    searchWords: String,
    bookmarkNumRange: IntRange?,
    searchDateRange: LocalDateRange?,
    onBookmarkNumRangeChanged: (IntRange?) -> Unit,
    onSearchDateRangeChanged: (LocalDateRange?) -> Unit,
    popBack: () -> Unit,
    showBottomSheet: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showBookmarkMenu by remember { mutableStateOf(false) }
    var showDateRangePicker by remember { mutableStateOf(false) }

    TopAppBar(
        modifier = modifier,
        title = {
            Text(
                text = searchWords,
                modifier = Modifier
                    .fillMaxWidth()
                    .throttleClick { popBack() },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        navigationIcon = {
            IconButton(
                onClick = popBack,
                shapes = IconButtonDefaults.shapes(),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Back"
                )
            }
        },
        actions = {
            // Date Range Picker
            IconButton(
                onClick = { showDateRangePicker = true },
                shapes = IconButtonDefaults.shapes(),
            ) {
                Icon(
                    imageVector = Icons.Rounded.CalendarMonth,
                    contentDescription = "Date Range",
                    tint = if (searchDateRange != null) MaterialTheme.colorScheme.primary else LocalContentColor.current
                )
            }
            // Bookmark Range
            IconButton(
                onClick = { showBookmarkMenu = true },
                shapes = IconButtonDefaults.shapes(),
            ) {
                Icon(
                    imageVector = Icons.Rounded.CollectionsBookmark,
                    contentDescription = "Bookmark Range",
                    tint = if (bookmarkNumRange != null) MaterialTheme.colorScheme.primary else LocalContentColor.current
                )
                BookmarkRangeSelector(
                    expanded = showBookmarkMenu,
                    onDismissRequest = { showBookmarkMenu = false },
                    bookmarkNumRange = bookmarkNumRange,
                    onBookmarkNumRangeChanged = onBookmarkNumRangeChanged
                )
            }
            //筛选按钮
            IconButton(
                onClick = showBottomSheet,
                shapes = IconButtonDefaults.shapes(),
            ) {
                Icon(
                    imageVector = Icons.Rounded.FilterAlt,
                    contentDescription = "Filter"
                )
            }
        }
    )
    if (showDateRangePicker) {
        val selectableDates = remember {
            object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    return utcTimeMillis <= Clock.System.now().minus(1.days).toEpochMilliseconds()
                }

                override fun isSelectableYear(year: Int): Boolean {
                    return year <= Clock.System.now()
                        .toLocalDateTime(TimeZone.currentSystemDefault()).year
                }
            }
        }
        val dateRangePickerState = rememberDateRangePickerState(
            initialSelectedStartDateMillis = searchDateRange?.start?.atStartOfDayIn(TimeZone.UTC)
                ?.toEpochMilliseconds(),
            initialSelectedEndDateMillis = searchDateRange?.endInclusive?.atStartOfDayIn(TimeZone.UTC)
                ?.toEpochMilliseconds(),
            selectableDates = selectableDates
        )
        DatePickerDialog(
            onDismissRequest = { showDateRangePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val startDate = dateRangePickerState.selectedStartDateMillis?.let {
                            Instant.fromEpochMilliseconds(it).toLocalDateTime(TimeZone.UTC).date
                        }
                        val endDate = dateRangePickerState.selectedEndDateMillis?.let {
                            Instant.fromEpochMilliseconds(it).toLocalDateTime(TimeZone.UTC).date
                        }
                        if (startDate != null && endDate != null) {
                            onSearchDateRangeChanged(LocalDateRange(startDate, endDate))
                        } else {
                            onSearchDateRangeChanged(null)
                        }
                        showDateRangePicker = false
                    }
                ) {
                    Text(text = stringResource(RString.confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDateRangePicker = false }) {
                    Text(text = stringResource(RString.cancel))
                }
            }
        ) {
            val datePickerFormatter = remember { DatePickerDefaults.dateFormatter() }
            val colors = DatePickerDefaults.colors()
            DateRangePicker(
                state = dateRangePickerState,
                modifier = Modifier.padding(top = 20.dp),
                dateFormatter = datePickerFormatter,
                colors = colors,
                title = {
                    DateRangePickerDefaults.DateRangePickerTitle(
                        displayMode = dateRangePickerState.displayMode,
                        modifier = Modifier.padding(start = 12.dp, end = 12.dp),
                        contentColor = colors.titleContentColor,
                    )
                },
                headline = {
                    ProvideTextStyle(MaterialTheme.typography.titleMedium) {
                        DateRangePickerDefaults.DateRangePickerHeadline(
                            selectedStartDateMillis = dateRangePickerState.selectedStartDateMillis,
                            selectedEndDateMillis = dateRangePickerState.selectedEndDateMillis,
                            displayMode = dateRangePickerState.displayMode,
                            dateFormatter = datePickerFormatter,
                            modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
                            contentColor = colors.headlineContentColor,
                        )
                    }
                }
            )
        }
    }
}

@Composable
private fun BookmarkRangeSelector(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    bookmarkNumRange: IntRange?,
    onBookmarkNumRangeChanged: (IntRange?) -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest
    ) {
        val ranges = listOf(
            stringResource(RString.label_default) to null,
            "10~29" to (10..29),
            "30~49" to (30..49),
            "50~99" to (50..99),
            "100~299" to (100..299),
            "300~499" to (300..499),
            "500~999" to (500..999),
            "1000~4999" to (1000..4999),
            "5000~9999" to (5000..9999),
            "10000~49999" to (10000..49999),
            ">50000" to (50000..Int.MAX_VALUE)
        )

        ranges.forEach { (label, range) ->
            val isSelected = range == bookmarkNumRange
            DropdownMenuItem(
                text = { Text(label) },
                trailingIcon = if (isSelected) {
                    {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = null
                        )
                    }
                } else null,
                onClick = {
                    onBookmarkNumRangeChanged(range)
                    onDismissRequest()
                }
            )
        }
    }
}
