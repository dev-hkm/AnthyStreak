package com.hkm.streakwidget

import com.hkm.streakwidget.domain.StreakCalculator
import org.junit.Assert.assertEquals
import org.junit.Test

class PauseModeTest {
    private val today = "2026-08-09"

    @Test
    fun `paused yesterday stays transparent to the streak`() {
        val result = StreakCalculator.calculate(
            completedDates = listOf("2026-08-07", "2026-08-06"),
            protectedDates = emptyList(),
            today = today,
            pausedDates = listOf("2026-08-08")
        )

        assertEquals(2, result)
    }

    @Test
    fun `pausing today keeps the earned streak before check in`() {
        val result = StreakCalculator.calculate(
            completedDates = listOf("2026-08-08", "2026-08-07"),
            protectedDates = emptyList(),
            today = today,
            pausedDates = listOf(today)
        )

        assertEquals(2, result)
    }

    @Test
    fun `paused days never inflate the streak count`() {
        val result = StreakCalculator.calculate(
            completedDates = listOf("2026-08-08"),
            protectedDates = emptyList(),
            today = today,
            pausedDates = listOf("2026-08-08")
        )

        assertEquals(0, result)
    }

    @Test
    fun `reconciliation never creates rescue debt for a paused day`() {
        val debts = StreakCalculator.reconcileDebts(
            startDate = "2026-08-01",
            completedDates = listOf("2026-08-01"),
            existingDebtDates = emptyList(),
            today = "2026-08-04",
            pausedDates = listOf("2026-08-02")
        )

        assertEquals(listOf("2026-08-03"), debts)
    }

    @Test
    fun `paused days are never offered as unclaimed missed days`() {
        val unclaimed = StreakCalculator.getUnclaimedMissedDates(
            startDate = "2026-08-01",
            completedDates = listOf("2026-08-01"),
            debts = emptyList(),
            today = "2026-08-04",
            pausedDates = listOf("2026-08-02")
        )

        assertEquals(listOf("2026-08-03"), unclaimed)
    }

    @Test
    fun `longest run continues across a paused day`() {
        val result = StreakCalculator.longestRun(
            completedDates = listOf("2026-08-01", "2026-08-03"),
            protectedDates = emptyList(),
            pausedDates = listOf("2026-08-02")
        )

        assertEquals(2, result)
    }
}
