package com.hkm.streakwidget

import com.hkm.streakwidget.ui.components.BonsaiGrowthStage
import org.junit.Assert.assertEquals
import org.junit.Test

class BonsaiGrowthStageTest {

    @Test
    fun `returns SEED for 0 or negative streak`() {
        assertEquals(BonsaiGrowthStage.SEED, BonsaiGrowthStage.fromStreak(0))
        assertEquals(BonsaiGrowthStage.SEED, BonsaiGrowthStage.fromStreak(-1))
    }

    @Test
    fun `returns SPROUT for streak 1 to 3`() {
        assertEquals(BonsaiGrowthStage.SPROUT, BonsaiGrowthStage.fromStreak(1))
        assertEquals(BonsaiGrowthStage.SPROUT, BonsaiGrowthStage.fromStreak(2))
        assertEquals(BonsaiGrowthStage.SPROUT, BonsaiGrowthStage.fromStreak(3))
    }

    @Test
    fun `returns SAPLING for streak 4 to 7`() {
        assertEquals(BonsaiGrowthStage.SAPLING, BonsaiGrowthStage.fromStreak(4))
        assertEquals(BonsaiGrowthStage.SAPLING, BonsaiGrowthStage.fromStreak(7))
    }

    @Test
    fun `returns BONSAI for streak 8 to 20`() {
        assertEquals(BonsaiGrowthStage.BONSAI, BonsaiGrowthStage.fromStreak(8))
        assertEquals(BonsaiGrowthStage.BONSAI, BonsaiGrowthStage.fromStreak(20))
    }

    @Test
    fun `returns BLOOMING for streak 21 and above`() {
        assertEquals(BonsaiGrowthStage.BLOOMING, BonsaiGrowthStage.fromStreak(21))
        assertEquals(BonsaiGrowthStage.BLOOMING, BonsaiGrowthStage.fromStreak(100))
    }
}
