package com.flowtap.app.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color

/**
 * Shared design tokens: spacing, motion durations, easing, and the timer text style.
 */
object Spacing {
    val xxs = 2.dp
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 24.dp
    val xxl = 32.dp
    val screenPadding = 16.dp
    val cardGap = 12.dp
    // Production polish tokens — use everywhere, no hardcoded dp.
    val screenTop = 16.dp
    val sectionGap = 20.dp
    val headerGap = 8.dp
    val rowGap = 8.dp
    val cardPadding = 16.dp
    val cardPaddingLarge = 24.dp
    val listBottom = 24.dp
}

object Dimens {
    val dialogMaxHeight = 280.dp
    val primaryButtonHeight = 56.dp
    val presetMinHeight = 48.dp
    val cardBorderWidth = 1.dp
    val heroRingIdle = 160.dp
    val heroRingActive = 260.dp
    val heroRingHistory = 170.dp
}

object Motion {
    const val FastMs = 160
    const val BaseMs = 300
    const val SlowMs = 500
    const val TimerSweepMs = 800
    const val PulseMs = 1600
}

/** Live timer text: bold tabular digits sized to fit inside the goal ring. */
val TimerTextStyle = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontWeight = FontWeight.Bold,
    fontSize = 52.sp,
    lineHeight = 56.sp,
    fontFeatureSettings = "tnum",
)
