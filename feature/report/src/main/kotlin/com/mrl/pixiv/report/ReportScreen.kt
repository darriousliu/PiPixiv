package com.mrl.pixiv.report

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.maxLength
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mrl.pixiv.common.router.NavigationManager
import com.mrl.pixiv.common.router.ReportType
import com.mrl.pixiv.common.util.RString
import com.mrl.pixiv.common.util.ToastUtil
import com.mrl.pixiv.common.viewmodel.asState
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

private const val MAX_REPORT_CONTENT_LENGTH = 3000

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportScreen(
    id: Long,
    type: ReportType,
    modifier: Modifier = Modifier,
    viewModel: ReportCommentViewModel = koinViewModel { parametersOf(id, type) }
) {
    val navigationManager = koinInject<NavigationManager>()
    val state = viewModel.asState()
    val reportContent = viewModel.reportContent
    var showTopicSheet by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .imePadding(),
        topBar = {
            TopAppBar(
                title = {
                    Text(text = stringResource(RString.report))
                },
                navigationIcon = {
                    IconButton(
                        onClick = { navigationManager.popBackStack() },
                        shapes = IconButtonDefaults.shapes()
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        if (state.topicList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularWavyProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp)
            ) {
                Text(
                    text = stringResource(RString.report_reason),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                )

                val selectedTopic = state.topicList.find { it.topicId == state.selectedTopicId }
                Text(
                    text = selectedTopic?.topicTitle ?: stringResource(RString.please_select),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showTopicSheet = true }
                        .padding(vertical = 12.dp),
                    style = MaterialTheme.typography.bodyLarge
                )

                Text(
                    text = stringResource(RString.report_detail),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                )

                OutlinedTextField(
                    state = reportContent,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    placeholder = { Text(stringResource(RString.report_detail_hint)) },
                    supportingText = {
                        Text(
                            text = "${reportContent.text.length}/$MAX_REPORT_CONTENT_LENGTH",
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.End
                        )
                    },
                    inputTransformation = InputTransformation.maxLength(MAX_REPORT_CONTENT_LENGTH),
                )

                Button(
                    onClick = {
                        viewModel.submitReport()
                        ToastUtil.safeShortToast(RString.report_success)
                        navigationManager.popBackStack()
                    },
                    enabled = state.selectedTopicId != null && reportContent.text.isNotBlank(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp)
                ) {
                    Text(text = stringResource(RString.send))
                }
            }
        }
    }

    if (showTopicSheet) {
        ModalBottomSheet(
            onDismissRequest = { showTopicSheet = false },
            sheetState = rememberModalBottomSheetState(true)
        ) {
            LazyColumn {
                items(
                    items = state.topicList,
                    key = { it.topicId }
                ) { topic ->
                    ListItem(
                        headlineContent = { Text(topic.topicTitle) },
                        modifier = Modifier.clickable {
                            viewModel.selectTopic(topic.topicId)
                            showTopicSheet = false
                        }
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}
