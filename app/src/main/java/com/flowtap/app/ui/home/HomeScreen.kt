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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
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
import com.flowtap.app.ui.theme.PaperCardShape
import com.flowtap.app.ui.theme.PrimaryButtonShape
import com.flowtap.app.ui.theme.LocalFocusPalette
import com.flowtap.app.ui.theme.Spacing
import com.flowtap.app.ui.util.rememberAdaptiveSidePadding
import com.flowtap.app.ui.util.rememberIsCompactHeight
import com.flowtap.app.ui.util.rememberResumeTick
import com.flowtap.app.session.BlockingReadiness
import com.flowtap.app.data.prefs.BlockingMethod
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

    val todayByMode by vm.todayByMode.collectAsStateWithLifecycle()
    val selectedMode by vm.selectedMode.collectAsStateWithLifecycle()
    val selectedToday by vm.selectedTodayMs.collectAsStateWithLifecycle()
    val selectedGoal by vm.selectedGoalMs.collectAsStateWithLifecycle()
    val hardcore by vm.hardcoreMode.collectAsStateWithLifecycle()
    val recent by vm.recent.collectAsStateWithLifecycle()
    val modes by vm.modes.collectAsStateWithLifecycle()
    val softStartMode by vm.softStartMode.collectAsStateWithLifecycle()
    val modeNames by vm.modeNames.collectAsStateWithLifecycle()
    val active by SessionManager.active.collectAsStateWithLifecycle()

    // F12: NFC-off hint, refreshed whenever returning from system screens.
    // T2: stats also refresh on resume so a midnight crossing without a new
    // session still recomputes today's totals.
    val nfcTick = rememberResumeTick()
    LaunchedEffect(nfcTick) { container.statsRepository.refreshTime() }
    val nfcOff = remember(nfcTick) {
        Permissions.hasNfcHardware(context) && !Permissions.isNfcEnabled(context)
    }
    // Warm-paper readiness: permission granted is not proof of enforcement.
    val blockingMethod by container.settingsDataStore.blockingMethod.collectAsStateWithLifecycle(initialValue = BlockingMethod.FULL_POWER)
    val readiness = remember(nfcTick, blockingMethod, active) {
        BlockingReadiness(
            method = blockingMethod,
            accessibilityEnabled = Permissions.isAccessibilityEnabled(context),
            accessibilityServiceConnected = com.flowtap.app.blocking.FocusAccessibilityService.isConnected,
            usageAccessGranted = Permissions.isUsageAccessGranted(context),
            overlayGranted = Permissions.canDrawOverlays(context),
            sessionActive = active != null,
            selectedAppCount = (selectedMode ?: modes.firstOrNull())?.let { if (it.allowlistOnly) it.allowedApps.size else it.blockedApps.size } ?: 0,
        )
    }

    fun onToggleDnd() {
        vm.cycleNotificationMode { result ->
            if (result == HomeViewModel.CycleResult.DND &&
                !Permissions.isDndGranted(context)
            ) {
                Permissions.enableDnd(context)
            }
        }
    }

    // H2: true three-option selector — direct choice, same permission routing.
    fun onSelectSound(next: com.flowtap.app.data.model.NotificationMode) {
        vm.setNotificationMode(next) { result ->
            if (result == HomeViewModel.CycleResult.DND &&
                !Permissions.isDndGranted(context)
            ) {
                Permissions.enableDnd(context)
            }
        }
    }

    val sidePadding = rememberAdaptiveSidePadding()
    Scaffold(
        containerColor = palette.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (active == null && modes.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(palette.background)
                        .padding(horizontal = sidePadding, vertical = Spacing.sm),
                ) {
                    Button(
                        onClick = { vm.startFocusDefault() },
                        enabled = softStartMode != null,
                        modifier = Modifier.fillMaxWidth().height(Dimens.primaryButtonHeight),
                        shape = PrimaryButtonShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = palette.accent,
                            contentColor = palette.onAccent,
                        ),
                    ) {
                        Text(
                            stringResource(R.string.home_start),
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                }
            }
        },
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
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
                    .background(palette.card, PaperCardShape)
                    .border(Dimens.cardBorderWidth, palette.cardBorder, PaperCardShape)
                    .clip(PaperCardShape)
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
            val snap = active!!
            val runningMode = modes.firstOrNull { it.id == snap.modeId }
            val runningGoalMs = ((runningMode?.dailyGoalMinutes ?: 300) * 60_000L)
            val runningBaseMs = todayByMode[snap.modeId] ?: 0L
            ActiveSessionView(
                snap = snap,
                hardcore = hardcore,
                context = context,
                onEnd = { vm.endFocus() },
                onEmergencyEnd = { vm.endFocusEmergency() },
                onToggleDnd = ::onToggleDnd,
                onSelectSound = ::onSelectSound,
                modeTodayBaseMs = runningBaseMs,
                modeGoalMs = runningGoalMs,
            )
        } else {
            IdleView(
                todayTotal = selectedToday,
                dailyGoal = selectedGoal,
                recent = recent,
                modeNames = modeNames,
                modes = modes,
                selectedMode = selectedMode,
                onSelectMode = { vm.selectMode(it) },
                defaultMode = selectedMode ?: modes.firstOrNull(),
                softStartMode = softStartMode,
                modesEmpty = modes.isEmpty(),
                readinessStatus = readiness.statusLabel(),
                readinessReady = readiness.isReady,
                readinessMissing = readiness.missingRequirement(),
                onToggleDnd = ::onToggleDnd,
                onSelectSound = ::onSelectSound,
                onPairTag = {
                    context.startActivity(Intent(context, PairTagActivity::class.java))
                },
                onOpenHistory = onOpenHistory,
            )
        }
        Spacer(Modifier.height(Spacing.listBottom))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun IdleView(
    todayTotal: Long,
    dailyGoal: Long,
    recent: List<SessionRecord>,
    modeNames: Map<Long, String>,
    modes: List<FocusMode>,
    selectedMode: FocusMode?,
    onSelectMode: (Long) -> Unit,
    defaultMode: FocusMode?,
    softStartMode: FocusMode?,
    modesEmpty: Boolean,
    readinessStatus: String,
    readinessReady: Boolean,
    readinessMissing: String?,
    onToggleDnd: () -> Unit,
    onSelectSound: (com.flowtap.app.data.model.NotificationMode) -> Unit,
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
            shape = PrimaryButtonShape,
            colors = ButtonDefaults.buttonColors(containerColor = palette.accent, contentColor = palette.onAccent),
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

    ModeSelectorRow(
        modes = modes,
        selected = selectedMode,
        onSelect = onSelectMode,
    )
    Spacer(Modifier.height(Spacing.rowGap))

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
                    compact = true,
                )
            }
            Spacer(Modifier.height(Spacing.md))
            val heroSize = if (rememberIsCompactHeight()) Dimens.heroRingCompact else Dimens.heroRing
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                GoalRing(
                    progress = progress,
                    size = heroSize,
                    strokeWidth = Dimens.heroRingStroke,
                    modifier = Modifier.sizeIn(maxWidth = 320.dp, maxHeight = 320.dp),
                ) {
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
                                maxWidthFraction = 0.6f,
                            )
                        }
                        Spacer(Modifier.height(Spacing.xs))
                        Text(
                            selectedMode?.name
                                ?: stringResource(R.string.home_deep_work_fallback),
                            style = MaterialTheme.typography.titleMedium,
                            color = palette.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.fillMaxWidth(0.6f),
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
            Spacer(Modifier.height(Spacing.md))
            Text(
                text = if (percent >= 100) {
                    stringResource(R.string.home_goal_complete)
                } else {
                    stringResource(
                        R.string.home_goal_remaining_x,
                        TimeUtil.formatGoal(dailyGoal / 60_000),
                        if (remainingMs % 3_600_000L == 0L) {
                            TimeUtil.formatGoal(remainingMs / 60_000)
                        } else {
                            TimeUtil.formatHoursMinutes(remainingMs)
                        },
                    )
                },
                style = MaterialTheme.typography.labelLarge,
                color = palette.accentText,
                textAlign = TextAlign.Center,
            )
        }
    }

    Spacer(Modifier.height(Spacing.lg))

    if (softStartMode == null) {
        Spacer(Modifier.height(Spacing.sm))
        Text(
            text = stringResource(R.string.home_strict_only_hint),
            style = MaterialTheme.typography.labelLarge,
            color = palette.textTertiary,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )
    }
    Spacer(Modifier.height(Spacing.sm))
    Text(
        text = if (readinessReady) readinessStatus else listOfNotNull(readinessStatus, readinessMissing).joinToString(" · "),
        style = MaterialTheme.typography.labelLarge,
        color = if (readinessReady) palette.accentText else palette.textTertiary,
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
            compact = true,
        )
        if (mode?.allowlistOnly == true) {
            StatusChip(
                text = stringResource(
                    R.string.modes_chip_allowlist_x,
                    mode.allowedApps.size,
                ),
                icon = Icons.Outlined.Checklist,
                compact = true,
            )
        } else {
            val blockedCount = mode?.blockedApps?.size ?: 0
            StatusChip(
                text = pluralStringResource(R.plurals.plural_apps_blocked, blockedCount, blockedCount),
                icon = Icons.Outlined.Block,
                compact = true,
            )
        }
        mode?.autoEndMinutes?.let {
            StatusChip(
                text = stringResource(R.string.modes_chip_auto_x, formatAutoEndShort(it)),
                icon = Icons.Outlined.Timer,
                compact = true,
            )
        }
        if (mode?.keepAwake == true) {
            StatusChip(
                text = stringResource(R.string.modes_chip_awake),
                compact = true,
            )
        }
        SoundModeChip(
            current = mode?.notificationMode
                ?: com.flowtap.app.data.model.NotificationMode.NONE,
            onSelect = onSelectSound,
        )
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
    onSelectSound: (com.flowtap.app.data.model.NotificationMode) -> Unit,
    modeTodayBaseMs: Long,
    modeGoalMs: Long,
) {
    val palette = LocalFocusPalette.current
    // H1: the tick state drives the display — elapsedMs is read below so each
    // one-second write invalidates composition and the timer visibly advances.
    var elapsedMs by remember(snap.sessionId) { mutableLongStateOf(snap.elapsedMs()) }
    LaunchedEffect(snap.sessionId) {
        while (true) {
            elapsedMs = snap.elapsedMs()
            delay(1000)
        }
    }

    // Hardcore mode: the session cannot be ended from the app at all.
    val endLocked = com.flowtap.app.session.SessionPolicy.isEndLocked(snap, hardcore)

    // Active timer shows THIS mode's today total (cumulative, live-ticking),
    // not all modes combined. modeTodayBaseMs counts completed sessions for
    // this mode only — the running one isn't persisted until it ends.
    // Overnight sessions are capped at today's slice. nowMs is derived from
    // the observed tick so the display is driven by that clock (H1).
    val nowMs = snap.startedAtEpoch + elapsedMs
    val elapsedToday = (nowMs - maxOf(snap.startedAtEpoch, TimeUtil.localDayStart(nowMs)))
        .coerceAtLeast(0L)
    val dayTotal = modeTodayBaseMs + elapsedToday
    val dayProgress = if (modeGoalMs > 0) dayTotal.toFloat() / modeGoalMs else 0f
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

    // Unified with idle: same ring spec so active/inactive timers match.
    val activeRingSize = if (rememberIsCompactHeight()) Dimens.heroRingCompact else Dimens.heroRing
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        GoalRing(
            progress = dayProgress,
            size = activeRingSize,
            strokeWidth = Dimens.heroRingStroke,
            modifier = Modifier.sizeIn(maxWidth = 320.dp, maxHeight = 320.dp),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                AdaptiveTimerText(
                    text = dayLabel,
                    color = palette.textPrimary,
                    baseFontSize = MaterialTheme.typography.displayMedium.fontSize,
                    maxWidthFraction = 0.6f,
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
                    // H1: session elapsed is distinct from today's cumulative total.
                    TimeUtil.formatClockTime(elapsedMs) + " • " + stringResource(R.string.home_todays_total),
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
            compact = true,
        )
        if (snap.allowlistOnly) {
            StatusChip(
                text = stringResource(
                    R.string.modes_chip_allowlist_x,
                    snap.allowedApps.size,
                ),
                icon = Icons.Outlined.Checklist,
                compact = true,
            )
        } else {
            StatusChip(
                text = pluralStringResource(
                    R.plurals.plural_apps_blocked,
                    snap.blockedApps.size,
                    snap.blockedApps.size,
                ),
                icon = Icons.Outlined.Block,
                compact = true,
            )
        }
        snap.autoEndMinutes?.let {
            StatusChip(
                text = stringResource(R.string.modes_chip_auto_x, formatAutoEndShort(it)),
                icon = Icons.Outlined.Timer,
                compact = true,
            )
        }
        if (snap.keepAwake == true) {
            StatusChip(
                text = stringResource(R.string.modes_chip_awake),
                compact = true,
            )
        }
        SoundModeChip(
            current = snap.notificationMode,
            onSelect = onSelectSound,
        )
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
            shape = PrimaryButtonShape,
            colors = ButtonDefaults.buttonColors(containerColor = palette.accent, contentColor = palette.onAccent),
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

/**
 * H2: true three-option sound selector. A plain button opens Keep
 * unchanged / Silent / Do Not Disturb — never a switch implying binary.
 */
@Composable
private fun SoundModeChip(
    current: com.flowtap.app.data.model.NotificationMode,
    onSelect: (com.flowtap.app.data.model.NotificationMode) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val label = when (current) {
        com.flowtap.app.data.model.NotificationMode.DND ->
            stringResource(R.string.home_dnd_on)
        com.flowtap.app.data.model.NotificationMode.SILENT ->
            stringResource(R.string.home_silent)
        com.flowtap.app.data.model.NotificationMode.NONE ->
            stringResource(R.string.home_notif_off)
    }
    Box {
        Box(
            modifier = Modifier
                .clickable(role = Role.Button, onClick = { open = true })
                .semantics {
                    contentDescription =
                        context.getString(R.string.home_sound_cd_x, label)
                },
        ) {
            StatusChip(
                text = label,
                icon = Icons.Outlined.NotificationsOff,
                compact = true,
            )
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.modes_notif_none)) },
                onClick = {
                    open = false
                    onSelect(com.flowtap.app.data.model.NotificationMode.NONE)
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.modes_notif_silent)) },
                onClick = {
                    open = false
                    onSelect(com.flowtap.app.data.model.NotificationMode.SILENT)
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.modes_notif_dnd)) },
                onClick = {
                    open = false
                    onSelect(com.flowtap.app.data.model.NotificationMode.DND)
                },
            )
        }
    }
}

