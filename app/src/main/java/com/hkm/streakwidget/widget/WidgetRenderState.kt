package com.hkm.streakwidget.widget

import com.hkm.streakwidget.domain.StreakCalculator
import com.hkm.streakwidget.model.GoalType
import com.hkm.streakwidget.model.Habit

/**
 * Presentation-only widget data. Keeping this separate from RemoteViews makes the
 * streak rules deterministic and prevents the launcher layout from owning product logic.
 */
internal data class WidgetRenderState(
    val visualState: WidgetVisualState,
    val title: String,
    val streak: Int,
    val streakLabel: String,
    val status: String,
    val taskProgress: String?,
    val actionLabel: String,
    val primaryAction: WidgetPrimaryAction,
    val accentHex: String,
    val isCompleted: Boolean
)

internal enum class WidgetVisualState { EMPTY, READY, COMPLETED }

internal enum class WidgetPrimaryAction { OPEN_APP, CHECK_IN }

internal fun Habit.isWidgetChecklistEligible(): Boolean =
    goalType == GoalType.CHECKLIST && todoItems.isNotEmpty()

internal enum class WidgetIgnitionStage { ANTICIPATION, BLOOM, OVERSHOOT, SETTLED }

internal data class WidgetIgnitionFrame(
    val stage: WidgetIgnitionStage,
    val atMillis: Long,
    val outerAlpha: Int,
    val midAlpha: Int,
    val innerAlpha: Int,
    val glowAlpha: Int
)

internal object WidgetIgnitionSequence {
    fun frames(): List<WidgetIgnitionFrame> = listOf(
        WidgetIgnitionFrame(WidgetIgnitionStage.ANTICIPATION, 0L, 150, 92, 50, 24),
        WidgetIgnitionFrame(WidgetIgnitionStage.ANTICIPATION, 220L, 210, 135, 82, 70),
        WidgetIgnitionFrame(WidgetIgnitionStage.BLOOM, 520L, 255, 220, 150, 178),
        WidgetIgnitionFrame(WidgetIgnitionStage.BLOOM, 900L, 255, 255, 235, 255),
        WidgetIgnitionFrame(WidgetIgnitionStage.OVERSHOOT, 1260L, 246, 255, 255, 220),
        WidgetIgnitionFrame(WidgetIgnitionStage.OVERSHOOT, 1600L, 255, 245, 220, 176),
        WidgetIgnitionFrame(WidgetIgnitionStage.SETTLED, 1980L, 255, 235, 210, 135),
        WidgetIgnitionFrame(WidgetIgnitionStage.SETTLED, 2380L, 255, 225, 200, 105)
    )
}

internal object WidgetRenderStateFactory {
    const val FALLBACK_FLAME_HEX = "#E97B4A"

    fun selectHabit(habits: List<Habit>, focusedHabitId: String?): Habit? =
        habits.firstOrNull { it.id == focusedHabitId && !it.isArchived }
            ?: habits.firstOrNull { !it.isArchived }

    fun create(habit: Habit?, today: String = StreakCalculator.todayString()): WidgetRenderState {
        if (habit == null) {
            return WidgetRenderState(
                visualState = WidgetVisualState.EMPTY,
                title = "Start your first goal",
                streak = 0,
                streakLabel = "DAYS",
                status = "ONE PROMISE AT A TIME",
                taskProgress = null,
                actionLabel = "CREATE GOAL",
                primaryAction = WidgetPrimaryAction.OPEN_APP,
                accentHex = "#777777",
                isCompleted = false
            )
        }

        val completed = today in habit.completionHistory
        val taskProgress = habit.takeIf { it.isWidgetChecklistEligible() }
            ?.let { checklist ->
                val completedTasks = checklist.todoItems.count { it.isCompletedOn(today) }
                "$completedTasks OF ${checklist.todoItems.size} TASKS"
            }

        val streak = StreakCalculator.calculate(habit.completionHistory, habit.debts, today)
        return WidgetRenderState(
            visualState = if (completed) WidgetVisualState.COMPLETED else WidgetVisualState.READY,
            title = habit.name,
            streak = streak,
            streakLabel = if (streak == 1) {
                "DAY"
            } else {
                "DAYS"
            },
            status = if (completed) "TODAY SECURED" else "READY TODAY",
            taskProgress = taskProgress,
            actionLabel = if (completed) "TODAY SECURED" else "CHECK IN",
            primaryAction = if (completed) WidgetPrimaryAction.OPEN_APP else WidgetPrimaryAction.CHECK_IN,
            accentHex = normalizeAccent(habit.colorHex),
            isCompleted = completed
        )
    }

    fun normalizeAccent(value: String): String =
        value.trim().uppercase().takeIf { it.matches(Regex("#[0-9A-F]{6}")) } ?: FALLBACK_FLAME_HEX
}
