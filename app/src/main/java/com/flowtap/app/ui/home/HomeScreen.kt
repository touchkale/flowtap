package com.flowtap.app.ui.home

import android.content.Context
import android.content.Intent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.outlined.Nfc
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.flowtap.app.R
import com.flowtap.app.data.model.ActiveSessionSnapshot
import com.flowtap.app.data.model.FocusMode
import com.flowtap.app.data.model.SessionRecord
import com.flowtap.app.di.AppContainer
import com.flowtap.app.nfc.PairTagActivity
import com.flowtap.app.session.SessionManager
import com.flowtap.app.ui.components.AdaptiveTimerText
import com.flowtap.app.ui.components.AppIcon
import com.flowtap.app.ui.components.EmptyState
import com.flowtap.app.ui.components.FocusCard
import com.flowtap.app.ui.components.GoalRing
import com.flowtap.app.ui.components.HoldToEndButton
import com.flowtap.app.ui.components.SectionHeader
import com.flowtap.app.ui.components.StatusChip
import com.flowtap.app.ui.onboarding.Permissions
import com.flowtap.app.ui.theme.Dimens
import com.flowtap.app.ui.theme.LocalFocusPalette
import com.flowtap.app.ui.theme.Spacing
import com.flowtap.app.ui.util.rememberAdaptiveSidePadding
import com.flowtap.app.ui.util.rememberIsCompactHeight
import com.flowtap.app.ui.util.rememberResumeTick
import com.flowtap.app.util.AppLabels
import com.flowtap.app.util.TimeUtil
import kotlinx.coroutines.delay

