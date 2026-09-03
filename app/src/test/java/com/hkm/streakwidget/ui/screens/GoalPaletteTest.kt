package com.hkm.streakwidget.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GoalPaletteTest {
    @Test
    fun `palette contains 22 named unique colors`() {
        assertEquals(22, GoalPalette.options.size)
        assertEquals(22, GoalPalette.options.map { it.hex }.distinct().size)
        assertTrue(GoalPalette.options.all { it.name.isNotBlank() })
        assertEquals(22, GoalPalette.options.map { it.name }.distinct().size)
    }
}
