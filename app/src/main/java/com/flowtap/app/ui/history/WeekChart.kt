package com.flowtap.app.ui.history

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.flowtap.app.R
import com.flowtap.app.data.model.DayTotal
import com.flowtap.app.ui.theme.LocalFocusPalette
import com.flowtap.app.ui.theme.Spacing
import com.flowtap.app.util.TimeUtil
import kotlinx.coroutines.launch

/**
 * Monday–Sunday bar chart. [goalMs] is the DAILY goal — callers must pass
 * weeklyGoal/7, not the weekly total. Bars animate up with a 50ms stagger.
 */
@Composable
fun WeekChart(
    dayTotals: List<DayTotal>,
    goalMs: Long,
    modifier: Modifier = Modifier,
) {
    val palette = LocalFocusPalette.current
    val now = System.currentTimeMillis()
    val todayStart = TimeUtil.localDayStart(now)

    val maxVal = maxOf(goalMs, dayTotals.maxOfOrNull { it.totalMs } ?: 0L, 1L)
    val daysPart = buildString {
        dayTotals.forEachIndexed { i, day ->
            append("${TimeUtil.weekdayInitial(i)} ${TimeUtil.formatHoursMinutes(day.totalMs)}; ")
        }
    }
    val description = stringResource(
        R.string.history_chart_cd_x,
        daysPart,
        TimeUtil.formatHoursMinutes(goalMs),
    )

    val animations = remember(dayTotals) { dayTotals.map { Animatable(0f) } }
    LaunchedEffect(dayTotals) {
        animations.forEachIndexed { i, anim ->
            launch {
                anim.animateTo(1f, tween(durationMillis = 600, delayMillis = i * 50))
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) { contentDescription = description },
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp),
        ) {
            val slot = size.width / dayTotals.size.coerceAtLeast(1)
            val barWidth = slot * 0.45f
            val topPadding = 10.dp.toPx()

            val goalFrac = (goalMs.toFloat() / maxVal).coerceIn(0f, 1f)
            val goalY = topPadding + (size.height - topPadding) * (1f - goalFrac)
            drawLine(
                color = palette.accentText.copy(alpha = 0.6f),
                start = Offset(0f, goalY),
                end = Offset(size.width, goalY),
                strokeWidth = 2.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f)),
            )

            dayTotals.forEachIndexed { i, day ->
                val frac = (day.totalMs.toFloat() / maxVal).coerceIn(0f, 1f) * animations[i].value
                val barHeight = (size.height - topPadding) * frac
                val left = i * slot + (slot - barWidth) / 2f
                val isToday = day.dayStart == todayStart
                drawRoundRect(
                    color = if (isToday) palette.accent else palette.accent.copy(alpha = 0.45f),
                    topLeft = Offset(left, (size.height - barHeight).coerceAtMost(size.height - 2.dp.toPx())),
                    size = Size(barWidth, barHeight.coerceAtLeast(if (day.totalMs > 0) 4.dp.toPx() else 0f)),
                    cornerRadius = CornerRadius(6.dp.toPx()),
                )
            }
        }
        Spacer(Modifier.height(Spacing.sm))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            dayTotals.forEachIndexed { i, day ->
                val isToday = day.dayStart == todayStart
                Text(
                    text = TimeUtil.weekdayInitial(i),
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium,
                    ),
                    color = if (isToday) palette.accentText else palette.textTertiary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = Spacing.xxs),
                )
            }
        }
    }
}