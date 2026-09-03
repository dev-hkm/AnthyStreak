package com.hkm.streakwidget.domain

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/** Pure streak rules, kept independent from Android UI and storage. */
object StreakCalculator {
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
