package com.mrl.pixiv.common.compose.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.expressiveLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.graphics.Color
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.material3.dynamicColorScheme
import com.mrl.pixiv.common.data.setting.AppPaletteStyle
import com.mrl.pixiv.common.data.setting.AppearanceSettings
import com.mrl.pixiv.common.data.setting.ResolvedThemeColor
import com.mrl.pixiv.common.data.setting.resolve
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.milliseconds

/** 读取平台的壁纸配色，不支持时返回空值。 */
@Composable
expect fun platformDynamicColorScheme(darkTheme: Boolean): ColorScheme?

/** 不用种子色时的默认配色。 */
fun baselineColorScheme(darkTheme: Boolean): ColorScheme =
    if (darkTheme) darkColorScheme() else expressiveLightColorScheme()

/** 应用主题和样机共用同一套配色解析及纯黑背景规则。 */
@Composable
fun rememberAppColorScheme(appearance: AppearanceSettings, darkTheme: Boolean): ColorScheme {
    val request = AppColorSchemeRequest(appearance, darkTheme, platformDynamicColorScheme(darkTheme))
    return remember(request) { request.build() }
}

/**
 * 与 [rememberAppColorScheme] 规则相同，但种子色配色在后台生成，供外观页的样机预览。
 *
 * [appearance] 在快照中读取，取色时连续变化也不会让调用方重组；只处理最新一次输入，生成完成前
 * 沿用上一份配色，最多每 [BackgroundSchemeIntervalMillis] 更新一次。首次组合时缓存里没有，
 * 就先显示 [placeholder]。
 */
@Composable
fun rememberAppColorSchemeState(
    darkTheme: Boolean,
    placeholder: () -> ColorScheme,
    appearance: () -> AppearanceSettings,
): State<ColorScheme> {
    val platformScheme = platformDynamicColorScheme(darkTheme)
    val latestAppearance by rememberUpdatedState(appearance)
    val initial = remember {
        val request = Snapshot.withoutReadObservation {
            AppColorSchemeRequest(appearance(), darkTheme, platformScheme)
        }
        request to request.cachedOrNull()
    }
    val scheme = remember { mutableStateOf(initial.second ?: placeholder()) }
    LaunchedEffect(darkTheme, platformScheme) {
        var shown = initial.first.takeIf { initial.second != null }
        snapshotFlow { AppColorSchemeRequest(latestAppearance(), darkTheme, platformScheme) }
            .filter { it != shown }
            .conflate()
            .collect { request ->
                scheme.value = request.buildInBackground()
                shown = request
                delay(BackgroundSchemeIntervalMillis.milliseconds)
            }
    }
    return scheme
}

/** 后台配色的最短更新间隔，约 30 帧每秒，足够连续预览。 */
private const val BackgroundSchemeIntervalMillis = 32L

/** 种子色配色，缓存里没有时在后台生成；生成完成前返回空。 */
@Composable
fun rememberSeededColorScheme(seed: Long, style: AppPaletteStyle?, darkTheme: Boolean): ColorScheme? {
    val key = SeededSchemeKey(seed, style, darkTheme)
    return produceState(initialValue = remember(key) { SeededSchemeCache[key] }, key) {
        value = seededColorSchemeInBackground(key)
    }.value
}

fun previewColorScheme(seed: Long, style: AppPaletteStyle?, darkTheme: Boolean): ColorScheme =
    dynamicColorScheme(
        seedColor = Color(seed),
        isDark = darkTheme,
        isAmoled = false,
        style = when (style) {
            AppPaletteStyle.TONAL_SPOT -> PaletteStyle.TonalSpot
            AppPaletteStyle.NEUTRAL -> PaletteStyle.Neutral
            AppPaletteStyle.FIDELITY -> PaletteStyle.Fidelity
            null -> PaletteStyle.Monochrome
        },
        specVersion = ColorSpec.SpecVersion.SPEC_2025,
    )

/** 生成一份应用配色所需的全部输入，相等时生成结果也相同。 */
private data class AppColorSchemeRequest(
    val resolved: ResolvedThemeColor,
    val darkTheme: Boolean,
    val amoledDark: Boolean,
    val platformScheme: ColorScheme?,
) {
    constructor(appearance: AppearanceSettings, darkTheme: Boolean, platformScheme: ColorScheme?) : this(
        resolved = appearance.resolve(dynamicAvailable = platformScheme != null),
        darkTheme = darkTheme,
        amoledDark = appearance.amoledDark,
        platformScheme = platformScheme,
    )

    /** 在主线程同步生成。 */
    fun build(): ColorScheme = finish(
        when (resolved) {
            is ResolvedThemeColor.Seeded -> seededColorScheme(SeededSchemeKey(resolved, darkTheme))
            else -> unseededBase()
        }
    )

    /** 不需要种子色或缓存里已有时直接给出配色，否则返回空。 */
    fun cachedOrNull(): ColorScheme? = when (resolved) {
        is ResolvedThemeColor.Seeded -> SeededSchemeCache[SeededSchemeKey(resolved, darkTheme)]?.let(::finish)
        else -> finish(unseededBase())
    }

    /** 种子色配色放到后台生成，其余直接给出。 */
    suspend fun buildInBackground(): ColorScheme = when (resolved) {
        is ResolvedThemeColor.Seeded -> finish(seededColorSchemeInBackground(SeededSchemeKey(resolved, darkTheme)))
        else -> finish(unseededBase())
    }

    private fun unseededBase(): ColorScheme = when (resolved) {
        ResolvedThemeColor.Dynamic -> requireNotNull(platformScheme)
        else -> baselineColorScheme(darkTheme)
    }

    private fun finish(scheme: ColorScheme): ColorScheme = if (darkTheme && amoledDark) scheme.toAmoled() else scheme
}

private data class SeededSchemeKey(val seed: Long, val style: AppPaletteStyle?, val darkTheme: Boolean) {
    constructor(resolved: ResolvedThemeColor.Seeded, darkTheme: Boolean) : this(resolved.seed, resolved.style, darkTheme)

    fun build(): ColorScheme = previewColorScheme(seed, style, darkTheme)
}

/**
 * 种子色配色的缓存，同一种子色、风格和深浅只生成一次。
 *
 * 色板预先生成的配色在选中后可以直接给应用主题用；取色器最后预览的配色在应用时也能直接命中。
 * 只在主线程（组合及其协程）读写，生成本身可以放到后台。
 */
private object SeededSchemeCache {
    private const val MAX_SIZE = 64
    private val schemes = LinkedHashMap<SeededSchemeKey, ColorScheme>()

    operator fun get(key: SeededSchemeKey): ColorScheme? = schemes.remove(key)?.also { schemes[key] = it }

    operator fun set(key: SeededSchemeKey, scheme: ColorScheme) {
        schemes.remove(key)
        schemes[key] = scheme
        if (schemes.size > MAX_SIZE) schemes.remove(schemes.keys.first())
    }
}

private fun seededColorScheme(key: SeededSchemeKey): ColorScheme =
    SeededSchemeCache[key] ?: key.build().also { SeededSchemeCache[key] = it }

/** 在后台生成，回到调用方的主线程协程后再写入缓存。 */
private suspend fun seededColorSchemeInBackground(key: SeededSchemeKey): ColorScheme =
    SeededSchemeCache[key] ?: withContext(Dispatchers.Default) { key.build() }.also { SeededSchemeCache[key] = it }

private fun ColorScheme.toAmoled(): ColorScheme = copy(
    background = Color.Black,
    surface = Color.Black,
    surfaceDim = Color.Black,
    surfaceContainerLowest = Color.Black,
)
