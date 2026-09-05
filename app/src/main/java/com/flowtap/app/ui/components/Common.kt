package com.flowtap.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Nfc
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flowtap.app.R
import com.flowtap.app.ui.theme.Dimens
import com.flowtap.app.ui.theme.LocalFocusPalette
import com.flowtap.app.ui.theme.Motion
import com.flowtap.app.ui.theme.Spacing
import com.flowtap.app.ui.theme.TimerTextStyle
import com.flowtap.app.util.AppIcons
import com.flowtap.app.util.Haptics
import com.flowtap.app.util.HoldToEnd
import kotlinx.coroutines.delay

/** Bordered card surface — guarantees separation in light and dark mode. */
@Composable
fun FocusCard(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val palette = LocalFocusPalette.current
    Box(
        modifier = modifier
            .background(palette.card, MaterialTheme.shapes.medium)
            .border(Dimens.cardBorderWidth, palette.cardBorder, MaterialTheme.shapes.medium)
            .clip(MaterialTheme.shapes.medium),
        contentAlignment = Alignment.Center,
        content = content,
    )
}

/** Circular progress ring with animated sweep (FastOutSlowIn). */
@Composable
fun GoalRing(
    progress: Float,
    modifier: Modifier = Modifier,
    size: Dp = 120.dp,
    strokeWidth: Dp = 10.dp,
    content: @Composable BoxScope.() -> Unit = {},
) {
    val palette = LocalFocusPalette.current
    val animated by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(Motion.TimerSweepMs),
        label = "ring",
    )
    Box(modifier.then(Modifier.size(size)), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val strokePx = strokeWidth.toPx()
            val diameterPx = size.toPx()
            val arcSize = androidx.compose.ui.geometry.Size(diameterPx - strokePx, diameterPx - strokePx)
            val offset = androidx.compose.ui.geometry.Offset(strokePx / 2f, strokePx / 2f)
            drawArc(
                color = palette.ringTrack,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = strokePx, cap = StrokeCap.Round),
                topLeft = offset,
                size = arcSize,
            )
            drawArc(
                color = palette.accent,
                startAngle = -90f,
                sweepAngle = 360f * animated,
                useCenter = false,
                style = Stroke(width = strokePx, cap = StrokeCap.Round),
                topLeft = offset,
                size = arcSize,
            )
        }
        content()
    }
}

/** Pulsing NFC icon used on idle home and pairing screens. */
@Composable
fun PulsingNfcIcon(
    modifier: Modifier = Modifier,
    size: Dp = 72.dp,
) {
    val palette = LocalFocusPalette.current
    val transition = rememberInfiniteTransition(label = "nfcPulse")
    val pulse by transition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(Motion.PulseMs / 2, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "nfc",
    )
    Box(
        modifier = modifier.size(size).alpha(pulse).scale(pulse),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.Outlined.Nfc,
            contentDescription = stringResource(R.string.common_nfc_cd),
            modifier = Modifier.size(size),
            tint = palette.accent,
        )
    }
}

/** Small status pill: icon + label. Bordered so it reads on light cards. */
@Composable
fun StatusChip(
    text: String,
    icon: ImageVector? = null,
    modifier: Modifier = Modifier,
) {
    val palette = LocalFocusPalette.current
    Row(
        modifier = modifier
            .background(palette.card, CircleShape)
            .border(Dimens.cardBorderWidth, palette.cardBorder, CircleShape)
            .clip(CircleShape)
            .semantics(mergeDescendants = true) { contentDescription = text }
            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp), tint = palette.accentText)
        }
        Text(text, style = MaterialTheme.typography.labelLarge, color = palette.textSecondary)
    }
}

/** Section header with optional action label. Action is a real TextButton. */
@Composable
fun SectionHeader(
    title: String,
    action: String? = null,
    onAction: (() -> Unit)? = null,
) {
    val palette = LocalFocusPalette.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = palette.textPrimary)
        if (action != null && onAction != null) {
            TextButton(onClick = onAction) {
                Text(
                    action,
                    style = MaterialTheme.typography.labelLarge,
                    color = palette.accentText,
                )
            }
        }
    }
}

/** Animated count-up for a numeric value. */
@Composable
fun CountUpText(
    targetValue: Int,
    style: androidx.compose.ui.text.TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val animated by animateFloatAsState(
        targetValue = targetValue.toFloat(),
        animationSpec = tween(Motion.BaseMs),
        label = "countUp",
    )
    Text(
        text = animated.toInt().toString(),
        style = style,
        color = color,
        modifier = modifier,
        textAlign = TextAlign.Center,
    )
}

/** Big live timer in tabular digits. */
@Composable
fun TimerText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color? = null,
) {
    val palette = LocalFocusPalette.current
    Text(
        text = text,
        style = TimerTextStyle,
        color = color ?: palette.textPrimary,
        modifier = modifier,
        textAlign = TextAlign.Center,
        maxLines = 1,
    )
}

/**
 * Timer text that shrinks until it fits its available width, so values like
 * "00:00:06" or "2h 30m" can never overlap the goal ring — on narrow screens,
 * huge font scales, or long durations. [maxWidthFraction] is relative to the
 * parent width; pick it so the text stays inside the ring.
 */
