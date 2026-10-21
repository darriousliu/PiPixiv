package com.mrl.pixiv.common.compose.ui.novel

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.WatchLater
import androidx.compose.material.icons.rounded.WatchLater
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemColors
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.intl.Locale
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mrl.pixiv.common.compose.rememberThrottleClick
import com.mrl.pixiv.common.data.Novel
import com.mrl.pixiv.common.repository.NovelReadLaterRepository
import com.mrl.pixiv.common.repository.requireUserPreferenceFlow
import com.mrl.pixiv.common.util.RStrings
import com.mrl.pixiv.common.util.ToastUtil
import com.mrl.pixiv.strings.add_to_read_later
import com.mrl.pixiv.strings.ai_translation_config_required
import com.mrl.pixiv.strings.ai_translation_failed
import com.mrl.pixiv.strings.read_later_added
import com.mrl.pixiv.strings.read_later_removed
import com.mrl.pixiv.strings.remove_from_read_later
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

@Composable
fun NovelReadLaterButton(
    novel: Novel,
    modifier: Modifier = Modifier,
    tint: Color = Color.Unspecified,
    repository: NovelReadLaterRepository = koinInject(),
) {
    NovelReadLaterAction(novel, repository) { isAdded, onClick ->
        IconButton(modifier = modifier, onClick = onClick) {
            Icon(
                imageVector = if (isAdded) Icons.Rounded.WatchLater else Icons.Outlined.WatchLater,
                contentDescription = stringResource(
                    if (isAdded) RStrings.remove_from_read_later else RStrings.add_to_read_later
                ),
                tint = tint,
            )
        }
    }
}

@Composable
fun NovelReadLaterListItem(
    novel: Novel,
    modifier: Modifier = Modifier,
    colors: ListItemColors = ListItemDefaults.colors(),
    repository: NovelReadLaterRepository = koinInject(),
) {
    NovelReadLaterAction(novel, repository) { isAdded, onClick ->
        ListItem(
            onClick = rememberThrottleClick(onClick = onClick),
            shapes = ListItemDefaults.shapes(shape = RectangleShape),
            content = {
                Text(
                    text = stringResource(
                        if (isAdded) RStrings.remove_from_read_later else RStrings.add_to_read_later
                    )
                )
            },
            modifier = modifier,
            leadingContent = {
                Icon(
                    imageVector = if (isAdded) Icons.Rounded.WatchLater else Icons.Outlined.WatchLater,
                    contentDescription = null,
                )
            },
            colors = colors,
        )
    }
}

@Composable
private fun NovelReadLaterAction(
    novel: Novel,
    repository: NovelReadLaterRepository,
    content: @Composable (isAdded: Boolean, onClick: () -> Unit) -> Unit,
) {
    val preference by requireUserPreferenceFlow.collectAsStateWithLifecycle()
    val targetLanguage = preference.appLanguage
        ?.takeIf { it.isNotBlank() }
        ?: Locale.current.toLanguageTag().ifBlank { "en" }
    val itemFlow = remember(novel.id, targetLanguage) {
        repository.observeItem(
            novelId = novel.id,
            targetLanguage = targetLanguage,
        )
    }
    val item by itemFlow.collectAsStateWithLifecycle(initialValue = null)
    val scope = rememberCoroutineScope()
    val isAdded = item != null
    content(isAdded) {
        scope.launch {
            try {
                if (!isAdded) {
                    repository.enqueue(
                        novel = novel,
                        targetLanguage = targetLanguage,
                    )
                } else {
                    repository.remove(
                        novelId = novel.id,
                        targetLanguage = targetLanguage,
                    )
                }
                ToastUtil.safeShortToast(
                    if (!isAdded) {
                        RStrings.read_later_added
                    } else {
                        RStrings.read_later_removed
                    }
                )
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: IllegalArgumentException) {
                ToastUtil.safeShortToast(RStrings.ai_translation_config_required)
            } catch (throwable: Throwable) {
                ToastUtil.safeShortToast(
                    RStrings.ai_translation_failed,
                    throwable.message.orEmpty(),
                )
            }
        }
    }
}
