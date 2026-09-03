package com.hkm.streakwidget.ui.screens

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

internal fun boundedTabIndicatorOffset(animatedOffset: Dp, segmentWidth: Dp): Dp {
    val startInset = 5.dp
    val safeSegmentWidth = segmentWidth.coerceAtLeast(0.dp)
    return animatedOffset.coerceIn(startInset, safeSegmentWidth + startInset)
}
