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

    @Test
    fun `reconciles missed past dates without debting today`() {
        val result = StreakCalculator.reconcileDebts(
            startDate = "2026-08-07",
            completedDates = listOf("2026-08-07"),
            existingDebtDates = emptyList(),
            today = "2026-08-10"
        )

        assertEquals(listOf("2026-08-08", "2026-08-09"), result)
    }

    @Test
    fun `preserves existing debts and never duplicates completed dates`() {
        val result = StreakCalculator.reconcileDebts(
            startDate = "2026-08-07",
            completedDates = listOf("2026-08-07", "2026-08-09"),
            existingDebtDates = listOf("2026-08-08", "2026-08-08"),
            today = "2026-08-10"
        )

        assertEquals(listOf("2026-08-08"), result)
    }

    @Test
    fun `keeps streak continuous across a reconciled missed day`() {
        val debts = StreakCalculator.reconcileDebts(
            startDate = "2026-08-07",
            completedDates = listOf("2026-08-07", "2026-08-09"),
            existingDebtDates = emptyList(),
            today = "2026-08-10"
        )

        assertEquals(3, StreakCalculator.calculate(
            completedDates = listOf("2026-08-07", "2026-08-09"),
            protectedDates = debts,
            today = "2026-08-10"
        ))
    }

    @Test
    fun `does not create debt before the first check in`() {
        val result = StreakCalculator.reconcileDebts(
            startDate = "2026-08-07",
            completedDates = emptyList(),
            existingDebtDates = emptyList(),
            today = "2026-08-10"
        )

        assertEquals(emptyList<String>(), result)
    }

    @Test
    fun `limits auto debts to maximum of two days`() {
        // Start on Aug 1 (checked in). Today is Aug 5.
        // Missed Aug 2, Aug 3, Aug 4 (3 days missed).
        val debts = StreakCalculator.reconcileDebts(
            startDate = "2026-08-01",
            completedDates = listOf("2026-08-01"),
            existingDebtDates = emptyList(),
            today = "2026-08-05"
        )

        // Only first 2 days are auto-debts
        assertEquals(listOf("2026-08-02", "2026-08-03"), debts)

        // Aug 4 is unclaimed and requires user action
        val unclaimed = StreakCalculator.getUnclaimedMissedDates(
            startDate = "2026-08-01",
            completedDates = listOf("2026-08-01"),
            debts = debts,
            today = "2026-08-05"
        )
        assertEquals(listOf("2026-08-04"), unclaimed)

        // Streak is 0 before claiming Aug 4 because yesterday (Aug 4) was missed and unprotected
        val streakBeforeClaim = StreakCalculator.calculate(
            completedDates = listOf("2026-08-01"),
            protectedDates = debts,
            today = "2026-08-05"
        )
        assertEquals(0, streakBeforeClaim)

        // After user claims Aug 4 as debt, streak is saved and restored!
        val fullDebts = debts + unclaimed
        val streakAfterClaim = StreakCalculator.calculate(
            completedDates = listOf("2026-08-01"),
            protectedDates = fullDebts,
            today = "2026-08-05"
        )
        assertEquals(4, streakAfterClaim)
    }
}
