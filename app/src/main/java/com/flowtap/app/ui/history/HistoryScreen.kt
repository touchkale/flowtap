package com.flowtap.app.ui.history

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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.flowtap.app.R
import com.flowtap.app.data.model.SessionRecord
import com.flowtap.app.data.repo.WeekStats
import com.flowtap.app.di.AppContainer
import com.flowtap.app.ui.components.AdaptiveTimerText
import com.flowtap.app.ui.components.EmptyState
import com.flowtap.app.ui.components.FocusCard
import com.flowtap.app.ui.components.GoalRing
import com.flowtap.app.ui.components.SectionHeader
import com.flowtap.app.ui.theme.Dimens
import com.flowtap.app.ui.theme.PaperCardShape
import com.flowtap.app.ui.theme.PrimaryButtonShape
import com.flowtap.app.ui.theme.LocalFocusPalette
import com.flowtap.app.ui.theme.Spacing
import com.flowtap.app.ui.util.rememberAdaptiveSidePadding
import com.flowtap.app.util.TimeUtil

@Composable
fun HistoryScreen(
    container: AppContainer,
    onOpenHome: () -> Unit,
) {
    val vm: HistoryViewModel = viewModel(factory = HistoryViewModel.factory(container))
    val palette = LocalFocusPalette.current

    val week by vm.week.collectAsStateWithLifecycle()
    val modes by vm.modes.collectAsStateWithLifecycle()
    val modeWeeks by vm.modeWeeks.collectAsStateWithLifecycle()
    val combinedDaily by vm.combinedDailyMs.collectAsStateWithLifecycle()
    val streak by vm.streakDays.collectAsStateWithLifecycle()
    val longest by vm.longestMs.collectAsStateWithLifecycle()
    val average by vm.averageDailyMs.collectAsStateWithLifecycle()
    val sessions by vm.sessions.collectAsStateWithLifecycle()
    val modeNames by vm.modeNames.collectAsStateWithLifecycle()

    var selectedSession by remember { mutableStateOf<SessionRecord?>(null) }

    // T2: recompute day-boundary stats on resume (midnight crossing).
    val resumeTick = com.flowtap.app.ui.util.rememberResumeTick()
    androidx.compose.runtime.LaunchedEffect(resumeTick) {
        container.statsRepository.refreshTime()
    }

    // Daily bars compare against the combined per-mode daily goals.
    val dailyGoalMs = combinedDaily
    val sidePadding = rememberAdaptiveSidePadding()

    // MainNav's Scaffold already applies the status-bar inset; only the
    // navigation-bar inset is needed here so the top matches Home exactly.
    Scaffold(
        containerColor = palette.background,
        contentWindowInsets = WindowInsets.navigationBars,
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = sidePadding),
        ) {
            item {
                Spacer(Modifier.height(Spacing.screenTop))
                Text(
                    stringResource(R.string.history_title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = palette.textPrimary,
                )
                Spacer(Modifier.height(Spacing.xs))
                Text(
                    TimeUtil.formatWeekRange(
                        week.days.firstOrNull()?.dayStart ?: System.currentTimeMillis(),
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.textSecondary,
                )
                Spacer(Modifier.height(Spacing.sectionGap))
            }

            if (sessions.isNotEmpty()) item {
                ModeWeekCard(modes = modes, modeWeeks = modeWeeks)
                Spacer(Modifier.height(Spacing.cardGap))
                StatTrio(streak = streak, longest = longest, average = average)
                Spacer(Modifier.height(Spacing.sectionGap))
            }

            if (sessions.isNotEmpty()) item {
                if (week.days.isNotEmpty()) {
                    SectionHeader(title = stringResource(R.string.history_this_week))
                    Spacer(Modifier.height(Spacing.headerGap))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                    ) {
                        Text(
                            stringResource(R.string.history_daily_bars),
                            style = MaterialTheme.typography.labelLarge,
                            color = palette.textSecondary,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            stringResource(
                                R.string.history_daily_goal_x,
                                TimeUtil.formatGoal(dailyGoalMs / 60_000),
                            ),
                            style = MaterialTheme.typography.labelLarge,
                            color = palette.accentText,
                            maxLines = 1,
                        )
                    }
                    Spacer(Modifier.height(Spacing.headerGap))
                    FocusCard(modifier = Modifier.fillMaxWidth()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(Spacing.cardPadding),
                        ) {
                            WeekChart(dayTotals = week.days, goalMs = dailyGoalMs)
                        }
                    }
                    if (week.days.all { it.totalMs == 0L }) {
                        Spacer(Modifier.height(Spacing.sm))
                        Text(
                            stringResource(R.string.history_chart_empty_hint),
                            style = MaterialTheme.typography.labelLarge,
                            color = palette.textTertiary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
                Spacer(Modifier.height(Spacing.sectionGap))
            }

            if (sessions.isNotEmpty()) item {
                SectionHeader(title = stringResource(R.string.history_all))
                Spacer(Modifier.height(Spacing.headerGap))
            }

            if (sessions.isEmpty()) {
                item {
                    // P1c: compact hint instead of a tall empty chart card.
                    EmptyState(
                        title = stringResource(R.string.history_empty_title),
                        subtitle = stringResource(R.string.history_empty_subtitle),
                        action = stringResource(R.string.history_open_home),
                        onAction = onOpenHome,
                    )
                }
            } else {
                items(sessions, key = { it.id }) { session ->
                    SessionRow(
                        session = session,
                        modeName = modeNames[session.modeId]
                            ?: stringResource(R.string.history_deep_work),
                        onClick = { selectedSession = session },
                    )
                    Spacer(Modifier.height(Spacing.rowGap))
                }
            }
            item { Spacer(Modifier.height(Spacing.listBottom)) }
        }
    }

    selectedSession?.let { session ->
        SessionDetailDialog(
            session = session,
            modeName = modeNames[session.modeId] ?: stringResource(R.string.history_deep_work),
            onDismiss = { selectedSession = null },
            onDelete = {
                vm.deleteSession(session.id)
                selectedSession = null
            },
        )
    }
}

/**
 * This week's focus, one row per mode against its own 7× daily target —
 * a single summed goal is meaningless once modes have individual goals.
 */
@Composable
private fun ModeWeekCard(
    modes: List<com.flowtap.app.data.model.FocusMode>,
    modeWeeks: Map<Long, WeekStats>,
) {
    val palette = LocalFocusPalette.current
    FocusCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.cardPadding),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            Text(
                stringResource(R.string.history_this_week_title),
                style = MaterialTheme.typography.titleMedium,
                color = palette.textPrimary,
            )
            if (modes.isEmpty()) {
                Text(
                    stringResource(R.string.history_chart_empty_hint),
                    style = MaterialTheme.typography.labelLarge,
                    color = palette.textTertiary,
                )
            } else {
                modes.forEach { mode ->
                    val total = modeWeeks[mode.id]?.totalMs ?: 0L
                    val target = mode.dailyGoalMinutes * 7 * 60_000L
                    val progress = if (target > 0) total.toFloat() / target else 0f
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                mode.name,
                                style = MaterialTheme.typography.bodyMedium,
                                color = palette.textPrimary,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                stringResource(
                                    R.string.history_mode_week_x,
                                    TimeUtil.formatHoursMinutes(total),
                                    TimeUtil.formatGoal(mode.dailyGoalMinutes * 7L),
                                ),
                                style = MaterialTheme.typography.labelLarge,
                                color = palette.accentText,
                            )
                        }
                        androidx.compose.material3.LinearProgressIndicator(
                            progress = { progress.coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(MaterialTheme.shapes.small),
                            color = palette.accent,
                            trackColor = palette.ringTrack,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatTrio(streak: Int, longest: Long, average: Long) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        StatBox(
            pluralStringResource(R.plurals.plural_streak_days, streak, streak),
            stringResource(R.string.history_streak),
            Icons.Outlined.LocalFireDepartment,
            Modifier.weight(1f),
        )
        StatBox(
            TimeUtil.formatHoursMinutes(longest),
            stringResource(R.string.history_longest),
            Icons.Outlined.Schedule,
            Modifier.weight(1f),
        )
        StatBox(
            TimeUtil.formatHoursMinutes(average),
            stringResource(R.string.history_daily_avg),
            Icons.Outlined.FitnessCenter,
            Modifier.weight(1f),
        )
    }
}

@Composable
private fun StatBox(
    value: String,
    label: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
) {
    val palette = LocalFocusPalette.current
    Column(
        modifier = modifier
            .background(palette.card, PaperCardShape)
            .border(Dimens.cardBorderWidth, palette.cardBorder, PaperCardShape)
            .padding(horizontal = Spacing.md, vertical = Spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, contentDescription = null, tint = palette.accentText, modifier = Modifier.size(20.dp))
        Spacer(Modifier.height(Spacing.sm))
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            color = palette.textPrimary,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
        Spacer(Modifier.height(Spacing.xxs))
        Text(label, style = MaterialTheme.typography.labelLarge, color = palette.textSecondary)
    }
}

@Composable
private fun SessionRow(
    session: SessionRecord,
    modeName: String,
    onClick: () -> Unit,
) {
    val palette = LocalFocusPalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(palette.card, PaperCardShape)
            .border(Dimens.cardBorderWidth, palette.cardBorder, PaperCardShape)
            .clickable(onClick = onClick)
            .padding(horizontal = Spacing.cardPadding, vertical = Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(palette.ringTrack, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Outlined.Timer,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = palette.accentText,
            )
        }
        Spacer(Modifier.size(Spacing.md))
        Column(modifier = Modifier.weight(1f)) {
            Text(modeName, style = MaterialTheme.typography.titleMedium, color = palette.textPrimary)
            Spacer(Modifier.height(Spacing.xxs))
            Text(
                "${TimeUtil.formatDay(session.startedAt)} · ${TimeUtil.formatClock(session.startedAt)}",
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

@Composable
private fun SessionDetailDialog(
    session: SessionRecord,
    modeName: String,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
) {
    val palette = LocalFocusPalette.current
    var confirmDelete by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (confirmDelete) stringResource(R.string.history_delete_title) else modeName) },
        text = {
            if (confirmDelete) {
                Text(stringResource(R.string.history_delete_sub))
            } else {
                Column {
                    DetailRow(
                        stringResource(R.string.history_started),
                        TimeUtil.formatClock(session.startedAt),
                    )
                    DetailRow(
                        stringResource(R.string.history_ended),
                        if (session.endedAt > 0) TimeUtil.formatClock(session.endedAt) else "—",
                    )
                    DetailRow(
                        stringResource(R.string.history_duration),
                        TimeUtil.formatDuration(session.durationMs),
                    )
                    DetailRow(
                        stringResource(R.string.history_ended_by),
                        prettyEndReason(session.endReason.value),
                    )
                }
            }
        },
        confirmButton = {
            if (confirmDelete) {
                TextButton(onClick = onDelete) {
                    Text(
                        stringResource(R.string.common_delete),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            } else {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_close)) }
            }
        },
        dismissButton = {
            if (confirmDelete) {
                TextButton(onClick = { confirmDelete = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            } else {
                TextButton(onClick = { confirmDelete = true }) {
                    Icon(
                        Icons.Outlined.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.error,
                    )
                    Spacer(Modifier.size(4.dp))
                    Text(
                        stringResource(R.string.common_delete),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
    )
}

@Composable
private fun DetailRow(label: String, value: String) {
    val palette = LocalFocusPalette.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.sm),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = palette.textSecondary)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = palette.textPrimary)
    }
}

private fun prettyEndReason(raw: String): String {
    val cleaned = raw.replace('_', ' ').lowercase().trim()
    if (cleaned.isEmpty()) return "—"
    return cleaned.split(" ").joinToString(" ") { word ->
        word.replaceFirstChar { c -> c.uppercase() }
    }
}
