package com.hkm.streakwidget.ui.insights

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hkm.streakwidget.R
import com.hkm.streakwidget.domain.InsightDay
import com.hkm.streakwidget.domain.InsightDayState
import com.hkm.streakwidget.domain.InsightsCalculator
import com.hkm.streakwidget.domain.MilestoneRules
import com.hkm.streakwidget.model.Habit
import com.hkm.streakwidget.ui.theme.AppIcons
import java.util.Locale
import kotlin.math.roundToInt

/** Honest, gentle statistics for the focused goal: heatmap, rhythms and milestones. */
@Composable
fun InsightsScreen(
    habit: Habit?,
    reduceMotion: Boolean,
    onBack: () -> Unit,
    onHaptic: () -> Unit,
    modifier: Modifier = Modifier
) {
    val summary = remember(habit, habit?.completionHistory, habit?.pausedDates, habit?.debts) {
        habit?.let { InsightsCalculator.summarize(it) } ?: InsightsCalculator.emptySummary()
    }
    val accent = habit?.accentColor() ?: Color(0xFFE97B4A)
    val weekdayLabels = stringArrayResource(R.array.insights_weekday_labels)

    Column(
        modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp)
            .padding(top = 4.dp, bottom = 28.dp)
    ) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 64.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { onHaptic(); onBack() }, modifier = Modifier.size(48.dp)) {
                Icon(
                    AppIcons.Back,
                    contentDescription = stringResource(R.string.cd_back),
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(Modifier.width(4.dp))
            Text(
                stringResource(R.string.insights_eyebrow),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.4.sp
            )
        }
        Text(
            habit?.name ?: stringResource(R.string.insights_title),
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 28.sp,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            stringResource(R.string.insights_subtitle),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 14.sp,
            lineHeight = 20.sp
        )
        Spacer(Modifier.height(22.dp))

        if (habit == null) {
            InfoCard(stringResource(R.string.insights_no_goal))
            return@Column
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            StatCard(
                modifier = Modifier.weight(1f),
                icon = AppIcons.Flame,
                label = stringResource(R.string.insights_current),
                value = summary.currentStreak.toString(),
                accent = accent
            )
            StatCard(
                modifier = Modifier.weight(1f),
                icon = AppIcons.Trophy,
                label = stringResource(R.string.insights_best),
                value = summary.bestRun.toString(),
                accent = accent
            )
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            StatCard(
                modifier = Modifier.weight(1f),
                icon = AppIcons.CheckCircle,
                label = stringResource(R.string.insights_checkins),
                value = summary.totalCheckIns.toString(),
                accent = accent
            )
            StatCard(
                modifier = Modifier.weight(1f),
                icon = AppIcons.ChartBars,
                label = stringResource(R.string.insights_consistency),
                value = "${summary.consistencyPercent}%",
                accent = accent
            )
        }
        Spacer(Modifier.height(20.dp))

        if (summary.totalCheckIns == 0) {
            InfoCard(stringResource(R.string.insights_empty))
            return@Column
        }

        SectionCard {
            SectionHeading(AppIcons.Calendar, stringResource(R.string.insights_heatmap))
            Text(
                stringResource(R.string.insights_heatmap_detail),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
                lineHeight = 15.sp
            )
            Spacer(Modifier.height(12.dp))
            Heatmap(summary.heatmap, accent, weekdayLabels)
            Spacer(Modifier.height(12.dp))
            HeatmapLegend(accent)
        }
        Spacer(Modifier.height(14.dp))

        SectionCard {
            SectionHeading(AppIcons.ChartBars, stringResource(R.string.insights_weekdays))
            Spacer(Modifier.height(8.dp))
            summary.weekdayRates.forEachIndexed { index, rate ->
                WeekdayBar(
                    label = weekdayLabels.getOrElse(index) { "" },
                    rate = rate,
                    accent = accent,
                    animate = !reduceMotion
                )
            }
        }
        Spacer(Modifier.height(14.dp))

        SectionCard {
            SectionHeading(AppIcons.Trophy, stringResource(R.string.insights_milestones))
            Spacer(Modifier.height(4.dp))
            MilestoneStrip(currentStreak = summary.currentStreak, accent = accent)
            Spacer(Modifier.height(10.dp))
            val next = MilestoneRules.next(summary.currentStreak)
            Text(
                next?.let { stringResource(R.string.insights_next, it) }
                    ?: stringResource(R.string.insights_all_milestones),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                lineHeight = 17.sp
            )
        }
        Spacer(Modifier.height(14.dp))

        SectionCard {
            Text(
                stringResource(R.string.insights_summary, summary.windowCheckIns, summary.activeDays),
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            if (summary.debtCount > 0 || summary.pauseCount > 0) {
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    if (summary.debtCount > 0) {
                        CountChip(AppIcons.Shield, stringResource(R.string.insights_debt_count, summary.debtCount))
                    }
                    if (summary.pauseCount > 0) {
                        CountChip(AppIcons.Pause, stringResource(R.string.insights_pause_count, summary.pauseCount))
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCard(
    modifier: Modifier,
    icon: ImageVector,
    label: String,
    value: String,
    accent: Color
) {
    Column(
        modifier
            .clip(RoundedCornerShape(22.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
            .padding(14.dp)
    ) {
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surface) {
            Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.padding(7.dp).size(17.dp))
        }
        Spacer(Modifier.height(10.dp))
        Text(value, color = MaterialTheme.colorScheme.onSurface, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
        Text(
            label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun SectionCard(content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
            .padding(14.dp),
        content = content
    )
}

@Composable
private fun SectionHeading(icon: ImageVector, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(15.dp))
        Spacer(Modifier.width(7.dp))
        Text(
            title.uppercase(Locale.getDefault()),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 1.1.sp
        )
    }
}

@Composable
private fun Heatmap(days: List<InsightDay>, accent: Color, weekdayLabels: Array<String>) {
    val weeks = remember(days) { days.chunked(7) }
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        repeat(7) { weekday ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    weekdayLabels.getOrElse(weekday) { "" },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(24.dp)
                )
                weeks.forEach { week ->
                    HeatCell(day = week.getOrNull(weekday), accent = accent)
                    Spacer(Modifier.width(3.dp))
                }
            }
        }
    }
}

@Composable
private fun HeatCell(day: InsightDay?, accent: Color) {
    val stateLabel = when (day?.state) {
        InsightDayState.COMPLETED -> stringResource(R.string.insights_legend_done)
        InsightDayState.PROTECTED -> stringResource(R.string.insights_legend_protected)
        InsightDayState.PAUSED -> stringResource(R.string.insights_legend_paused)
        InsightDayState.MISSED -> stringResource(R.string.insights_legend_missed)
        InsightDayState.INACTIVE, null -> null
    }
    val fill = when (day?.state) {
        InsightDayState.COMPLETED -> accent
        InsightDayState.PROTECTED -> accent.copy(alpha = 0.42f)
        InsightDayState.PAUSED -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.75f)
        InsightDayState.MISSED -> MaterialTheme.colorScheme.surfaceContainerHighest
        InsightDayState.INACTIVE, null -> Color.Transparent
    }
    val base = Modifier
        .size(15.dp)
        .clip(RoundedCornerShape(4.dp))
        .background(fill)
    val shaped = if (day?.state == InsightDayState.INACTIVE || day == null) {
        base.border(BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)), RoundedCornerShape(4.dp))
    } else {
        base
    }
    val described = if (day != null && stateLabel != null) {
        shaped.semantics { contentDescription = "${day.date}, $stateLabel" }
    } else {
        shaped
    }
    Box(described)
}

