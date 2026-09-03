package com.hkm.streakwidget.ui.components

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin

enum class IgnitionStage {
    COMPRESS,
    BLOOM,
    RECOIL,
    SETTLE,
    SETTLED
}

data class FlameMotionFrame(
    val scaleX: Float = 1f,
    val scaleY: Float = 1f,
    val tipOffsetX: Float = 0f,
    val shoulderOffsetX: Float = 0f,
    val glowScale: Float = 1f,
    val emberProgress: Float = 0f,
    val haloAlpha: Float = 1f,
    val ringProgress: Float = 0f,
    val ringAlpha: Float = 0f,
    val sparkProgress: Float = 0f
)

internal fun flameElapsedMillisAt(startedAtNanos: Long, nowNanos: Long): Long =
    if (nowNanos <= startedAtNanos) 0L else (nowNanos - startedAtNanos) / 1_000_000L

fun ignitionStageAt(elapsedMs: Int): IgnitionStage = when (elapsedMs.coerceAtLeast(0)) {
    in 0 until 100 -> IgnitionStage.COMPRESS
    in 100 until 420 -> IgnitionStage.BLOOM
    in 420 until 760 -> IgnitionStage.RECOIL
    in 760 until 1800 -> IgnitionStage.SETTLE
    else -> IgnitionStage.SETTLED
}

fun ignitionFrameAt(elapsedMs: Int): FlameMotionFrame {
    val time = elapsedMs.coerceAtLeast(0)
    val emberProgress = ((time - 110).coerceAtLeast(0) % 900) / 900f
    val sparkProgress = ((time - 150).coerceAtLeast(0) % 1_050) / 1_050f

    return when (ignitionStageAt(time)) {
        IgnitionStage.COMPRESS -> {
            val progress = smoothStep(time / 100f)
            FlameMotionFrame(
                scaleX = lerp(1f, 1.045f, progress),
                scaleY = lerp(1f, .91f, progress),
                glowScale = lerp(.86f, .7f, progress),
                haloAlpha = lerp(.36f, .24f, progress),
                emberProgress = emberProgress,
                sparkProgress = sparkProgress
            )
        }

        IgnitionStage.BLOOM -> {
            val progress = smoothStep((time - 100) / 320f)
            FlameMotionFrame(
                scaleX = lerp(1.045f, .93f, progress),
                scaleY = lerp(.91f, 1.14f, progress),
                tipOffsetX = lerp(0f, .028f, progress),
                shoulderOffsetX = lerp(0f, -.018f, progress),
                glowScale = lerp(.7f, 1.34f, progress),
                haloAlpha = lerp(.24f, 1f, progress),
                ringProgress = progress * .42f,
                ringAlpha = (1f - progress * .18f).coerceIn(0f, 1f),
                emberProgress = emberProgress,
                sparkProgress = sparkProgress
            )
        }

        IgnitionStage.RECOIL -> {
            val progress = smoothStep((time - 420) / 340f)
            FlameMotionFrame(
                scaleX = lerp(.93f, 1.035f, progress),
                scaleY = lerp(1.14f, .98f, progress),
                tipOffsetX = lerp(.028f, -.022f, progress),
                shoulderOffsetX = lerp(-.018f, .012f, progress),
                glowScale = lerp(1.34f, .96f, progress),
                haloAlpha = lerp(1f, .62f, progress),
                ringProgress = lerp(.42f, .82f, progress),
                ringAlpha = lerp(.82f, .42f, progress),
                emberProgress = emberProgress,
                sparkProgress = sparkProgress
            )
        }

        IgnitionStage.SETTLE -> {
            val progress = ((time - 760) / 1040f).coerceIn(0f, 1f)
            val envelope = exp(-4.2f * progress)
            val wave = sin(progress * PI.toFloat() * 4.2f)
            FlameMotionFrame(
                scaleX = (1f + .034f * envelope * wave).coerceIn(.96f, 1.04f),
                scaleY = (1f - .044f * envelope * wave).coerceIn(.95f, 1.05f),
                tipOffsetX = (.021f * envelope * cos(progress * PI.toFloat() * 4f)).coerceIn(-.021f, .021f),
                shoulderOffsetX = (-.011f * envelope * wave).coerceIn(-.011f, .011f),
                glowScale = (1f + .075f * envelope * wave).coerceIn(.92f, 1.08f),
                haloAlpha = lerp(.62f, .36f, progress),
                ringProgress = lerp(.82f, 1f, progress),
                ringAlpha = lerp(.42f, 0f, progress),
                emberProgress = emberProgress,
                sparkProgress = sparkProgress
            )
        }

        IgnitionStage.SETTLED -> steadyFrameAt(time.toLong(), seed = 0)
    }
}

fun steadyFrameAt(elapsedMs: Long, seed: Int): FlameMotionFrame {
    val time = elapsedMs.coerceAtLeast(0L).toDouble()
    val seedPhase = seed * 0.6180339887498948
    val first = sin(time * (2.0 * PI / 1_537.0) + seedPhase)
    val second = sin(time * (2.0 * PI / 2_473.0) + seedPhase * 1.7 + 0.83)
    val emberProgress = (elapsedMs.coerceAtLeast(0L) % 1_700L) / 1_700f

    return FlameMotionFrame(
        scaleX = (1.0 + first * .010 - second * .005).toFloat().coerceIn(.985f, 1.015f),
        scaleY = (1.0 + first * .017 + second * .007).toFloat().coerceIn(.976f, 1.024f),
        tipOffsetX = (first * .025 + second * .012).toFloat().coerceIn(-.037f, .037f),
        shoulderOffsetX = (second * .011 - first * .006).toFloat().coerceIn(-.017f, .017f),
        glowScale = (1.0 + first * .025 + second * .015).toFloat().coerceIn(.96f, 1.04f),
        emberProgress = emberProgress
    )
}

private fun smoothStep(value: Float): Float {
    val bounded = value.coerceIn(0f, 1f)
    return bounded * bounded * (3f - 2f * bounded)
}

private fun lerp(start: Float, end: Float, progress: Float): Float =
    start + (end - start) * progress.coerceIn(0f, 1f)
