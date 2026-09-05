package com.flowtap.app.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.flowtap.app.ui.theme.Spacing

/**
 * P1 adaptive layouts. Phones keep edge-to-edge content; wide screens
 * (landscape, tablets, foldables) cap content width and center it via
 * symmetric side padding, and short screens shrink hero visuals.
 */
object Adaptive {
    val MaxContentWidth: Dp = 600.dp
    const val CompactHeightDp = 480
}

/** Side padding that centers content up to [Adaptive.MaxContentWidth]. */
@Composable
fun rememberAdaptiveSidePadding(
    maxWidth: Dp = Adaptive.MaxContentWidth,
    minPadding: Dp = Spacing.screenPadding,
): Dp {
    val screenWidth = with(LocalDensity.current) {
        LocalConfiguration.current.screenWidthDp.dp
    }
    return ((screenWidth - maxWidth) / 2).coerceAtLeast(minPadding)
}

/** True on landscape phones / short windows where heroes should shrink. */
@Composable
fun rememberIsCompactHeight(): Boolean =
    LocalConfiguration.current.screenHeightDp < Adaptive.CompactHeightDp
