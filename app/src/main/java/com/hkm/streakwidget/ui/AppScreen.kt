package com.hkm.streakwidget.ui

/** Top-level destinations rendered inside the single activity. */
enum class AppScreen { HOME, SETTINGS, INSIGHTS }

data class MilestoneEvent(
    val habitId: String,
    val habitName: String,
    val days: Int
)
