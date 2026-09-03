package com.hkm.streakwidget.ui.screens

import com.hkm.streakwidget.model.GoalType
import com.hkm.streakwidget.model.Habit

internal enum class GoalRowPrimaryAction {
    OPEN,
    MANAGE
}

internal data class GoalRowPresentation(
    val statusText: String,
    val streakText: String,
    val isCompleted: Boolean,
    val primaryAction: GoalRowPrimaryAction
)

internal fun goalRowPresentation(habit: Habit, today: String): GoalRowPresentation {
    val completedTodos = habit.todoItems.count { it.isCompletedOn(today) }
    val checklistComplete = habit.goalType == GoalType.CHECKLIST &&
        habit.todoItems.isNotEmpty() && completedTodos == habit.todoItems.size
    val completed = if (habit.goalType == GoalType.CHECKLIST) {
        checklistComplete
    } else {
        today in habit.completionHistory
    }
    val status = when {
        habit.isArchived -> "Archived"
        habit.goalType == GoalType.CHECKLIST && checklistComplete -> "All $completedTodos completed"
        habit.goalType == GoalType.CHECKLIST -> "$completedTodos of ${habit.todoItems.size} completed"
        completed -> "Completed today"
        else -> "Ready for today"
    }
    val dayLabel = if (habit.streakCount == 1) "day" else "days"

    return GoalRowPresentation(
        statusText = status,
        streakText = "${habit.streakCount} $dayLabel",
        isCompleted = completed && !habit.isArchived,
        primaryAction = if (habit.isArchived) GoalRowPrimaryAction.MANAGE else GoalRowPrimaryAction.OPEN
    )
}
