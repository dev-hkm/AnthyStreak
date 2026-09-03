package com.hkm.streakwidget

import com.hkm.streakwidget.domain.StreakCalculator
import org.junit.Assert.assertEquals
import org.junit.Test

class StreakCalculatorTest {
    private val today = "2026-08-09"

    @Test
    fun `counts today and consecutive prior check-ins`() {
        val result = StreakCalculator.calculate(
            completedDates = listOf("2026-08-09", "2026-08-08", "2026-08-07"),
            protectedDates = emptyList(),
            today = today
        )

        assertEquals(3, result)
    }

    @Test
    fun `keeps yesterday streak before today is checked in`() {
        val result = StreakCalculator.calculate(
            completedDates = listOf("2026-08-08", "2026-08-07"),
            protectedDates = emptyList(),
            today = today
        )

        assertEquals(2, result)
    }

    @Test
    fun `returns zero after an unprotected missed day`() {
        val result = StreakCalculator.calculate(
            completedDates = listOf("2026-08-07"),
            protectedDates = emptyList(),
            today = today
        )

        assertEquals(0, result)
    }

    @Test
    fun `finds the longest completed run for a habit history`() {
        val result = StreakCalculator.longestRun(
            completedDates = listOf("2026-08-01", "2026-08-02", "2026-08-04", "2026-08-05", "2026-08-06"),
            protectedDates = emptyList()
        )

        assertEquals(3, result)
    }

    @Test
    fun `builds a seven day history ending today`() {
        val result = StreakCalculator.weekEndingOn(today)

        assertEquals(listOf("2026-08-03", "2026-08-04", "2026-08-05", "2026-08-06", "2026-08-07", "2026-08-08", "2026-08-09"), result)
    }
}
