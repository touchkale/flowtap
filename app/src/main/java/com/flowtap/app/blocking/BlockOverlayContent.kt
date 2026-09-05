package com.flowtap.app.blocking

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.flowtap.app.R
import com.flowtap.app.data.model.ActiveSessionSnapshot
import com.flowtap.app.ui.components.AdaptiveTimerText
import com.flowtap.app.ui.components.HoldToEndButton
import com.flowtap.app.ui.theme.Dimens
import com.flowtap.app.ui.theme.LocalFocusPalette
import com.flowtap.app.ui.theme.Spacing
import com.flowtap.app.ui.util.rememberAdaptiveSidePadding
import com.flowtap.app.util.TimeUtil
import kotlinx.coroutines.delay

/**
 * Fullscreen "Deep Work is active" content for the fallback
 * BlockOverlayActivity. Always offers a way out: non-strict sessions get an
 * End session button; strict sessions get an emergency hold-to-end plus a
 * timer button that returns to the app. There is always a visible exit — the
 * overlay can never trap the user.
 */
@Composable
fun BlockOverlayContent(
    snap: ActiveSessionSnapshot,
    onOpenTimer: () -> Unit,
    onEndSession: (() -> Unit)? = null,
    onEmergencyEnd: (() -> Unit)? = null,
) {
    val palette = LocalFocusPalette.current
    val context = LocalContext.current
    var elapsedMs by remember(snap.sessionId) { mutableLongStateOf(snap.elapsedMs()) }
    LaunchedEffect(snap.sessionId) {
        while (true) {
            elapsedMs = snap.elapsedMs()
            delay(1000)
        }
    }
    val duration = TimeUtil.formatClockTime(elapsedMs)
    val sidePadding = rememberAdaptiveSidePadding(maxWidth = 520.dp, minPadding = Spacing.xxl)

    AnimatedVisibility(
        visible = true,
        enter = slideInVertically(tween(250)) { it } + fadeIn(tween(250)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(palette.background)
                .systemBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = sidePadding, vertical = Spacing.xxl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .background(palette.accent.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Outlined.Lock,
                    contentDescription = null,
                    modifier = Modifier.size(44.dp),
                    tint = palette.accent,
                )
            }
            Spacer(Modifier.height(Spacing.xl))
            Text(
                text = stringResource(R.string.overlay_active_x, snap.modeName),
                style = MaterialTheme.typography.headlineLarge,
                color = palette.textPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics { heading() },
            )
            Spacer(Modifier.height(Spacing.md))
            AdaptiveTimerText(
                text = duration,
                color = palette.accent,
                maxWidthFraction = 0.85f,
                modifier = Modifier.semantics {
                    contentDescription = context.getString(
                        R.string.overlay_elapsed_cd_x,
                        duration,
                        snap.modeName,
                    )
                },
            )
            Text(
                text = stringResource(R.string.overlay_elapsed),
                style = MaterialTheme.typography.labelLarge,
                color = palette.textSecondary,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(Spacing.xl))
            Text(
                text = if (snap.strictMode) {
                    stringResource(R.string.overlay_strict_hint, snap.tagName ?: "NFC")
                } else {
                    stringResource(R.string.overlay_flex_hint)
                },
                style = MaterialTheme.typography.bodyLarge,
                color = palette.textSecondary,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(Spacing.xl))
            if (onEndSession != null) {
                Button(
                    onClick = onEndSession,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(Dimens.primaryButtonHeight),
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.buttonColors(containerColor = palette.accent),
                ) {
                    Text(
                        stringResource(R.string.common_end_session),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                Spacer(Modifier.height(Spacing.sm))
            }
            if (onEndSession == null && onEmergencyEnd != null) {
                Text(
                    text = stringResource(R.string.overlay_no_tag),
                    style = MaterialTheme.typography.labelLarge,
                    color = palette.textSecondary,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(Spacing.sm))
                HoldToEndButton(
                    onHoldComplete = onEmergencyEnd,
                    label = stringResource(R.string.hold_emergency_label),
                )
                Spacer(Modifier.height(Spacing.sm))
            }
            TextButton(onClick = onOpenTimer) {
                Text(
                    stringResource(R.string.overlay_show_timer),
                    style = MaterialTheme.typography.titleSmall,
                    color = palette.accentText,
                )
            }
        }
    }
}