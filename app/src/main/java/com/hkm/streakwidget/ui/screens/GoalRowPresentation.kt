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

internal data class GoalRowCopy(
    val archived: String = "Archived",
    val allCompleted: String = "All %1\$d completed",
    val progress: String = "%1\$d of %2\$d completed",
    val completedToday: String = "Completed today",
    val readyToday: String = "Ready for today",
    val pausedToday: String = "Paused today",
    val day: String = "day",
    val days: String = "days"
) {
    companion object {
        val English = GoalRowCopy()
    }
}

internal fun goalRowPresentation(
    habit: Habit,
    today: String,
    copy: GoalRowCopy = GoalRowCopy.English
): GoalRowPresentation {
    val completedTodos = habit.todoItems.count { it.isCompletedOn(today) }
    val checklistComplete = habit.goalType == GoalType.CHECKLIST &&
        habit.todoItems.isNotEmpty() && completedTodos == habit.todoItems.size
    val completed = if (habit.goalType == GoalType.CHECKLIST) {
        checklistComplete
    } else {
        today in habit.completionHistory
    }
    val status = when {
        habit.isArchived -> copy.archived
        habit.goalType == GoalType.CHECKLIST && checklistComplete ->
            String.format(copy.allCompleted, completedTodos)
        habit.isPausedOn(today) -> copy.pausedToday
        habit.goalType == GoalType.CHECKLIST ->
            String.format(copy.progress, completedTodos, habit.todoItems.size)
        completed -> copy.completedToday
        else -> copy.readyToday
    }
    val dayLabel = if (habit.streakCount == 1) copy.day else copy.days

    return GoalRowPresentation(
        statusText = status,
        streakText = "${habit.streakCount} $dayLabel",
        isCompleted = completed && !habit.isArchived,
        primaryAction = if (habit.isArchived) GoalRowPrimaryAction.MANAGE else GoalRowPrimaryAction.OPEN
    )
}
