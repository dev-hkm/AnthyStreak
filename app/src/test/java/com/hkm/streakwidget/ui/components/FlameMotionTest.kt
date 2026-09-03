package com.hkm.streakwidget.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FlameMotionTest {
    @Test
    fun ignition_has_expected_stage_boundaries() {
        assertEquals(IgnitionStage.COMPRESS, ignitionStageAt(99))
        assertEquals(IgnitionStage.BLOOM, ignitionStageAt(100))
        assertEquals(IgnitionStage.SETTLED, ignitionStageAt(1800))
    }

    @Test
    fun motion_is_bounded_and_deterministic() {
        val frame = ignitionFrameAt(420)

        assertTrue(frame.scaleY in .93f..1.14f)
        assertEquals(steadyFrameAt(1234, 7), steadyFrameAt(1234, 7))
    }

    @Test
    fun ignition_clock_tracks_raw_wall_clock_milliseconds() {
        assertEquals(1_800L, flameElapsedMillisAt(2_000_000_000L, 3_800_000_000L))
    }
}
