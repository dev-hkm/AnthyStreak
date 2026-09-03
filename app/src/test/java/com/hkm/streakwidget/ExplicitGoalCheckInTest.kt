package com.hkm.streakwidget

import com.hkm.streakwidget.model.Habit
import com.hkm.streakwidget.ui.GoalCheckInState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExplicitGoalCheckInTest {
    @Test
    fun `check in changes only the requested goal when focus changes`() {
        val today = "2026-08-09"
        val first = habit(id = "first", name = "Read")
        val second = habit(id = "second", name = "Workout")
        val focusedSecond = GoalCheckInState(
            habits = listOf(first, second),
            focusedHabitId = second.id
        )

        val updated = focusedSecond.toggleToday(first.id, today)

        assertEquals(second.id, updated.focusedHabitId)
        assertTrue(today in updated.habits.single { it.id == first.id }.completionHistory)
        assertFalse(today in updated.habits.single { it.id == second.id }.completionHistory)
    }

    private fun habit(id: String, name: String) = Habit(
        id = id,
        name = name,
        startDate = "2026-08-01"
    )
}
