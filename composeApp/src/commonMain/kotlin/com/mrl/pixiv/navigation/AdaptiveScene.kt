package com.mrl.pixiv.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavMetadataKey
import androidx.navigation3.runtime.get
import androidx.navigation3.scene.Scene
import androidx.navigation3.scene.SceneStrategy
import androidx.navigation3.scene.SceneStrategyScope
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import androidx.navigation3.ui.NavDisplay
import androidx.navigationevent.NavigationEvent
import com.mrl.pixiv.common.compose.layout.PaneHost
import com.mrl.pixiv.common.compose.layout.PaneInputState
import com.mrl.pixiv.common.compose.layout.PaneRole
import com.mrl.pixiv.common.compose.layout.ResizableSplitLayout
import com.mrl.pixiv.common.compose.layout.SplitPaneDividerWidth
import com.mrl.pixiv.common.compose.layout.SplitPaneState
import com.mrl.pixiv.common.router.NavigationRecord
import com.mrl.pixiv.common.router.paneSpec
import com.mrl.pixiv.common.util.isAndroid
import com.mrl.pixiv.common.util.platform

internal object NavigationRecordKey : NavMetadataKey<NavigationRecord>

internal val NavEntry<NavigationRecord>.record: NavigationRecord
    get() = checkNotNull(metadata[NavigationRecordKey])

/**
 * 场景过渡回调通过公开 API 提供导航起点和终点。
 * 将这段仅用于界面的过渡状态与条目标识分开，并在各种返回方式下更新。
 */
@Stable
internal class AdaptivePaneTransitionState(
    private val enablePredictiveBackCard: Boolean = platform.isAndroid(),
) {
    private var segment by mutableStateOf<PaneTransitionSegment?>(null)
    private var backCard by mutableStateOf<PredictiveBackCardTransition?>(null)

    fun update(
        initial: AdaptiveScene?,
        target: AdaptiveScene?,
        kind: TransitionKind = TransitionKind.Forward,
        swipeEdge: Int = NavigationEvent.EDGE_NONE,
    ) {
        segment = PaneTransitionSegment(
            initialKey = initial?.key,
            targetKey = target?.key,
            keepsSamePage = initial != null && target != null &&
                initial.top.contentKey == target.top.contentKey,
        )
        // NavDisplay 可能在切换到上一场景之前查询动画参数。
        // 手势状态应在场景移除时清理，不能因空闲状态下的参数查询而提前清除。
        if (initial?.key == target?.key) return
        if (!enablePredictiveBackCard || initial == null || target == null ||
            initial.source != null || target.source != null
        ) {
            backCard = null
            return
        }
        if (kind == TransitionKind.Predictive) {
            if (backCard?.matches(initial.key, target.key) != true || backCard?.swipeEdge != swipeEdge) {
                backCard = PredictiveBackCardTransition(initial.key, target.key, swipeEdge)
            }
        } else if (backCard?.matches(initial.key, target.key) != true) {
            backCard = null
        }
    }

    fun backCardFor(scene: AdaptiveScene): PredictiveBackCardTransition? =
        backCard?.takeIf { it.contains(scene.key) }

    fun finishBackCard(transition: PredictiveBackCardTransition) {
        // 被中断手势的场景移除时，不能清除后续手势的状态。
        if (backCard === transition) backCard = null
    }

    fun suppressesAnimationFor(scene: AdaptiveScene): Boolean = segment?.let {
        it.keepsSamePage && (scene.key == it.initialKey || scene.key == it.targetKey)
    } ?: false

}

private data class PaneTransitionSegment(
    val initialKey: Any?,
    val targetKey: Any?,
    val keepsSamePage: Boolean,
)

/** 此策略也负责单栏展示，使窗口尺寸变化前后使用一致的过渡规则。 */
internal class AdaptiveSceneStrategy(
    private val availableWidth: Dp,
    private val availableHeight: Dp,
    private val splitState: SplitPaneState,
    private val inputState: PaneInputState,
    private val paneTransitionState: AdaptivePaneTransitionState,
    private val allowSplit: Boolean = true,
) : SceneStrategy<NavigationRecord> {
    override fun SceneStrategyScope<NavigationRecord>.calculateScene(
        entries: List<NavEntry<NavigationRecord>>,
    ): Scene<NavigationRecord>? {
        val top = entries.lastOrNull() ?: return null
        val sourceIndex = entries.indexOfLast { it.record.entryId == top.record.ownerEntryId }
        val source = entries.getOrNull(sourceIndex)?.takeIf { candidate ->
            val sourceSpec = candidate.record.destination.paneSpec
            val detailSpec = top.record.destination.paneSpec
            allowSplit && availableHeight >= 480.dp &&
                sourceSpec.canHostDetail && detailSpec.canShowAsDetail &&
                !detailSpec.preferredFullWidth &&
                entries.subList(sourceIndex + 1, entries.size).all {
                    it.record.ownerEntryId == candidate.record.entryId
                } &&
                availableWidth >= sourceSpec.minSourceWidth +
                    SplitPaneDividerWidth + detailSpec.minDetailWidth
        }
        return AdaptiveScene(
            previousEntries = entries.dropLast(1),
            source = source,
            top = top,
            splitState = splitState,
            inputState = inputState,
            paneTransitionState = paneTransitionState,
        )
    }
}

