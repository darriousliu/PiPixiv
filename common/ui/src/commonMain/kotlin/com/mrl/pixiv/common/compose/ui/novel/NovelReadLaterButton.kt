package com.mrl.pixiv.common.compose.ui.novel

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.WatchLater
import androidx.compose.material.icons.rounded.WatchLater
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.intl.Locale
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
    IconButton(
        modifier = modifier,
        onClick = {
            scope.launch {
                try {
                    if (item == null) {
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
                        if (item == null) {
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
    ) {
        Icon(
            imageVector = if (item == null) {
                Icons.Outlined.WatchLater
            } else {
                Icons.Rounded.WatchLater
            },
            contentDescription = stringResource(
                if (item == null) {
                    RStrings.add_to_read_later
                } else {
                    RStrings.remove_from_read_later
                }
            ),
            tint = tint,
        )
    }
}
