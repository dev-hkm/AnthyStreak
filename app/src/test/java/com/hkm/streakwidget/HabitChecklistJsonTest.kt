package com.hkm.streakwidget

import com.hkm.streakwidget.model.Habit
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HabitChecklistJsonTest {
    @Test
    fun `round trips checklist tasks including daily completion date`() {
        val source = JSONObject()
            .put("id", "workout")
            .put("name", "Workout")
            .put("startDate", "2026-08-09")
            .put("goalType", "CHECKLIST")
            .put("todoItems", JSONArray()
                .put(JSONObject().put("id", "squats").put("title", "3 sets of squats").put("completedOn", "2026-08-09"))
                .put(JSONObject().put("id", "walk").put("title", "10 minute cooldown").put("completedOn", JSONObject.NULL)))

        val roundTripped = Habit.fromJsonObject(source).toJsonObject()

        assertEquals("CHECKLIST", roundTripped.getString("goalType"))
        val tasks = roundTripped.getJSONArray("todoItems")
        assertEquals("3 sets of squats", tasks.getJSONObject(0).getString("title"))
        assertEquals("2026-08-09", tasks.getJSONObject(0).getString("completedOn"))
        assertTrue(tasks.getJSONObject(1).isNull("completedOn"))
    }

    @Test
    fun `imports legacy goal JSON without a checklist`() {
        val legacy = JSONObject()
            .put("id", "legacy")
            .put("name", "Read")
            .put("startDate", "2026-08-09")

        val restored = Habit.fromJsonObject(legacy)

        val exportedAgain = restored.toJsonObject()

        assertEquals("GOAL", exportedAgain.getString("goalType"))
        assertEquals(0, exportedAgain.getJSONArray("todoItems").length())
    }

    @Test
    fun `round trips streak notes for backup`() {
        val source = JSONObject()
            .put("id", "reading")
            .put("name", "Read")
            .put("startDate", "2026-08-09")
            .put("notes", JSONArray().put(
                JSONObject()
                    .put("id", "note-1")
                    .put("date", "2026-08-10")
                    .put("text", "Finished chapter four")
                    .put("createdAt", 123L)
            ))

        val roundTripped = Habit.fromJsonObject(source).toJsonObject()
        val note = roundTripped.getJSONArray("notes").getJSONObject(0)

        assertEquals("2026-08-10", note.getString("date"))
        assertEquals("Finished chapter four", note.getString("text"))
        assertEquals(123L, note.getLong("createdAt"))
    }
}