@Composable
fun HomeScreen(
    container: AppContainer,
    onOpenHistory: () -> Unit,
) {
    val context = LocalContext.current
    val vm: HomeViewModel = viewModel(factory = HomeViewModel.factory(container))
    val palette = LocalFocusPalette.current

    val todayTotal by vm.todayTotalMs.collectAsStateWithLifecycle()
    val dailyGoal by vm.dailyGoalMs.collectAsStateWithLifecycle()
    val hardcore by vm.hardcoreMode.collectAsStateWithLifecycle()
    val recent by vm.recent.collectAsStateWithLifecycle()
    val modes by vm.modes.collectAsStateWithLifecycle()
    val softStartMode by vm.softStartMode.collectAsStateWithLifecycle()
    val modeNames by vm.modeNames.collectAsStateWithLifecycle()
    val active by SessionManager.active.collectAsStateWithLifecycle()

    // F12: NFC-off hint, refreshed whenever returning from system screens.
    val nfcTick = rememberResumeTick()
    val nfcOff = remember(nfcTick) {
        Permissions.hasNfcHardware(context) && !Permissions.isNfcEnabled(context)
    }

    fun onToggleDnd() {
        vm.toggleModeDnd { result ->
            if (result == HomeViewModel.DndToggleResult.ENABLED &&
                !Permissions.isDndGranted(context)
            ) {
                Permissions.enableDnd(context)
            }
        }
    }

    val sidePadding = rememberAdaptiveSidePadding()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = sidePadding),
    ) {
        Spacer(Modifier.height(Spacing.screenTop))
        Text(
            stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineMedium,
            color = palette.textPrimary,
        )
        Spacer(Modifier.height(Spacing.xs))
        Text(
            stringResource(R.string.app_tagline),
            style = MaterialTheme.typography.bodyMedium,
            color = palette.textSecondary,
        )
        Spacer(Modifier.height(Spacing.md))

        if (nfcOff) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(palette.card, MaterialTheme.shapes.medium)
                    .border(Dimens.cardBorderWidth, palette.cardBorder, MaterialTheme.shapes.medium)
                    .clip(MaterialTheme.shapes.medium)
                    .clickable { Permissions.enableNfc(context) }
                    .padding(horizontal = Spacing.cardPadding, vertical = Spacing.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Outlined.Nfc,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = palette.accentText,
                )
                Spacer(Modifier.size(Spacing.sm))
                Text(
                    stringResource(R.string.home_nfc_off),
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.textPrimary,
                )
            }
            Spacer(Modifier.height(Spacing.rowGap))
        }

        if (active != null) {
            ActiveSessionView(
                snap = active!!,
                hardcore = hardcore,
                context = context,
                onEnd = { vm.endFocus() },
                onEmergencyEnd = { vm.endFocusEmergency() },
                onToggleDnd = ::onToggleDnd,
                todayTotalMs = todayTotal,
                dailyGoalMs = dailyGoal,
            )
        } else {
            IdleView(
                todayTotal = todayTotal,
                dailyGoal = dailyGoal,
                recent = recent,
                modeNames = modeNames,
                defaultMode = modes.firstOrNull(),
                softStartMode = softStartMode,
                modesEmpty = modes.isEmpty(),
                onStart = { vm.startFocusDefault() },
                onToggleDnd = ::onToggleDnd,
                onPairTag = {
                    context.startActivity(Intent(context, PairTagActivity::class.java))
                },
                onOpenHistory = onOpenHistory,
            )
        }
        Spacer(Modifier.height(Spacing.listBottom))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun IdleView(
    todayTotal: Long,
    dailyGoal: Long,
    recent: List<SessionRecord>,
    modeNames: Map<Long, String>,
    defaultMode: FocusMode?,
    softStartMode: FocusMode?,
    modesEmpty: Boolean,
    onStart: () -> Unit,
    onToggleDnd: () -> Unit,
    onPairTag: () -> Unit,
    onOpenHistory: () -> Unit,
) {
    val palette = LocalFocusPalette.current

    if (modesEmpty) {
        // F12: no modes yet — guide the user to pairing instead of a dead button.
        Spacer(Modifier.height(Spacing.lg))
        EmptyState(
            title = stringResource(R.string.home_no_modes_title),
            subtitle = stringResource(R.string.home_no_modes_subtitle),
            pulsing = true,
        )
        Spacer(Modifier.height(Spacing.lg))
        Button(
            onClick = onPairTag,
            modifier = Modifier
                .fillMaxWidth()
                .height(Dimens.primaryButtonHeight),
            shape = MaterialTheme.shapes.medium,
            colors = ButtonDefaults.buttonColors(containerColor = palette.accent),
        ) {
            Icon(Icons.Outlined.Nfc, contentDescription = null)
            Spacer(Modifier.size(Spacing.sm))
            Text(stringResource(R.string.home_pair_tag), style = MaterialTheme.typography.titleMedium)
        }
        return
    }

    val progress = if (dailyGoal > 0) todayTotal.toFloat() / dailyGoal else 0f
    val percent = (progress * 100).toInt().coerceIn(0, 100)
    val remainingMs = (dailyGoal - todayTotal).coerceAtLeast(0L)

    FocusCard(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.cardPaddingLarge),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                Text(
                    stringResource(R.string.home_todays_focus),
                    style = MaterialTheme.typography.labelLarge,
                    color = palette.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                StatusChip(
                    text = if (percent >= 100) {
                        stringResource(R.string.home_goal_reached)
                    } else {
                        stringResource(R.string.home_percent_of_goal, percent)
                    },
                )
            }
            Spacer(Modifier.height(Spacing.md))
            GoalRing(progress = progress, size = Dimens.heroRingIdle, strokeWidth = 12.dp) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    AnimatedContent(
                        targetState = TimeUtil.formatHoursMinutes(todayTotal),
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "todayTotal",
                    ) { value ->
                        AdaptiveTimerText(
                            text = value,
                            color = palette.textPrimary,
                            baseFontSize = MaterialTheme.typography.displayMedium.fontSize,
                            maxWidthFraction = 0.52f,
                        )
                    }
                    Spacer(Modifier.height(Spacing.xs))
                    Text(
                        stringResource(R.string.home_deep_work_today),
                        style = MaterialTheme.typography.labelLarge,
                        color = palette.textSecondary,
                    )
                    Text(
                        stringResource(R.string.common_goal_x, TimeUtil.formatGoal(dailyGoal / 60_000)),
                        style = MaterialTheme.typography.labelLarge,
                        color = palette.accentText,
                    )
                }
            }
            Spacer(Modifier.height(Spacing.md))
            Text(
                text = if (percent >= 100) {
                    stringResource(R.string.home_goal_complete)
                } else {
                    stringResource(R.string.home_remaining_x, TimeUtil.formatHoursMinutes(remainingMs))
                },
                style = MaterialTheme.typography.labelLarge,
                color = palette.textTertiary,
                textAlign = TextAlign.Center,
            )
        }
    }

    Spacer(Modifier.height(Spacing.lg))

    Button(
        onClick = onStart,
        enabled = softStartMode != null,
        modifier = Modifier
            .fillMaxWidth()
            .height(Dimens.primaryButtonHeight),
        shape = MaterialTheme.shapes.medium,
        colors = ButtonDefaults.buttonColors(containerColor = palette.accent),
    ) {
        Icon(Icons.Outlined.Nfc, contentDescription = null)
        Spacer(Modifier.size(Spacing.sm))
        Text(stringResource(R.string.home_start), style = MaterialTheme.typography.titleMedium)
    }
    Spacer(Modifier.height(Spacing.sm))
    Text(
        text = if (softStartMode == null) {
            stringResource(R.string.home_strict_only_hint)
        } else {
            stringResource(R.string.home_soft_hint)
        },
        style = MaterialTheme.typography.labelLarge,
        color = palette.textTertiary,
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Center,
    )

    Spacer(Modifier.height(Spacing.lg))

    @OptIn(ExperimentalLayoutApi::class)
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        val mode = defaultMode
        StatusChip(
            text = if (mode?.strictMode == true) {
                stringResource(R.string.home_strict_on)
            } else {
                stringResource(R.string.home_strict_off)
            },
            icon = Icons.Outlined.Lock,
        )
        if (mode?.allowlistOnly == true) {
            StatusChip(
                text = stringResource(
                    R.string.modes_chip_allowlist_x,
                    mode.allowedApps.size,
                ),
                icon = Icons.Outlined.Checklist,
            )
        } else {
            val blockedCount = mode?.blockedApps?.size ?: 0
            StatusChip(
                text = pluralStringResource(R.plurals.plural_apps_blocked, blockedCount, blockedCount),
                icon = Icons.Outlined.Block,
            )
        }
        mode?.autoEndMinutes?.let {
            StatusChip(
                text = stringResource(R.string.modes_chip_auto_x, formatAutoEndShort(it)),
                icon = Icons.Outlined.Timer,
            )
        }
        if (mode?.keepAwake == true) {
            StatusChip(text = stringResource(R.string.modes_chip_awake))
        }
        Box(
            modifier = Modifier.toggleable(
                value = mode?.dndEnabled == true,
                role = Role.Switch,
                onValueChange = { onToggleDnd() },
            ),
        ) {
            StatusChip(
                text = if (mode?.dndEnabled == true) {
                    stringResource(R.string.home_dnd_on)
                } else {
                    stringResource(R.string.home_dnd_off)
                },
                icon = Icons.Outlined.NotificationsOff,
            )
        }
    }

    Spacer(Modifier.height(Spacing.xl))

    SectionHeader(
        title = stringResource(R.string.home_recent),
        action = if (recent.isNotEmpty()) stringResource(R.string.home_view_all) else null,
        onAction = onOpenHistory,
    )
    Spacer(Modifier.height(Spacing.headerGap))

    if (recent.isEmpty()) {
        Text(
            stringResource(R.string.home_no_sessions),
            style = MaterialTheme.typography.bodyMedium,
            color = palette.textSecondary,
        )
    } else {
        recent.forEach { session ->
            SessionRow(session, modeNames)
            Spacer(Modifier.height(Spacing.rowGap))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ActiveSessionView(
    snap: ActiveSessionSnapshot,
    hardcore: Boolean,
    context: android.content.Context,
    onEnd: () -> Unit,
    onEmergencyEnd: () -> Unit,
    onToggleDnd: () -> Unit,
    todayTotalMs: Long,
    dailyGoalMs: Long,
) {
    val palette = LocalFocusPalette.current
    var elapsedMs by remember(snap.sessionId) { mutableLongStateOf(snap.elapsedMs()) }
    LaunchedEffect(snap.sessionId) {
        while (true) {
            elapsedMs = snap.elapsedMs()
            delay(1000)
        }
    }

    // Hardcore mode: the session cannot be ended from the app at all.
    val endLocked = snap.strictMode || hardcore

    // Home always shows TODAY's cumulative time: a new session started later the
    // same day continues from today's total instead of resetting to zero.
    // Live sum (not a captured base): todayTotalMs only counts completed
    // sessions — the running one isn't persisted until it ends — so there is
    // no double count, and a cold start can never freeze a stale zero.
    // Overnight sessions are capped at today's slice via the session start.
    val nowMs = System.currentTimeMillis()
    val elapsedToday = (nowMs - maxOf(snap.startedAtEpoch, TimeUtil.localDayStart(nowMs)))
        .coerceAtLeast(0L)
    val dayTotal = todayTotalMs + elapsedToday
    val dayProgress = if (dailyGoalMs > 0) dayTotal.toFloat() / dailyGoalMs else 0f
    val dayLabel = TimeUtil.formatHoursMinutes(dayTotal)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.semantics {
            contentDescription = context.getString(R.string.home_session_active)
        },
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(palette.accent, CircleShape),
        )
        Spacer(Modifier.size(Spacing.sm))
        Text(
            stringResource(R.string.home_active),
            style = MaterialTheme.typography.labelLarge,
            color = palette.accentText,
        )
    }

    Spacer(Modifier.height(Spacing.lg))

    // Landscape / short windows: shrink the hero so timer + actions fit.
    val activeRingSize = if (rememberIsCompactHeight()) 180.dp else Dimens.heroRingActive
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        GoalRing(
            progress = dayProgress,
            size = activeRingSize,
            strokeWidth = 14.dp,
            modifier = Modifier.sizeIn(maxWidth = 320.dp, maxHeight = 320.dp),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                AdaptiveTimerText(
                    text = dayLabel,
                    color = palette.textPrimary,
                    maxWidthFraction = 0.62f,
                    modifier = Modifier.semantics {
                        contentDescription = context.getString(R.string.home_timer_cd_x, dayLabel)
                    },
                )
                Spacer(Modifier.height(Spacing.xs))
                Text(
                    snap.modeName,
                    style = MaterialTheme.typography.titleMedium,
                    color = palette.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth(0.6f),
                    textAlign = TextAlign.Center,
                )
                Text(
                    stringResource(R.string.home_todays_total),
                    style = MaterialTheme.typography.labelLarge,
                    color = palette.textTertiary,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }

    Spacer(Modifier.height(Spacing.lg))

    @OptIn(ExperimentalLayoutApi::class)
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        StatusChip(
            text = when {
                hardcore -> stringResource(R.string.home_hardcore)
                snap.strictMode -> stringResource(R.string.home_strict_on)
                else -> stringResource(R.string.home_strict_off)
            },
            icon = Icons.Outlined.Lock,
        )
        if (snap.allowlistOnly) {
            StatusChip(
                text = stringResource(
                    R.string.modes_chip_allowlist_x,
                    snap.allowedApps.size,
                ),
                icon = Icons.Outlined.Checklist,
            )
        } else {
            StatusChip(
                text = pluralStringResource(
                    R.plurals.plural_apps_blocked,
                    snap.blockedApps.size,
                    snap.blockedApps.size,
                ),
                icon = Icons.Outlined.Block,
            )
        }
        snap.autoEndMinutes?.let {
            StatusChip(
                text = stringResource(R.string.modes_chip_auto_x, formatAutoEndShort(it)),
                icon = Icons.Outlined.Timer,
            )
        }
        if (snap.keepAwake == true) {
            StatusChip(text = stringResource(R.string.modes_chip_awake))
        }
        Box(
            modifier = Modifier.toggleable(
                value = snap.dndEnabled,
                role = Role.Switch,
                onValueChange = { onToggleDnd() },
            ),
        ) {
            StatusChip(
                text = if (snap.dndEnabled) {
                    stringResource(R.string.home_dnd_on)
                } else {
                    stringResource(R.string.home_dnd_off)
                },
                icon = Icons.Outlined.NotificationsOff,
            )
        }
    }

    Spacer(Modifier.height(Spacing.lg))

    val enforcedPkgs = if (snap.allowlistOnly) snap.allowedApps else snap.blockedApps
    val blockedEntries = AppLabels.labeledPackages(context, enforcedPkgs)
    val unlabeledCount = (enforcedPkgs.size - blockedEntries.size).coerceAtLeast(0)
    if (blockedEntries.isNotEmpty() || unlabeledCount > 0) {
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            blockedEntries.take(4).forEach { (pkg, label) ->
                BlockedAppBadge(pkg = pkg, label = label)
            }
            val hidden = (blockedEntries.size - 4).coerceAtLeast(0) + unlabeledCount
            if (hidden > 0) {
                Box(modifier = Modifier.padding(vertical = Spacing.sm)) {
                    StatusChip(text = stringResource(R.string.home_more_x, hidden))
                }
            }
        }
    }

    Spacer(Modifier.height(Spacing.lg))

    if (endLocked) {
        FocusCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                modifier = Modifier.padding(Spacing.cardPaddingLarge),
            ) {
                Icon(
                    Icons.Outlined.Lock,
                    contentDescription = null,
                    tint = palette.accent,
                    modifier = Modifier.size(28.dp),
                )
                Text(
                    stringResource(R.string.home_tap_tag_to_end, snap.tagName ?: "NFC"),
                    style = MaterialTheme.typography.bodyLarge,
                    color = palette.textPrimary,
                    textAlign = TextAlign.Center,
                )
                Text(
                    stringResource(R.string.home_no_tag_emergency),
                    style = MaterialTheme.typography.labelLarge,
                    color = palette.textSecondary,
                    textAlign = TextAlign.Center,
                )
                HoldToEndButton(
                    onHoldComplete = onEmergencyEnd,
                    label = stringResource(R.string.home_hold_emergency),
                )
            }
        }
    } else {
        Button(
            onClick = onEnd,
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
        Text(
            stringResource(R.string.home_or_tap_tag),
            style = MaterialTheme.typography.labelLarge,
            color = palette.textTertiary,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun BlockedAppBadge(pkg: String, label: String) {    val palette = LocalFocusPalette.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(64.dp),
    ) {
        AppIcon(pkg = pkg, label = label, size = 44.dp, corner = 12.dp)
        Spacer(Modifier.height(Spacing.xs))
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = palette.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun SessionRow(session: SessionRecord, modeNames: Map<Long, String>) {
    val palette = LocalFocusPalette.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(palette.card, MaterialTheme.shapes.medium)
            .border(Dimens.cardBorderWidth, palette.cardBorder, MaterialTheme.shapes.medium)
            .clip(MaterialTheme.shapes.medium)
            .padding(horizontal = Spacing.cardPadding, vertical = Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(palette.ringTrack, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Outlined.Timer, contentDescription = null, modifier = Modifier.size(18.dp), tint = palette.accentText)
        }
        Spacer(Modifier.size(Spacing.md))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                modeNames[session.modeId] ?: stringResource(R.string.home_deep_work_fallback),
                style = MaterialTheme.typography.titleMedium,
                color = palette.textPrimary,
            )
            Spacer(Modifier.height(Spacing.xxs))
            Text(
                "${TimeUtil.formatClock(session.startedAt)} — ${TimeUtil.formatDuration(session.durationMs)}",
                style = MaterialTheme.typography.labelLarge,
                color = palette.textSecondary,
            )
        }
        Text(
            TimeUtil.formatDuration(session.durationMs),
            style = MaterialTheme.typography.titleMedium,
            color = palette.accentText,
        )
    }
}

private fun formatAutoEndShort(minutes: Int): String = when {
    minutes < 60 -> "${minutes}m"
    minutes % 60 == 0 -> "${minutes / 60}h"
    else -> "${minutes / 60}h ${minutes % 60}m"
}