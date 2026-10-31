package com.hkm.streakwidget

import com.hkm.streakwidget.domain.InsightDayState
import com.hkm.streakwidget.domain.InsightsCalculator
import com.hkm.streakwidget.model.Habit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InsightsCalculatorTest {
    private val today = "2026-08-09" // Sunday

    @Test
    fun `heatmap covers eight Monday aligned weeks ending today`() {
        val summary = InsightsCalculator.summarize(habit(), today)

        assertEquals(56, summary.heatmap.size)
        assertEquals("2026-06-15", summary.heatmap.first().date)
        assertEquals(today, summary.heatmap.last().date)
        assertTrue(summary.heatmap.zipWithNext().all { (first, second) ->
            first.date < second.date
        })
    }

    @Test
    fun `heatmap distinguishes completed protected paused missed and inactive days`() {
        val summary = InsightsCalculator.summarize(habit(), today)
        val stateByDate = summary.heatmap.associate { it.date to it.state }

        assertEquals(InsightDayState.INACTIVE, stateByDate["2026-07-31"])
        assertEquals(InsightDayState.COMPLETED, stateByDate["2026-08-01"])
        assertEquals(InsightDayState.MISSED, stateByDate["2026-08-02"])
        assertEquals(InsightDayState.PAUSED, stateByDate["2026-08-06"])
        assertEquals(InsightDayState.PROTECTED, stateByDate["2026-08-07"])
        assertEquals(InsightDayState.COMPLETED, stateByDate["2026-08-08"])
    }

    @Test
    fun `summary reports streaks consistency and open rescue days`() {
        val summary = InsightsCalculator.summarize(habit(), today)

        assertEquals(2, summary.totalCheckIns)
        assertEquals(2, summary.windowCheckIns)
        assertEquals(2, summary.currentStreak)
        assertEquals(2, summary.bestRun)
        assertEquals(8, summary.activeDays)
        assertEquals(25, summary.consistencyPercent)
        assertEquals(1, summary.debtCount)
        assertEquals(1, summary.pauseCount)
    }

    @Test
    fun `weekday rates use Monday first weekday buckets`() {
        val summary = InsightsCalculator.summarize(habit(), today)

        assertEquals(1f, summary.weekdayRates[5], 0.001f) // Saturday
        assertEquals(0f, summary.weekdayRates[4], 0.001f) // Friday
        assertEquals(0f, summary.weekdayRates[6], 0.001f) // Sunday
    }

    @Test
    fun `invalid today falls back to an empty summary`() {
        val summary = InsightsCalculator.summarize(habit(), "not-a-date")

        assertEquals(InsightsCalculator.emptySummary(), summary)
    }

    private fun habit() = Habit(
        name = "Read",
        startDate = "2026-08-01",
        completionHistory = listOf("2026-08-01", "2026-08-08"),
        debts = listOf("2026-08-07"),
        pausedDates = listOf("2026-08-06")
    )
}
