package com.hkm.streakwidget.ui.components

import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Predictable back gesture for an in-app destination. Progress is exposed so the
 * screen can move with the finger, exactly like a native push/pop.
 */
@Composable
fun AppPredictiveBackHandler(
    enabled: Boolean,
    onBack: () -> Unit,
    onProgress: (Float) -> Unit = {}
) {
    LaunchedEffect(enabled) {
        if (!enabled) onProgress(0f)
    }
    PredictiveBackHandler(enabled = enabled) { events ->
        try {
            events.collect { event -> onProgress(event.progress.coerceIn(0f, 1f)) }
            onBack()
        } finally {
            onProgress(0f)
        }
    }
}

fun Modifier.appPredictiveBackVisual(
    progress: Float,
    maxTranslationDp: Dp = 72.dp,
    minScale: Float = 0.94f,
    minAlpha: Float = 0.92f
): Modifier = graphicsLayer {
    val p = progress.coerceIn(0f, 1f)
    translationX = p * maxTranslationDp.toPx()
    scaleX = 1f - (1f - minScale) * p
    scaleY = 1f - (1f - minScale) * p
    alpha = 1f - (1f - minAlpha) * p
}
