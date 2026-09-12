package com.flowtap.app.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color

/**
 * Shared design tokens: warm-paper spacing, control sizes, motion, timer style.
 * Mockup rules: 20dp side gutters, 48dp minimum targets, 54-56dp primary
 * buttons, 76dp+ setting rows, 600dp max centered content width.
 */
object Spacing {
    val xxs = 2.dp
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 24.dp
    val xxl = 32.dp
    val screenPadding = 20.dp
    val cardGap = 12.dp
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
    val primaryButtonShape = 17.dp
    val controlMinHeight = 48.dp
    val settingRowMinHeight = 76.dp
    val presetMinHeight = 48.dp
    val cardBorderWidth = 1.dp
    /** Unified hero ring: idle and active Home timers share one spec so sizes can't drift. */
    val heroRing = 240.dp
    val heroRingCompact = 180.dp
    val heroRingHistory = 170.dp
    val heroRingStroke = 12.dp
    val heroRingIdle = heroRing
    val heroRingActive = heroRing
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
