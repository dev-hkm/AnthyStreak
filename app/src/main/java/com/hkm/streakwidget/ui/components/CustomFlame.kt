package com.hkm.streakwidget.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.isActive
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

enum class FlameState {
    IDLE,
    IGNITING,
    ACTIVE,
    COMPLETED,
    BROKEN
}

@Composable
fun CustomFlame(
    state: FlameState,
    modifier: Modifier = Modifier.size(160.dp),
    animationScale: Float = 1f,
    flameColor: Color = Color(0xFFFF5722),
    ignitionEventKey: Any? = state
) {
    val elapsedMs = rememberFlameClock(state, ignitionEventKey)
    val motion = if (state == FlameState.IGNITING) {
        ignitionFrameAt(elapsedMs.coerceAtMost(Int.MAX_VALUE.toLong()).toInt())
    } else {
        val safeScale = animationScale.takeIf(Float::isFinite)?.coerceAtLeast(.01f) ?: 1f
        steadyFrameAt((elapsedMs * safeScale).toLong(), state.ordinal)
    }

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height

        val outerColors = when (state) {
            FlameState.IDLE -> listOf(Color(0xFF555555), Color(0xFF333333))
            FlameState.IGNITING -> listOf(flameColor.lighten(0.16f), flameColor.darken(0.12f))
            FlameState.ACTIVE -> listOf(flameColor, flameColor.darken(0.18f))
            FlameState.COMPLETED -> listOf(flameColor.lighten(0.22f), flameColor)
            FlameState.BROKEN -> listOf(Color(0xFF555555), Color(0xFF303030))
        }

        val innerColors = when (state) {
            FlameState.IDLE -> listOf(Color(0xFF777777), Color(0xFF555555))
            FlameState.IGNITING -> listOf(Color.White, flameColor.lighten(0.42f))
            FlameState.ACTIVE -> listOf(flameColor.lighten(0.68f), flameColor.lighten(0.28f))
            FlameState.COMPLETED -> listOf(Color.White, flameColor.lighten(0.5f))
            FlameState.BROKEN -> listOf(Color(0xFF7A7A7A), Color(0xFF4A4A4A))
        }

        val glowColor = when (state) {
            FlameState.IDLE -> Color.Transparent
            FlameState.IGNITING -> flameColor.copy(alpha = 0.52f)
            FlameState.ACTIVE -> flameColor.copy(alpha = 0.2f)
            FlameState.COMPLETED -> flameColor.lighten(0.45f).copy(alpha = 0.35f)
            FlameState.BROKEN -> Color(0xFF777777).copy(alpha = 0.1f)
        }

        if (state != FlameState.IDLE) {
            val haloAlpha = (glowColor.alpha * motion.haloAlpha).coerceIn(0f, 1f)
            val halo = glowColor.copy(alpha = haloAlpha)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(halo, halo.copy(alpha = haloAlpha * .38f), Color.Transparent),
                    center = Offset(width / 2f, height * .57f),
                    radius = width * .6f * motion.glowScale
                )
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(halo.copy(alpha = haloAlpha * .34f), Color.Transparent),
                    center = Offset(width / 2f, height * .62f),
                    radius = width * .31f * motion.glowScale
                )
            )
        }

        if (state == FlameState.IGNITING && motion.ringAlpha > .01f) {
            val ringRadius = width * (.31f + .46f * motion.ringProgress)
            drawCircle(
                color = glowColor.copy(alpha = (.48f * motion.ringAlpha).coerceIn(0f, .48f)),
                radius = ringRadius,
                center = Offset(width / 2f, height * .57f),
                style = Stroke(width = width * .009f)
            )
            drawCircle(
                color = Color.White.copy(alpha = (.12f * motion.ringAlpha).coerceIn(0f, .12f)),
                radius = ringRadius * .88f,
                center = Offset(width / 2f, height * .57f),
                style = Stroke(width = width * .003f)
            )
        }

        if (state == FlameState.IGNITING || state == FlameState.ACTIVE || state == FlameState.COMPLETED) {
            val emberCount = when (state) {
                FlameState.IGNITING -> 8
                FlameState.COMPLETED -> 4
                else -> 2
            }
            repeat(emberCount) { index ->
                val phase = (motion.emberProgress + index * .271f) % 1f
                val drift = sin((phase * 2f * PI.toFloat()) + index * 1.37f)
                val x = width * (.5f + drift * .16f)
                val y = height * (.73f - phase * .43f)
                val shrink = (1f - phase).coerceIn(0f, 1f)
                drawCircle(
                    color = if (state == FlameState.ACTIVE) flameColor.lighten(.5f) else Color.White,
                    radius = width * .018f * shrink,
                    center = Offset(x, y),
                    alpha = shrink * .72f
                )
            }

            if (state == FlameState.IGNITING) {
                repeat(9) { index ->
                    val phase = (motion.sparkProgress + index * .113f) % 1f
                    val travel = phase * (2f - phase)
                    val angle = -PI.toFloat() / 2f + (index - 4) * .22f
                    val startDistance = width * (.07f + (index % 3) * .018f)
                    val endDistance = width * (.25f + (index % 4) * .045f)
                    val start = Offset(
                        x = width * .5f + cos(angle) * startDistance,
                        y = height * .68f + sin(angle) * startDistance
                    )
                    val end = Offset(
                        x = width * .5f + cos(angle) * (startDistance + endDistance * travel),
                        y = height * .68f + sin(angle) * (startDistance + endDistance * travel)
                    )
                    val alpha = ((1f - phase) * (1f - phase) * motion.ringAlpha).coerceIn(0f, .8f)
                    drawLine(
                        color = Color.White.copy(alpha = alpha),
                        start = start,
                        end = end,
                        strokeWidth = width * (.006f + (index % 2) * .002f)
                    )
                    drawCircle(
                        color = flameColor.lighten(.58f).copy(alpha = alpha),
                        radius = width * (.009f + (index % 3) * .002f) * (1f - phase),
                        center = end
                    )
                }
            }
        }

        scale(
            scaleX = motion.scaleX,
            scaleY = motion.scaleY,
            pivot = Offset(width * .5f, height * .95f)
        ) {
            val outerTipX = width * (.5f + motion.tipOffsetX)
            val outerPath = Path().apply {
                moveTo(outerTipX, height * .1f)
                cubicTo(
                    width * (.85f + motion.shoulderOffsetX), height * .35f,
                    width * .95f, height * .7f,
                    width * .5f, height * .95f
                )
                cubicTo(
                    width * .05f, height * .7f,
                    width * (.15f + motion.shoulderOffsetX), height * .35f,
                    outerTipX, height * .1f
                )
                close()
            }

            drawPath(
                path = outerPath,
                brush = Brush.verticalGradient(
                    colors = outerColors,
                    startY = height * .1f,
                    endY = height * .95f
                )
            )
        }

        scale(
            scaleX = motion.scaleX * .7f,
            scaleY = motion.scaleY * .7f,
            pivot = Offset(width * .5f, height * .9f)
        ) {
            val innerTipX = width * (.5f - motion.tipOffsetX * .55f)
            val innerPath = Path().apply {
                moveTo(innerTipX, height * .2f)
                cubicTo(
                    width * (.8f - motion.shoulderOffsetX * .4f), height * .4f,
                    width * .9f, height * .7f,
                    width * .5f, height * .9f
                )
                cubicTo(
                    width * .1f, height * .7f,
                    width * (.2f - motion.shoulderOffsetX * .4f), height * .4f,
                    innerTipX, height * .2f
                )
                close()
            }

            drawPath(
                path = innerPath,
                brush = Brush.verticalGradient(
                    colors = innerColors,
                    startY = height * .2f,
                    endY = height * .9f
                )
            )
        }
    }
}

@Composable
private fun rememberFlameClock(
    state: FlameState,
    ignitionEventKey: Any?
): Long {
    var elapsedMs by remember(state, ignitionEventKey) { mutableLongStateOf(0L) }

    LaunchedEffect(state, ignitionEventKey) {
        val startedAt = withFrameNanos { it }
        while (isActive) {
            withFrameNanos { now ->
                elapsedMs = flameElapsedMillisAt(startedAt, now)
            }
        }
    }
    return elapsedMs
}

private fun Color.lighten(amount: Float): Color = Color(
    red = red + (1f - red) * amount,
    green = green + (1f - green) * amount,
    blue = blue + (1f - blue) * amount,
    alpha = alpha
)

private fun Color.darken(amount: Float): Color = Color(
    red = red * (1f - amount),
    green = green * (1f - amount),
    blue = blue * (1f - amount),
    alpha = alpha
)
