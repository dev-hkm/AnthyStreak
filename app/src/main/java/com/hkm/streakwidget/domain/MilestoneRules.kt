package com.hkm.streakwidget.domain

/** Streak milestones are deliberately few so each one still feels meaningful. */
object MilestoneRules {
    val thresholds: List<Int> = listOf(3, 7, 14, 30, 60, 100, 180, 365)

    fun reached(streak: Int): Int? = thresholds.lastOrNull { it <= streak }

    /** A celebration belongs to the first check-in that crosses a threshold. */
    fun crossed(previousStreak: Int, currentStreak: Int): Int? =
        thresholds.firstOrNull { it > previousStreak && it <= currentStreak }

    fun next(streak: Int): Int? = thresholds.firstOrNull { it > streak }
}
