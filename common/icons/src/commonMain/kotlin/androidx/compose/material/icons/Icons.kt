package androidx.compose.material.icons

/**
 * Compatibility API for existing imports. All drawable paths come from Rounded Material Symbols.
 */
object Icons {
    object Filled
    object Outlined
    object Rounded
    object Sharp

    val Default: Filled
        get() = Filled

    object AutoMirrored {
        object Filled
        object Rounded
    }
}
