package com.hkm.streakwidget.widget

import com.hkm.streakwidget.model.GoalType
import com.hkm.streakwidget.model.Habit
import com.hkm.streakwidget.model.TodoItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetRenderStateTest {
    private val today = "2026-08-09"

    @Test
    fun `empty widget gives the launcher a clear create goal state`() {
        val state = WidgetRenderStateFactory.create(habit = null, today = today)

        assertEquals(WidgetVisualState.EMPTY, state.visualState)
        assertEquals("Start your first goal", state.title)
        assertEquals("CREATE GOAL", state.actionLabel)
        assertEquals(WidgetPrimaryAction.OPEN_APP, state.primaryAction)
        assertFalse(state.isCompleted)
        assertNull(state.taskProgress)
    }

    @Test
    fun `focused active goal wins and archived goals are never selected`() {
        val firstActive = habit(id = "first", name = "Read")
        val focusedActive = habit(id = "focused", name = "Workout")
        val archivedFocus = habit(id = "archived", name = "Old goal", isArchived = true)

        assertEquals(
            focusedActive,
            WidgetRenderStateFactory.selectHabit(listOf(firstActive, focusedActive, archivedFocus), focusedActive.id)
        )
        assertEquals(
            firstActive,
            WidgetRenderStateFactory.selectHabit(listOf(firstActive, archivedFocus), archivedFocus.id)
        )
        assertNull(WidgetRenderStateFactory.selectHabit(listOf(archivedFocus), archivedFocus.id))
    }

    @Test
    fun `pending goal shows its calculated streak accent and direct check in`() {
        val habit = habit(
            name = "Workout",
            colorHex = "#E97B4A",
            completionHistory = listOf("2026-08-08", "2026-08-07"),
            todoItems = listOf(
                TodoItem(title = "Squats", completedOn = today),
                TodoItem(title = "Run")
            )
        )

        val state = WidgetRenderStateFactory.create(habit, today)

        assertEquals(WidgetVisualState.READY, state.visualState)
        assertEquals(2, state.streak)
        assertEquals("Workout", state.title)
        assertEquals("READY TODAY", state.status)
        assertEquals("1 OF 2 TASKS", state.taskProgress)
        assertEquals("CHECK IN", state.actionLabel)
        assertEquals(WidgetPrimaryAction.CHECK_IN, state.primaryAction)
        assertEquals("#E97B4A", state.accentHex)
        assertFalse(state.isCompleted)
    }

    @Test
    fun `completed goal is secured and never exposes a second check in action`() {
        val habit = habit(completionHistory = listOf(today, "2026-08-08"))

        val state = WidgetRenderStateFactory.create(habit, today)

        assertEquals(WidgetVisualState.COMPLETED, state.visualState)
        assertEquals(2, state.streak)
        assertEquals("TODAY SECURED", state.status)
        assertEquals("TODAY SECURED", state.actionLabel)
        assertEquals(WidgetPrimaryAction.OPEN_APP, state.primaryAction)
        assertTrue(state.isCompleted)
    }

    @Test
    fun `invalid accent falls back to the approved flame orange`() {
        val state = WidgetRenderStateFactory.create(habit(colorHex = "definitely-not-a-color"), today)

        assertEquals("#E97B4A", state.accentHex)
    }

    @Test
    fun `checklist widget eligibility requires at least one task`() {
        val simpleGoal = habit(goalType = GoalType.GOAL)
        val emptyChecklist = habit(goalType = GoalType.CHECKLIST)
        val populatedChecklist = habit(
            goalType = GoalType.CHECKLIST,
            todoItems = listOf(TodoItem(title = "Read"))
        )

        assertFalse(simpleGoal.isWidgetChecklistEligible())
        assertFalse(emptyChecklist.isWidgetChecklistEligible())
        assertTrue(populatedChecklist.isWidgetChecklistEligible())
        assertNull(WidgetRenderStateFactory.create(emptyChecklist, today).taskProgress)
        assertEquals("0 OF 1 TASKS", WidgetRenderStateFactory.create(populatedChecklist, today).taskProgress)
    }

    @Test
    fun `widget ignition is a bounded four frame ritual ending before 750ms`() {
        val frames = WidgetIgnitionSequence.frames()

        assertEquals(
            listOf(
                WidgetIgnitionStage.ANTICIPATION,
                WidgetIgnitionStage.BLOOM,
                WidgetIgnitionStage.OVERSHOOT,
                WidgetIgnitionStage.SETTLED
            ),
            frames.map(WidgetIgnitionFrame::stage)
        )
        assertEquals(0L, frames.first().atMillis)
        assertTrue(frames.zipWithNext().all { (first, second) -> first.atMillis < second.atMillis })
        assertTrue(frames.last().atMillis in 600L..750L)
        assertTrue(frames.all { it.flameAlpha in 180..255 })
        assertTrue(frames.all { it.glowAlpha in 0..255 })
    }

    private fun habit(
        id: String = "goal",
        name: String = "Daily walk",
        colorHex: String = "#E97B4A",
        completionHistory: List<String> = emptyList(),
        todoItems: List<TodoItem> = emptyList(),
        goalType: GoalType = if (todoItems.isEmpty()) GoalType.GOAL else GoalType.CHECKLIST,
        isArchived: Boolean = false
    ) = Habit(
        id = id,
        name = name,
        startDate = "2026-08-01",
        colorHex = colorHex,
        completionHistory = completionHistory,
        goalType = goalType,
        todoItems = todoItems,
        isArchived = isArchived
    )
}