@Composable
private fun HeatmapLegend(accent: Color) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        LegendItem(accent, stringResource(R.string.insights_legend_done))
        LegendItem(accent.copy(alpha = 0.42f), stringResource(R.string.insights_legend_protected))
        LegendItem(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.75f), stringResource(R.string.insights_legend_paused))
        LegendItem(MaterialTheme.colorScheme.surfaceContainerHighest, stringResource(R.string.insights_legend_missed))
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(9.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(color)
        )
        Spacer(Modifier.width(4.dp))
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun WeekdayBar(label: String, rate: Float, accent: Color, animate: Boolean) {
    val animated by animateFloatAsState(
        targetValue = rate.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = if (animate) 620 else 0),
        label = "weekday_rate_$label"
    )
    Row(
        Modifier.fillMaxWidth().height(26.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(28.dp)
        )
        Box(
            Modifier
                .weight(1f)
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
        ) {
            if (animated > 0f) {
                Box(
                    Modifier
                        .fillMaxWidth(animated)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(4.dp))
                        .background(accent)
                )
            }
        }
        Text(
            "${(rate * 100).roundToInt()}%",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.End,
            modifier = Modifier.width(38.dp)
        )
    }
}

@Composable
private fun MilestoneStrip(currentStreak: Int, accent: Color) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        MilestoneRules.thresholds.forEach { days ->
            val reached = currentStreak >= days
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = if (reached) accent.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surface,
                border = BorderStroke(
                    1.dp,
                    if (reached) accent.copy(alpha = 0.45f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
            ) {
                Row(
                    Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (reached) {
                        Icon(AppIcons.Check, contentDescription = null, tint = accent, modifier = Modifier.size(12.dp))
                        Spacer(Modifier.width(4.dp))
                    }
                    Text(
                        days.toString(),
                        color = if (reached) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }
    }
}

@Composable
private fun CountChip(icon: ImageVector, label: String) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Row(Modifier.padding(horizontal = 9.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(13.dp))
            Spacer(Modifier.width(5.dp))
            Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun InfoCard(text: String) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surface) {
                Icon(
                    AppIcons.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(8.dp).size(18.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp, lineHeight = 19.sp)
        }
        Spacer(Modifier.height(12.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    }
}

private fun Habit.accentColor(): Color =
    runCatching { Color(android.graphics.Color.parseColor(colorHex)) }.getOrDefault(Color(0xFFE97B4A))
