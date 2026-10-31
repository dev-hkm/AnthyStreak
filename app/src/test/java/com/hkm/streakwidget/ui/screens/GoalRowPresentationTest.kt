package com.hkm.streakwidget.ui.screens

import com.hkm.streakwidget.model.GoalType
import com.hkm.streakwidget.model.Habit
import com.hkm.streakwidget.model.TodoItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GoalRowPresentationTest {
    private val today = "2026-08-10"

    @Test
    fun `checklist presentation reports daily progress`() {
        val presentation = goalRowPresentation(
            Habit(
                name = "Morning reset",
                startDate = today,
                goalType = GoalType.CHECKLIST,
                todoItems = listOf(
                    TodoItem(title = "Water", completedOn = today),
                    TodoItem(title = "Walk"),
                    TodoItem(title = "Journal")
                )
            ),
            today
        )

        assertEquals("1 of 3 completed", presentation.statusText)
        assertFalse(presentation.isCompleted)
    }

    @Test
    fun `completed goal uses completion text`() {
        val presentation = goalRowPresentation(
            Habit(name = "Read", startDate = today, completionHistory = listOf(today)),
            today
        )

        assertEquals("Completed today", presentation.statusText)
        assertTrue(presentation.isCompleted)
    }

    @Test
    fun `completed checklist reports all tasks complete`() {
        val presentation = goalRowPresentation(
            Habit(
                name = "Evening reset",
                startDate = today,
                goalType = GoalType.CHECKLIST,
                todoItems = listOf(
                    TodoItem(title = "Tidy", completedOn = today),
                    TodoItem(title = "Plan", completedOn = today)
                )
            ),
            today
        )

        assertEquals("All 2 completed", presentation.statusText)
        assertTrue(presentation.isCompleted)
    }

    @Test
    fun `streak metadata uses singular and plural day labels`() {
        val oneDay = goalRowPresentation(Habit(name = "Read", startDate = today, streakCount = 1), today)
        val manyDays = goalRowPresentation(Habit(name = "Run", startDate = today, streakCount = 4), today)

        assertEquals("1 day", oneDay.streakText)
        assertEquals("4 days", manyDays.streakText)
    }

    @Test
    fun `paused goal row uses calm pause copy without completing the day`() {
        val presentation = goalRowPresentation(
            Habit(
                name = "Read",
                startDate = today,
                streakCount = 6,
                pausedDates = listOf(today)
            ),
            today
        )

        assertEquals("Paused today", presentation.statusText)
        assertEquals("6 days", presentation.streakText)
        assertFalse(presentation.isCompleted)
    }

    @Test
    fun `archived row tap maps to manage and never restore`() {
        val presentation = goalRowPresentation(
            Habit(name = "Archived goal", startDate = today, isArchived = true),
            today
        )

        assertEquals(GoalRowPrimaryAction.MANAGE, presentation.primaryAction)
    }
}
