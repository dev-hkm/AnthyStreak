package com.hkm.streakwidget.domain

import com.hkm.streakwidget.R
import com.hkm.streakwidget.model.GoalType
import com.hkm.streakwidget.model.VisualTheme

data class HabitTemplate(
    val id: String,
    val nameRes: Int,
    val noteRes: Int,
    val goalType: GoalType,
    val taskRes: List<Int>,
    val colorHex: String,
    val theme: VisualTheme
)

/** A small, opinionated set of starts that never feels like a generic list. */
object HabitTemplates {
    val all: List<HabitTemplate> = listOf(
        HabitTemplate(
            id = "water",
            nameRes = R.string.template_water_title,
            noteRes = R.string.template_water_note,
            goalType = GoalType.GOAL,
            taskRes = emptyList(),
            colorHex = "#3B82F6",
            theme = VisualTheme.FLAME
        ),
        HabitTemplate(
            id = "read",
            nameRes = R.string.template_read_title,
            noteRes = R.string.template_read_note,
            goalType = GoalType.GOAL,
            taskRes = emptyList(),
            colorHex = "#8B5CF6",
            theme = VisualTheme.FLAME
        ),
        HabitTemplate(
            id = "move",
            nameRes = R.string.template_move_title,
            noteRes = R.string.template_move_note,
            goalType = GoalType.GOAL,
            taskRes = emptyList(),
            colorHex = "#F97316",
            theme = VisualTheme.FLAME
        ),
        HabitTemplate(
            id = "meditate",
            nameRes = R.string.template_meditate_title,
            noteRes = R.string.template_meditate_note,
            goalType = GoalType.GOAL,
            taskRes = emptyList(),
            colorHex = "#10B981",
            theme = VisualTheme.FLAME
        ),
        HabitTemplate(
            id = "study",
            nameRes = R.string.template_study_title,
            noteRes = R.string.template_study_note,
            goalType = GoalType.CHECKLIST,
            taskRes = listOf(
                R.string.template_study_task_review,
                R.string.template_study_task_practice,
                R.string.template_study_task_summary
            ),
            colorHex = "#EAB308",
            theme = VisualTheme.FLAME
        ),
        HabitTemplate(
            id = "sleep",
            nameRes = R.string.template_sleep_title,
            noteRes = R.string.template_sleep_note,
            goalType = GoalType.CHECKLIST,
            taskRes = listOf(
                R.string.template_sleep_task_screens,
                R.string.template_sleep_task_prepare,
                R.string.template_sleep_task_lights
            ),
            colorHex = "#6366F1",
            theme = VisualTheme.TREE
        )
    )
}