@Composable
fun AdaptiveTimerText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color? = null,
    baseFontSize: androidx.compose.ui.unit.TextUnit = TimerTextStyle.fontSize,
    minFontSize: androidx.compose.ui.unit.TextUnit = 28.sp,
    maxWidthFraction: Float = 0.55f,
) {
    val palette = LocalFocusPalette.current
    var size by remember(text.length) { mutableStateOf(baseFontSize) }
    Text(
        text = text,
        style = TimerTextStyle.copy(fontSize = size, lineHeight = size * 1.08f),
        color = color ?: palette.textPrimary,
        modifier = modifier.fillMaxWidth(maxWidthFraction),
        textAlign = TextAlign.Center,
        maxLines = 1,
        overflow = TextOverflow.Clip,
        onTextLayout = { result ->
            if (result.hasVisualOverflow && size > minFontSize) {
                size = (size.value - 2f).coerceAtLeast(minFontSize.value).sp
            }
        },
    )
}

/**
 * Emergency exit: press-and-hold for [holdMs] to end a session that cannot be
 * ended any other way (strict mode without the tag at hand). Fills with color
 * while held, ticks haptically each second, and resets if released early — so
 * it can never fire accidentally, but always offers a way out.
 */
@Composable
fun HoldToEndButton(
    onHoldComplete: () -> Unit,
    modifier: Modifier = Modifier,
    holdMs: Long = HoldToEnd.HOLD_MS,
    label: String = stringResource(R.string.hold_label),
) {
    val palette = LocalFocusPalette.current
    val context = LocalContext.current
    var holding by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    val remainSecs = HoldToEnd.remainSecs((progress * holdMs).toLong())

    LaunchedEffect(holding) {
        if (!holding) {
            progress = 0f
            return@LaunchedEffect
        }
        runCatching { Haptics.tick(context) }
        repeat(HoldToEnd.STEPS) { i ->
            delay(HoldToEnd.stepDelayMs())
            progress = (i + 1) / HoldToEnd.STEPS.toFloat()
            if ((i + 1) % 10 == 0) runCatching { Haptics.tick(context) }
        }
        runCatching { Haptics.sessionEnd(context) }
        onHoldComplete()
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(Dimens.primaryButtonHeight)
            .background(palette.ringTrack, MaterialTheme.shapes.medium)
            .border(Dimens.cardBorderWidth, palette.cardBorder, MaterialTheme.shapes.medium)
            .clip(MaterialTheme.shapes.medium)
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown()
                    holding = true
                    waitForUpOrCancellation()
                    holding = false
                }
            }
            .semantics {
                contentDescription = context.getString(R.string.hold_content_desc)
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .fillMaxHeight()
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .background(palette.danger.copy(alpha = 0.55f)),
        )
        Text(
            text = if (holding) {
                stringResource(R.string.hold_keep_x, remainSecs)
            } else {
                label
            },
            style = MaterialTheme.typography.titleMedium,
            color = palette.textPrimary,
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * Real app logo in a rounded square, with a letter-avatar fallback.
 * The icon loads off the main thread and is memory-cached; uninstalled or
 * icon-less packages permanently show the fallback initial.
 */
@Composable
fun AppIcon(
    pkg: String,
    label: String,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    corner: Dp = 12.dp,
) {
    val palette = LocalFocusPalette.current
    val context = LocalContext.current
    val density = LocalDensity.current
    val shape = RoundedCornerShape(corner)
    var bitmap by remember(pkg) { mutableStateOf<ImageBitmap?>(AppIcons.cached(pkg)) }
    LaunchedEffect(pkg) {
        if (bitmap == null) {
            bitmap = AppIcons.load(context.applicationContext, pkg, with(density) { size.roundToPx() })
        }
    }
    val bmp = bitmap
    if (bmp != null) {
        Image(
            bitmap = bmp,
            contentDescription = label,
            modifier = modifier
                .size(size)
                .border(Dimens.cardBorderWidth, palette.cardBorder, shape)
                .clip(shape),
            contentScale = ContentScale.Crop,
        )
    } else {
        Box(
            modifier = modifier
                .size(size)
                .background(palette.ringTrack, shape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                label.take(1).uppercase(),
                style = MaterialTheme.typography.titleMedium,
                color = palette.accentText,
            )
        }
    }
}

/**
 * One-tap card surfacing installed social/distracting apps worth blocking.
 * Shown only while [unselectedCount] > 0; disappears once all are selected.
 */
@Composable
fun RecommendedBlockCard(
    unselectedCount: Int,
    onBlockAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalFocusPalette.current
    FocusCard(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(Spacing.cardPadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Outlined.Block,
                contentDescription = null,
                tint = palette.accent,
                modifier = Modifier.size(22.dp),
            )
            Spacer(Modifier.size(Spacing.md))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.rec_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = palette.textPrimary,
                )
                Spacer(Modifier.height(Spacing.xxs))
                Text(
                    stringResource(R.string.rec_sub),
                    style = MaterialTheme.typography.labelLarge,
                    color = palette.textSecondary,
                )
            }
            Spacer(Modifier.size(Spacing.sm))
            Button(
                onClick = onBlockAll,
                colors = ButtonDefaults.buttonColors(containerColor = palette.accent),
            ) {
                Text(
                    stringResource(R.string.rec_block_all_x, unselectedCount),
                    maxLines = 1,
                )
            }
        }
    }
}

/** Empty state with icon and message. Set [pulsing] only for hero states. */
@Composable
fun EmptyState(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    pulsing: Boolean = false,
) {
    val palette = LocalFocusPalette.current
    Column(
        modifier = modifier.fillMaxWidth().padding(Spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        if (pulsing) {
            PulsingNfcIcon(size = 56.dp)
        } else {
            Icon(
                Icons.Outlined.Nfc,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = palette.textTertiary,
            )
        }
        Spacer(Modifier.height(Spacing.xs))
        Text(title, style = MaterialTheme.typography.titleMedium, color = palette.textPrimary, textAlign = TextAlign.Center)
        if (subtitle != null) {
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = palette.textSecondary, textAlign = TextAlign.Center)
        }
    }
}
