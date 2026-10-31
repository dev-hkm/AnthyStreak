package com.hkm.streakwidget.domain

import com.hkm.streakwidget.model.Habit
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

enum class InsightDayState { COMPLETED, PROTECTED, PAUSED, MISSED, INACTIVE }

data class InsightDay(val date: String, val state: InsightDayState)

data class InsightsSummary(
    val heatmap: List<InsightDay>,
    val totalCheckIns: Int,
    val windowCheckIns: Int,
    val currentStreak: Int,
    val bestRun: Int,
    val consistencyPercent: Int,
    val weekdayRates: List<Float>,
    val debtCount: Int,
    val pauseCount: Int,
    val activeDays: Int
)

/** Pure statistics over a habit, independent from Android and storage. */
object InsightsCalculator {
    private const val HEATMAP_WEEKS = 8

    fun summarize(habit: Habit, today: String = StreakCalculator.todayString()): InsightsSummary {
        val todayDate = parse(today) ?: return emptySummary()
        val start = parse(habit.startDate)
        val completed = habit.completionHistory.filter(::isValid).toSet()
        val debt = habit.debts.filter(::isValid).toSet()
        val paused = habit.pausedDates.filter(::isValid).toSet()

        val heatmap = mutableListOf<InsightDay>()
        val cursor = (todayDate.clone() as Calendar).apply {
            add(Calendar.DAY_OF_YEAR, -mondayFirstIndex(this))
            add(Calendar.DAY_OF_YEAR, -(HEATMAP_WEEKS - 1) * 7)
        }
        val weeklyCompleted = IntArray(7)
        val weeklyActive = IntArray(7)
        var activeDays = 0

        while (!cursor.after(todayDate)) {
            val date = format(cursor)
            val beforeStart = start == null || cursor.before(start)
            val state = when {
                date in paused -> InsightDayState.PAUSED
                date in completed -> InsightDayState.COMPLETED
                date in debt -> InsightDayState.PROTECTED
                beforeStart -> InsightDayState.INACTIVE
                else -> InsightDayState.MISSED
            }
            heatmap += InsightDay(date, state)
            if (!beforeStart && state != InsightDayState.PAUSED) {
                activeDays += 1
                val weekday = mondayFirstIndex(cursor)
                weeklyActive[weekday] += 1
                if (state == InsightDayState.COMPLETED) weeklyCompleted[weekday] += 1
            }
            cursor.add(Calendar.DAY_OF_YEAR, 1)
        }

        val completedInWindow = heatmap.count { it.state == InsightDayState.COMPLETED }
        val consistency = if (activeDays == 0) 0 else ((completedInWindow * 100f) / activeDays).toInt()
        val weekdayRates = List(7) { index ->
            if (weeklyActive[index] == 0) 0f else weeklyCompleted[index].toFloat() / weeklyActive[index]
        }

        return InsightsSummary(
            heatmap = heatmap,
            totalCheckIns = completed.size,
            windowCheckIns = completedInWindow,
            currentStreak = StreakCalculator.calculate(
                habit.completionHistory,
                habit.debts,
                today,
                habit.pausedDates
            ),
            bestRun = StreakCalculator.longestRun(habit.completionHistory, habit.debts, habit.pausedDates),
            consistencyPercent = consistency.coerceIn(0, 100),
            weekdayRates = weekdayRates,
            debtCount = debt.size,
            pauseCount = paused.size,
            activeDays = activeDays
        )
    }

    fun emptySummary(): InsightsSummary = InsightsSummary(
        heatmap = emptyList(),
        totalCheckIns = 0,
        windowCheckIns = 0,
        currentStreak = 0,
        bestRun = 0,
        consistencyPercent = 0,
        weekdayRates = List(7) { 0f },
        debtCount = 0,
        pauseCount = 0,
        activeDays = 0
    )

    private fun mondayFirstIndex(calendar: Calendar): Int = when (calendar.get(Calendar.DAY_OF_WEEK)) {
        Calendar.MONDAY -> 0
        Calendar.TUESDAY -> 1
        Calendar.WEDNESDAY -> 2
        Calendar.THURSDAY -> 3
        Calendar.FRIDAY -> 4
        Calendar.SATURDAY -> 5
        else -> 6
    }

    private fun parse(value: String): Calendar? = runCatching {
        val parsed = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            .apply { isLenient = false }
            .parse(value) ?: return null
        Calendar.getInstance().apply { time = parsed }
    }.getOrNull()

    private fun isValid(value: String): Boolean = parse(value) != null

    private fun format(calendar: Calendar): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(calendar.time)
}
