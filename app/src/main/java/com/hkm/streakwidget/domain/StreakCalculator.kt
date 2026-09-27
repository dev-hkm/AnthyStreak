package com.hkm.streakwidget.domain

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/** Pure streak rules, kept independent from Android UI and storage. */
object StreakCalculator {
    const val MAX_AUTO_DEBTS = 2

    /**
     * Automatically grants up to [maxAutoDebts] missed days as protected debt days.
     * Any missed days beyond this limit require manual claiming with user confirmation.
     * Completed dates always win, and today is deliberately excluded because it is
     * still actionable rather than missed.
     */
    fun reconcileDebts(
        startDate: String,
        completedDates: List<String>,
        existingDebtDates: List<String>,
        today: String = todayString(),
        maxAutoDebts: Int = MAX_AUTO_DEBTS
    ): List<String> {
        val todayDate = parseDate(today) ?: return emptyList()
        val completed = completedDates.filter(::isValidDate).toSet()
        val validExisting = existingDebtDates
            .filter(::isValidDate)
            .filter { date -> parseDate(date)?.before(todayDate) == true }
            .filter { it !in completed }
            .toSet()

        val debts = validExisting.toMutableSet()

        val firstCompletedDate = completed
            .mapNotNull(::parseDate)
            .minByOrNull(Calendar::getTimeInMillis)
            ?: return debts.sorted()

        val configuredStart = parseDate(startDate)
        val cursor = when {
            configuredStart == null -> firstCompletedDate
            firstCompletedDate.before(configuredStart) -> configuredStart
            else -> firstCompletedDate
        }

        // Available auto-debt allowance
        var autoDebtsRemaining = (maxAutoDebts - debts.size).coerceAtLeast(0)

        while (cursor.before(todayDate)) {
            val date = format(cursor)
            if (date !in completed && date !in debts) {
                if (autoDebtsRemaining > 0) {
                    debts += date
                    autoDebtsRemaining -= 1
                }
            }
            cursor.add(Calendar.DAY_OF_YEAR, 1)
        }

        return (debts - completed).sorted()
    }

    /**
     * Finds past missed dates that were not automatically granted debt
     * and need manual user confirmation to claim and protect the streak.
     */
    fun getUnclaimedMissedDates(
        startDate: String,
        completedDates: List<String>,
        debts: List<String>,
        today: String = todayString()
    ): List<String> {
        val todayDate = parseDate(today) ?: return emptyList()
        val completed = completedDates.filter(::isValidDate).toSet()
        val currentDebts = debts.filter(::isValidDate).toSet()

        val firstCompletedDate = completed
            .mapNotNull(::parseDate)
            .minByOrNull(Calendar::getTimeInMillis)
            ?: return emptyList()

        val configuredStart = parseDate(startDate)
        val cursor = when {
            configuredStart == null -> firstCompletedDate
            firstCompletedDate.before(configuredStart) -> configuredStart
            else -> firstCompletedDate
        }

        val unclaimed = mutableListOf<String>()
        while (cursor.before(todayDate)) {
            val date = format(cursor)
            if (date !in completed && date !in currentDebts) {
                unclaimed += date
            }
            cursor.add(Calendar.DAY_OF_YEAR, 1)
        }
        return unclaimed.sorted()
    }

    fun calculate(
        completedDates: List<String>,
        protectedDates: List<String>,
        today: String = todayString()
    ): Int {
        val completed = completedDates.toSet()
        val protected = protectedDates.toSet()
        val calendar = parseDate(today) ?: return 0

        var streak = 0
        if (completed.contains(today)) {
            streak += 1
            calendar.add(Calendar.DAY_OF_YEAR, -1)
        } else {
            calendar.add(Calendar.DAY_OF_YEAR, -1)
            if (!completed.contains(format(calendar)) && !protected.contains(format(calendar))) return 0
        }

        while (completed.contains(format(calendar)) || protected.contains(format(calendar))) {
            streak += 1
            calendar.add(Calendar.DAY_OF_YEAR, -1)
        }
        return streak
    }

    fun todayString(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Calendar.getInstance().time)

    fun weekEndingOn(endDate: String = todayString()): List<String> {
        val calendar = parseDate(endDate) ?: return emptyList()
        calendar.add(Calendar.DAY_OF_YEAR, -6)
        return List(7) {
            format(calendar).also { calendar.add(Calendar.DAY_OF_YEAR, 1) }
        }
    }

    fun longestRun(completedDates: List<String>, protectedDates: List<String>): Int {
        val days = (completedDates + protectedDates).filter(::isValidDate).toSortedSet()
        if (days.isEmpty()) return 0

        var best = 1
        var current = 1
        for (index in 1 until days.size) {
            val previous = parseDate(days.elementAt(index - 1)) ?: continue
            previous.add(Calendar.DAY_OF_YEAR, 1)
            if (format(previous) == days.elementAt(index)) {
                current += 1
                best = maxOf(best, current)
            } else {
                current = 1
            }
        }
        return best
    }

    private fun parseDate(value: String): Calendar? = runCatching {
        val parsed = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false }.parse(value) ?: return null
        Calendar.getInstance().apply { time = parsed }
    }.getOrNull()

    private fun isValidDate(value: String) = parseDate(value) != null

    private fun format(calendar: Calendar): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(calendar.time)
}
