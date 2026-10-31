package com.hkm.streakwidget

import com.hkm.streakwidget.domain.MilestoneRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MilestoneRulesTest {
    @Test
    fun `reached returns the highest earned threshold`() {
        assertNull(MilestoneRules.reached(2))
        assertEquals(3, MilestoneRules.reached(3))
        assertEquals(7, MilestoneRules.reached(13))
        assertEquals(30, MilestoneRules.reached(30))
        assertEquals(365, MilestoneRules.reached(400))
    }

    @Test
    fun `crossed fires only on the first check in that reaches a threshold`() {
        assertEquals(3, MilestoneRules.crossed(previousStreak = 2, currentStreak = 3))
        assertNull(MilestoneRules.crossed(previousStreak = 3, currentStreak = 6))
        assertEquals(3, MilestoneRules.crossed(previousStreak = 2, currentStreak = 9))
    }

    @Test
    fun `next points at the first threshold still ahead`() {
        assertEquals(3, MilestoneRules.next(0))
        assertEquals(14, MilestoneRules.next(7))
        assertEquals(365, MilestoneRules.next(200))
        assertNull(MilestoneRules.next(365))
    }

    @Test
    fun `threshold list stays ordered and unique`() {
        assertEquals(MilestoneRules.thresholds.sorted(), MilestoneRules.thresholds)
        assertEquals(MilestoneRules.thresholds.distinct(), MilestoneRules.thresholds)
    }
}
