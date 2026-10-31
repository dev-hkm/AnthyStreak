package com.hkm.streakwidget

import com.hkm.streakwidget.domain.HabitTemplates
import com.hkm.streakwidget.model.GoalType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HabitTemplatesTest {
    @Test
    fun `templates keep unique ids`() {
        val ids = HabitTemplates.all.map { it.id }

        assertEquals(ids.distinct(), ids)
    }

    @Test
    fun `checklist templates ship their daily tasks and goals stay single actions`() {
        HabitTemplates.all.forEach { template ->
            if (template.goalType == GoalType.CHECKLIST) {
                assertTrue("${template.id} needs tasks", template.taskRes.isNotEmpty())
            } else {
                assertTrue("${template.id} should stay a single action", template.taskRes.isEmpty())
            }
        }
    }

    @Test
    fun `template accents are valid colors`() {
        HabitTemplates.all.forEach { template ->
            assertTrue(template.id, Regex("^#[0-9A-Fa-f]{6}$").matches(template.colorHex))
        }
    }
}