internal data class AdaptiveScene(
    override val previousEntries: List<NavEntry<NavigationRecord>>,
    val source: NavEntry<NavigationRecord>?,
    val top: NavEntry<NavigationRecord>,
    val splitState: SplitPaneState,
    val inputState: PaneInputState,
    val paneTransitionState: AdaptivePaneTransitionState,
) : Scene<NavigationRecord> {
    override val key: Any = Pair(source?.contentKey, top.contentKey)
    override val entries = listOfNotNull(source, top)
    override val metadata =
        NavDisplay.transitionSpec { adaptiveTransform(TransitionKind.Forward, paneTransitionState) } +
            NavDisplay.popTransitionSpec { adaptiveTransform(TransitionKind.Back, paneTransitionState) } +
            NavDisplay.predictivePopTransitionSpec { edge ->
                adaptiveTransform(TransitionKind.Predictive, paneTransitionState, edge)
            }

    override val content: @Composable () -> Unit = {
        if (source == null) {
            PredictiveBackCard(
                sceneKey = key,
                card = paneTransitionState.backCardFor(this),
                onFinished = paneTransitionState::finishBackCard,
            ) {
                PaneHost(PaneRole.Single, isSplit = false) { top.Content() }
            }
        } else {
            val scope = LocalNavAnimatedContentScope.current
            ResizableSplitLayout(
                state = splitState,
                minSourceWidth = source.record.destination.paneSpec.minSourceWidth,
                minDetailWidth = top.record.destination.paneSpec.minDetailWidth,
                onDividerFocusChanged = { inputState.dividerFocused = it },
                source = {
                    PaneHost(PaneRole.Source, isSplit = true) { source.Content() }
                },
                detail = {
                    val suppressAnimation = paneTransitionState.suppressesAnimationFor(this)
                    PaneHost(
                        PaneRole.Detail,
                        isSplit = true,
                        modifier = with(scope) {
                            Modifier.fillMaxSize().animateEnterExit(
                                enter = if (suppressAnimation) EnterTransition.None else {
                                    fadeIn(tween(200)) + slideInHorizontally(tween(220)) { it / 16 }
                                },
                                // 返回手势推进时，详情面板保持不透明。
                                exit = ExitTransition.None,
                            )
                        },
                    ) { top.Content() }
                },
            )
        }
    }
}

internal enum class TransitionKind { Forward, Back, Predictive }

private fun AnimatedContentTransitionScope<Scene<*>>.adaptiveTransform(
    kind: TransitionKind,
    paneTransitionState: AdaptivePaneTransitionState,
    swipeEdge: Int = 0,
): ContentTransform {
    val initial = initialState as? AdaptiveScene
    val target = targetState as? AdaptiveScene
    paneTransitionState.update(initial, target, kind, swipeEdge)
    if (initial != null && target != null) {
        val samePage = initial.top.contentKey == target.top.contentKey
        val sameSource = initial.source != null &&
            initial.source.contentKey == target.source?.contentKey
        val openingSidePage = target.source?.contentKey == initial.top.contentKey
        val closingSidePage = initial.source?.contentKey == target.top.contentKey
        if (samePage || sameSource || openingSidePage || closingSidePage) {
            return EnterTransition.None togetherWith ExitTransition.None
        }
    }
    // Nav3 在确认或取消手势后的收尾阶段可能切回普通返回或前进动画回调，
    // 此时仍需保持相同的卡片变换，并优先于 Picture 和 ImagePreview 的条目动画配置。
    if (initial != null && paneTransitionState.backCardFor(initial) != null) {
        return EnterTransition.None togetherWith ExitTransition.None
    }
    // 即使从侧栏打开，也保留 Picture 和 ImagePreview 的整页内容过渡动画。
    val navigatingEntry = if (kind == TransitionKind.Forward) target?.top else initial?.top
    val entryMetadata = navigatingEntry?.metadata
    val entryTransform = when (kind) {
        TransitionKind.Forward -> entryMetadata?.get(NavDisplay.TransitionKey)?.invoke(this)
        TransitionKind.Back -> entryMetadata?.get(NavDisplay.PopTransitionKey)?.invoke(this)
        TransitionKind.Predictive ->
            entryMetadata?.get(NavDisplay.PredictivePopTransitionKey)?.invoke(this, swipeEdge)
    }
    return entryTransform ?: (fadeIn(tween(220)) togetherWith fadeOut(tween(140)))
}
