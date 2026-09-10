package com.hkm.streakwidget.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.sin

enum class FlameState { IDLE, IGNITING, ACTIVE, COMPLETED, BROKEN }

@Composable
fun CustomFlame(
    state: FlameState,
    modifier: Modifier = Modifier.size(160.dp),
    animationScale: Float = 1f,
    flameColor: Color = Color(0xFFFF5722),
    ignitionEventKey: Any? = state
) {
    val lit = state == FlameState.IGNITING || state == FlameState.ACTIVE || state == FlameState.COMPLETED
    // Keep values across state changes so undo and completion never snap.
    val heat = remember { Animatable(if (lit && state != FlameState.IGNITING) 1f else 0f) }
    val lift = remember { Animatable(0f) }
    val bloom = remember { Animatable(0f) }
    val quench = remember { Animatable(0f) }
    val core = remember { Animatable(0f) }
    val squash = remember { Animatable(0f) }
    var seconds by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(lit) {
        if (lit) {
            val start = withFrameNanos { it }
            val initial = seconds
            while (isActive) withFrameNanos { seconds = initial + (it - start) / 1_000_000_000f }
        }
    }
    LaunchedEffect(state, ignitionEventKey) {
        val extinguishing = !lit && heat.value > .01f
        coroutineScope {
            launch {
                if (extinguishing) delay(200)
                heat.animateTo(if (lit) 1f else 0f, tween(if (lit) 1250 else 1050, easing = FastOutSlowInEasing))
            }
            launch {
                if (extinguishing) {
                    quench.animateTo(1f, tween(420, easing = FastOutSlowInEasing))
                    delay(140)
                }
                quench.animateTo(0f, spring(dampingRatio = .9f, stiffness = 35f))
            }
            launch {
                if (state == FlameState.IGNITING) {
                    core.animateTo(-.09f, tween(240, easing = FastOutSlowInEasing))
                    core.animateTo(.1f, tween(650, easing = FastOutSlowInEasing))
                }
                core.animateTo(0f, spring(dampingRatio = .8f, stiffness = 22f))
            }
            launch {
                if (state == FlameState.IGNITING) {
                    lift.animateTo(-.055f, tween(280, easing = FastOutSlowInEasing))
                    lift.animateTo(.095f, tween(770, easing = FastOutSlowInEasing))
                    // A visible, centered recoil: the flame compresses before settling.
                    squash.animateTo(.16f, tween(260, easing = FastOutSlowInEasing))
                    squash.animateTo(0f, spring(dampingRatio = .82f, stiffness = 28f))
                }
                lift.animateTo(0f, spring(dampingRatio = .78f, stiffness = 24f))
            }
            launch {
                if (state == FlameState.IGNITING) {
                    bloom.animateTo(1f, tween(1050, easing = FastOutSlowInEasing))
                }
                bloom.animateTo(0f, tween(2300, easing = FastOutSlowInEasing))
            }
        }
    }
    Canvas(modifier) {
        val warmth = heat.value.coerceIn(0f, 1f)
        val energy = bloom.value.coerceIn(0f, 1f)
        val collapse = quench.value.coerceIn(0f, 1f)
        val t = seconds * (animationScale.takeIf { it.isFinite() }?.coerceIn(0f, 3f) ?: 1f)
        val breath = sin(t * 2.1f) * (.008f + energy * .012f) * warmth
        val lean = (sin(t * 1.7f) * .014f + sin(t * 2.9f) * .004f + sin(t * 4.1f) * energy * .015f) * warmth
        val w = size.width
        val h = size.height
        val center = Offset(w * .5f, h * (.58f + collapse * .16f))
        drawCircle(
            brush = Brush.radialGradient(listOf(flameColor.copy(alpha = (.27f + energy * .18f) * warmth), flameColor.copy(alpha = (.08f + energy * .06f) * warmth), Color.Transparent), center, w * (.72f + energy * .13f)),
            radius = w * (.72f + energy * .13f), center = center
        )
        scale(1f - lift.value * .35f - breath * .4f + collapse * .07f + squash.value * .12f, 1f + lift.value + breath - collapse * .18f - squash.value, Offset(w * .5f, h * .92f)) {
            val outer = Path().apply {
                moveTo(w * (.5f + lean), h * .09f)
                cubicTo(w * (.79f + lean), h * .31f, w * .92f, h * .69f, w * .5f, h * .93f)
                cubicTo(w * .08f, h * .69f, w * (.21f + lean), h * .31f, w * (.5f + lean), h * .09f)
                close()
            }
            drawPath(outer, Brush.verticalGradient(listOf(
                lerp(Color(0xFF626262), lerp(flameColor, Color.White, .16f + energy * .15f), warmth),
                lerp(Color(0xFF363636), lerp(flameColor, Color.Black, .16f), warmth)
            ), startY = h * .09f, endY = h * .93f))
            scale(1f - collapse * .2f + squash.value * .08f, 1f + core.value - collapse * .42f - squash.value * .7f, Offset(w * .5f, h * .88f)) {
            val inner = Path().apply {
                moveTo(w * (.5f - lean * .5f), h * (.43f - lift.value * .35f))
                cubicTo(w * .68f, h * .57f, w * .78f, h * .77f, w * .5f, h * .88f)
                cubicTo(w * .22f, h * .77f, w * .32f, h * .57f, w * (.5f - lean * .5f), h * (.43f - lift.value * .35f))
                close()
            }
            drawPath(inner, Brush.verticalGradient(listOf(
                lerp(Color(0xFF909090), lerp(flameColor, Color.White, .82f + energy * .14f), warmth),
                lerp(Color(0xFF595959), lerp(flameColor, Color.White, .36f), warmth)
            ), startY = h * .43f, endY = h * .88f))
            }
        }
    }
}
