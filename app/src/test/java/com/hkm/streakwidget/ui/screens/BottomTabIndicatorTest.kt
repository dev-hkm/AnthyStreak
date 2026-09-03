package com.hkm.streakwidget.ui.screens

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

class BottomTabIndicatorTest {
    @Test
    fun `keeps a spring undershoot inside the left tab boundary`() {
        assertEquals(5.dp, boundedTabIndicatorOffset(animatedOffset = (-18).dp, segmentWidth = 164.dp))
    }

    @Test
    fun `keeps a spring overshoot inside the right tab boundary`() {
        assertEquals(169.dp, boundedTabIndicatorOffset(animatedOffset = 190.dp, segmentWidth = 164.dp))
    }

    @Test
    fun `preserves an in range indicator position`() {
        assertEquals(98.dp, boundedTabIndicatorOffset(animatedOffset = 98.dp, segmentWidth = 164.dp))
    }

    @Test
    fun `uses a safe zero width range when constraints are unexpectedly narrow`() {
        assertEquals(5.dp, boundedTabIndicatorOffset(animatedOffset = 0.dp, segmentWidth = (-4).dp))
    }
}