/**
 * Mode selector row above the focus card: current mode + chevron, menu lists
 * all modes. Sits outside the ring so the menu can never cover timer content.
 */
@Composable
private fun ModeSelectorRow(
    modes: List<FocusMode>,
    selected: FocusMode?,
    onSelect: (Long) -> Unit,
) {
    val palette = LocalFocusPalette.current
    var expanded by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(palette.card, PaperCardShape)
            .border(Dimens.cardBorderWidth, palette.cardBorder, PaperCardShape)
            .clip(PaperCardShape)
            .clickable(enabled = modes.size > 1) { expanded = true }
            .padding(horizontal = Spacing.cardPadding, vertical = Spacing.md),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Outlined.Nfc,
                contentDescription = null,
                tint = palette.accentText,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.size(Spacing.md))
            Text(
                selected?.name ?: stringResource(R.string.home_select_mode),
                style = MaterialTheme.typography.titleMedium,
                color = palette.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (modes.size > 1) {
                Icon(
                    Icons.Filled.ArrowDropDown,
                    contentDescription = stringResource(R.string.home_mode_dropdown_cd),
                    tint = palette.textSecondary,
                )
            }
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            modes.forEach { mode ->
                DropdownMenuItem(
                    text = { Text(mode.name) },
                    onClick = {
                        onSelect(mode.id)
                        expanded = false
                    },
                )
            }
        }
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
            .background(palette.card, PaperCardShape)
            .border(Dimens.cardBorderWidth, palette.cardBorder, PaperCardShape)
            .clip(PaperCardShape)
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
