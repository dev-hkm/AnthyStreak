package com.hkm.streakwidget.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Stages of Bonsai tree growth based on streak count:
 * - SEED (0 streak): Pot with fertile soil & glowing seed ready to germinate
 * - SPROUT (1-3 days): Tender green curved sprout with 2 cotyledon leaves
 * - SAPLING (4-7 days): Growing stem with multiple vibrant young leaves
 * - BONSAI (8-20 days): Elegant aged wood trunk with lush cloud foliage pads
 * - BLOOMING (21+ days): Majestic mature bonsai adorned with glowing blossoms
 */
enum class BonsaiGrowthStage {
    SEED,
    SPROUT,
    SAPLING,
    BONSAI,
    BLOOMING;

    companion object {
        fun fromStreak(streakCount: Int): BonsaiGrowthStage = when {
            streakCount <= 0 -> SEED
            streakCount in 1..3 -> SPROUT
            streakCount in 4..7 -> SAPLING
            streakCount in 8..20 -> BONSAI
            else -> BLOOMING
        }
    }
}

@Composable
fun CustomBonsai(
    streakCount: Int,
    state: FlameState,
    modifier: Modifier = Modifier.size(220.dp),
    accentColor: Color = Color(0xFF4CAF50),
    animationScale: Float = 1f,
    eventKey: Any? = state
) {
    val stage = remember(streakCount) { BonsaiGrowthStage.fromStreak(streakCount) }
    val isCompleted = state == FlameState.COMPLETED || state == FlameState.ACTIVE
    val isWatering = state == FlameState.IGNITING

    // Energy / vitality transition
    val vitality = remember { Animatable(if (isCompleted) 1f else 0.45f) }
    val bloomScale = remember { Animatable(0f) }
    val dropProgress = remember { Animatable(0f) }
    val leafRustle = remember { Animatable(0f) }

    var seconds by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(true) {
        val start = withFrameNanos { it }
        val initial = seconds
        while (isActive) {
            withFrameNanos { seconds = initial + (it - start) / 1_000_000_000f }
        }
    }

    LaunchedEffect(state, eventKey) {
        coroutineScope {
            launch {
                vitality.animateTo(
                    targetValue = if (isCompleted || isWatering) 1f else 0.45f,
                    animationSpec = tween(900, easing = FastOutSlowInEasing)
                )
            }
            if (isWatering) {
                launch {
                    dropProgress.snapTo(0f)
                    dropProgress.animateTo(1f, tween(650, easing = FastOutSlowInEasing))
                }
                launch {
                    bloomScale.animateTo(1.25f, tween(350, easing = FastOutSlowInEasing))
                    bloomScale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
                }
                launch {
                    leafRustle.animateTo(1f, tween(400, easing = FastOutSlowInEasing))
                    leafRustle.animateTo(0f, spring(dampingRatio = 0.7f, stiffness = 40f))
                }
            } else {
                bloomScale.animateTo(1f, tween(300))
                leafRustle.animateTo(0f, tween(300))
            }
        }
    }

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val t = seconds * (animationScale.coerceIn(0.1f, 3f))
        val currentVitality = vitality.value.coerceIn(0f, 1f)

        // Subtle gentle breathing/swaying in the wind
        val sway = sin(t * 1.6f) * 1.5f + (leafRustle.value * sin(t * 12f) * 5f)
        val breathe = (sin(t * 2.2f) * 0.015f + 1f) * (if (bloomScale.value > 0f) bloomScale.value else 1f)

        // Draw soft ambient glow behind plant
        val glowRadius = w * 0.42f * currentVitality
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    accentColor.copy(alpha = 0.18f * currentVitality),
                    accentColor.copy(alpha = 0.05f * currentVitality),
                    Color.Transparent
                ),
                center = Offset(w * 0.5f, h * 0.55f),
                radius = glowRadius
            ),
            radius = glowRadius,
            center = Offset(w * 0.5f, h * 0.55f)
        )

        // Draw Ceramic Bonsai Pot (Always grounded at bottom)
        drawPot(w, h, currentVitality)

        // Draw Plant Stage inside scaling scope
        scale(scaleX = breathe, scaleY = breathe, pivot = Offset(w * 0.5f, h * 0.78f)) {
            when (stage) {
                BonsaiGrowthStage.SEED -> drawSeedStage(w, h, currentVitality, accentColor, t)
                BonsaiGrowthStage.SPROUT -> drawSproutStage(w, h, currentVitality, accentColor, sway)
                BonsaiGrowthStage.SAPLING -> drawSaplingStage(w, h, currentVitality, accentColor, sway)
                BonsaiGrowthStage.BONSAI -> drawBonsaiStage(w, h, currentVitality, accentColor, sway, isBlooming = false)
                BonsaiGrowthStage.BLOOMING -> drawBonsaiStage(w, h, currentVitality, accentColor, sway, isBlooming = true, pulse = t)
            }
        }

        // Draw Nurture Ritual (Dew drops falling & ripple splash)
        if (isWatering && dropProgress.value > 0f && dropProgress.value < 1f) {
            drawWateringRitual(w, h, dropProgress.value, accentColor)
        }
    }
}

private fun DrawScope.drawPot(w: Float, h: Float, vitality: Float) {
    val potTop = h * 0.76f
    val potBottom = h * 0.88f
    val potLeft = w * 0.28f
    val potRight = w * 0.72f
    val potWidth = potRight - potLeft

    // Ceramic pot body (rich matte porcelain / dark terracotta)
    val potPath = Path().apply {
        moveTo(potLeft - 4f, potTop)
        lineTo(potRight + 4f, potTop)
        lineTo(potRight - 12f, potBottom)
        lineTo(potLeft + 12f, potBottom)
        close()
    }

    val potGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF2C2F33),
            Color(0xFF1E2124)
        ),
        startY = potTop,
        endY = potBottom
    )
    drawPath(potPath, potGradient)

    // Pot rim lip
    val rimPath = Path().apply {
        addRoundRect(
            RoundRect(
                left = potLeft - 7f,
                top = potTop - 4f,
                right = potRight + 7f,
                bottom = potTop + 5f,
                cornerRadius = CornerRadius(4f, 4f)
            )
        )
    }
    drawPath(rimPath, Brush.horizontalGradient(listOf(Color(0xFF3F4448), Color(0xFF23272A))))

    // Pot feet (Zen Bonsai stands)
    val footWidth = 14f
    val footHeight = 6f
    drawRoundRect(
        color = Color(0xFF1A1C1E),
        topLeft = Offset(potLeft + 14f, potBottom),
        size = Size(footWidth, footHeight),
        cornerRadius = CornerRadius(2f, 2f)
    )
    drawRoundRect(
        color = Color(0xFF1A1C1E),
        topLeft = Offset(potRight - 14f - footWidth, potBottom),
        size = Size(footWidth, footHeight),
        cornerRadius = CornerRadius(2f, 2f)
    )

    // Soil mound
    val soilPath = Path().apply {
        moveTo(potLeft - 2f, potTop)
        cubicTo(potLeft + potWidth * 0.25f, potTop - 8f, potRight - potWidth * 0.25f, potTop - 8f, potRight + 2f, potTop)
        close()
    }
    drawPath(soilPath, Brush.verticalGradient(listOf(Color(0xFF422C1D), Color(0xFF2B1C12)), startY = potTop - 8f, endY = potTop))

    // Moss accent on soil
    val mossColor = lerp(Color(0xFF435A38), Color(0xFF6B8E23), vitality)
    drawCircle(mossColor, radius = 10f, center = Offset(w * 0.42f, potTop - 2f))
    drawCircle(mossColor, radius = 8f, center = Offset(w * 0.58f, potTop - 1f))
}

private fun DrawScope.drawSeedStage(w: Float, h: Float, vitality: Float, accentColor: Color, t: Float) {
    val seedCenter = Offset(w * 0.5f, h * 0.74f)
    val seedGlow = (sin(t * 3f) * 0.2f + 0.8f) * vitality

    // Glowing aura around the dormant seed
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(accentColor.copy(alpha = 0.35f * seedGlow), Color.Transparent),
            center = seedCenter,
            radius = 24f
        ),
        radius = 24f,
        center = seedCenter
    )

    // Seed pod shape (Golden/Auburn acorn-seed)
    val seedPath = Path().apply {
        moveTo(seedCenter.x, seedCenter.y - 12f)
        cubicTo(seedCenter.x + 10f, seedCenter.y - 5f, seedCenter.x + 9f, seedCenter.y + 8f, seedCenter.x, seedCenter.y + 11f)
        cubicTo(seedCenter.x - 9f, seedCenter.y + 8f, seedCenter.x - 10f, seedCenter.y - 5f, seedCenter.x, seedCenter.y - 12f)
        close()
    }
    drawPath(seedPath, Brush.verticalGradient(listOf(Color(0xFFD4AF37), Color(0xFF8B5A2B)), startY = seedCenter.y - 12f, endY = seedCenter.y + 11f))

    // Little energetic crack / green tip
    val tipPath = Path().apply {
        moveTo(seedCenter.x, seedCenter.y - 12f)
        lineTo(seedCenter.x + 2f, seedCenter.y - 17f)
    }
    drawPath(tipPath, accentColor, style = Stroke(width = 3f, cap = StrokeCap.Round))
}

private fun DrawScope.drawSproutStage(w: Float, h: Float, vitality: Float, accentColor: Color, sway: Float) {
    val baseY = h * 0.75f
    val sproutColor = lerp(Color(0xFF7CB342), accentColor, vitality)

    // Tender green curved stem
    val stemPath = Path().apply {
        moveTo(w * 0.5f, baseY)
        cubicTo(w * 0.49f, h * 0.65f, w * (0.52f + sway * 0.002f), h * 0.55f, w * (0.5f + sway * 0.004f), h * 0.47f)
    }
    drawPath(stemPath, sproutColor, style = Stroke(width = 6f, cap = StrokeCap.Round))

    val tipX = w * (0.5f + sway * 0.004f)
    val tipY = h * 0.47f

    // Two young cotyledon leaves opening
    // Left leaf
    rotate(-32f + sway, pivot = Offset(tipX, tipY)) {
        val leftLeaf = Path().apply {
            moveTo(tipX, tipY)
            cubicTo(tipX - 22f, tipY - 14f, tipX - 24f, tipY + 4f, tipX, tipY)
            close()
        }
        drawPath(leftLeaf, Brush.radialGradient(listOf(sproutColor, Color(0xFF558B2F)), center = Offset(tipX - 10f, tipY), radius = 22f))
    }

    // Right leaf
    rotate(38f + sway, pivot = Offset(tipX, tipY)) {
        val rightLeaf = Path().apply {
            moveTo(tipX, tipY)
            cubicTo(tipX + 24f, tipY - 15f, tipX + 26f, tipY + 4f, tipX, tipY)
            close()
        }
        drawPath(rightLeaf, Brush.radialGradient(listOf(sproutColor, Color(0xFF33691E)), center = Offset(tipX + 10f, tipY), radius = 24f))
    }
}

private fun DrawScope.drawSaplingStage(w: Float, h: Float, vitality: Float, accentColor: Color, sway: Float) {
    val baseY = h * 0.75f
    val woodColor = Color(0xFF6D4C41)
    val leafColor = lerp(Color(0xFF66BB6A), accentColor, vitality)

    // Sturdier stem
    val stemPath = Path().apply {
        moveTo(w * 0.5f, baseY)
        cubicTo(w * 0.48f, h * 0.62f, w * 0.53f, h * 0.50f, w * (0.49f + sway * 0.003f), h * 0.38f)
    }
    drawPath(stemPath, woodColor, style = Stroke(width = 8f, cap = StrokeCap.Round))

    // Branches & leaves
    val branch1Start = Offset(w * 0.51f, h * 0.56f)
    val branch2Start = Offset(w * 0.49f, h * 0.47f)
    val topPoint = Offset(w * (0.49f + sway * 0.003f), h * 0.38f)

    // Left branch
    drawLine(woodColor, branch1Start, Offset(branch1Start.x - 28f, branch1Start.y - 14f), strokeWidth = 5f, cap = StrokeCap.Round)
    drawLeaf(Offset(branch1Start.x - 28f, branch1Start.y - 14f), angle = -40f + sway, size = 26f, color = leafColor)

    // Right branch
    drawLine(woodColor, branch2Start, Offset(branch2Start.x + 30f, branch2Start.y - 12f), strokeWidth = 5f, cap = StrokeCap.Round)
    drawLeaf(Offset(branch2Start.x + 30f, branch2Start.y - 12f), angle = 45f + sway, size = 28f, color = leafColor)

    // Top crown cluster
    drawLeaf(topPoint, angle = -15f + sway, size = 30f, color = leafColor)
    drawLeaf(topPoint, angle = 20f + sway, size = 32f, color = leafColor)
}

private fun DrawScope.drawBonsaiStage(
    w: Float,
    h: Float,
    vitality: Float,
    accentColor: Color,
    sway: Float,
    isBlooming: Boolean,
    pulse: Float = 0f
) {
    val baseY = h * 0.75f
    val trunkBaseX = w * 0.51f
    val woodColor = Color(0xFF4E342E)
    val woodHighlight = Color(0xFF795548)
    val foliageGreen = lerp(Color(0xFF2E7D32), accentColor, vitality * 0.65f)
    val foliageDeep = Color(0xFF1B5E20)

    // Classic graceful S-curved Bonsai trunk (Moyogi style)
    val trunkPath = Path().apply {
        moveTo(trunkBaseX - 10f, baseY)
        cubicTo(w * 0.39f, h * 0.65f, w * 0.62f, h * 0.52f, w * (0.43f + sway * 0.003f), h * 0.36f)
        lineTo(w * (0.45f + sway * 0.003f), h * 0.35f)
        cubicTo(w * 0.66f, h * 0.51f, w * 0.44f, h * 0.65f, trunkBaseX + 10f, baseY)
        close()
    }
    drawPath(trunkPath, Brush.horizontalGradient(listOf(woodColor, woodHighlight, woodColor)))

    // Main left lower branch
    val b1StartX = w * 0.44f
    val b1StartY = h * 0.60f
    val b1EndX = w * 0.24f
    val b1EndY = h * 0.53f
    val b1Path = Path().apply {
        moveTo(b1StartX, b1StartY)
        cubicTo(w * 0.32f, h * 0.62f, w * 0.28f, h * 0.56f, b1EndX, b1EndY)
    }
    drawPath(b1Path, woodColor, style = Stroke(width = 8f, cap = StrokeCap.Round))

    // Right mid branch
    val b2StartX = w * 0.54f
    val b2StartY = h * 0.49f
    val b2EndX = w * 0.74f
    val b2EndY = h * 0.43f
    val b2Path = Path().apply {
        moveTo(b2StartX, b2StartY)
        cubicTo(w * 0.63f, h * 0.47f, w * 0.68f, h * 0.45f, b2EndX, b2EndY)
    }
    drawPath(b2Path, woodColor, style = Stroke(width = 7f, cap = StrokeCap.Round))

    // Top crown branch
    val crownX = w * (0.44f + sway * 0.003f)
    val crownY = h * 0.34f

    // Bonsai Foliage Clouds (Tamamono cloud pads)
    drawCloudPad(center = Offset(b1EndX, b1EndY), width = 64f, height = 34f, deepColor = foliageDeep, topColor = foliageGreen)
    drawCloudPad(center = Offset(b2EndX, b2EndY), width = 74f, height = 38f, deepColor = foliageDeep, topColor = foliageGreen)
    drawCloudPad(center = Offset(crownX, crownY), width = 90f, height = 46f, deepColor = foliageDeep, topColor = foliageGreen)

    // Additional layered depth clouds
    drawCloudPad(center = Offset(w * 0.35f, h * 0.42f), width = 50f, height = 28f, deepColor = foliageDeep, topColor = foliageGreen)

    // Blooming Sakura/Bonsai blossoms if streak is mature (21+ days)
    if (isBlooming) {
        val blossomColor = accentColor
        val glow = (sin(pulse * 2.5f) * 0.2f + 0.8f)

        drawBlossom(Offset(b1EndX - 10f, b1EndY - 6f), blossomColor, glow)
        drawBlossom(Offset(b1EndX + 14f, b1EndY + 4f), blossomColor, glow)

        drawBlossom(Offset(b2EndX - 16f, b2EndY - 8f), blossomColor, glow)
        drawBlossom(Offset(b2EndX + 18f, b2EndY - 2f), blossomColor, glow)
        drawBlossom(Offset(b2EndX + 2f, b2EndY + 8f), blossomColor, glow)

        drawBlossom(Offset(crownX - 24f, crownY - 8f), blossomColor, glow)
        drawBlossom(Offset(crownX, crownY - 14f), blossomColor, glow)
        drawBlossom(Offset(crownX + 26f, crownY - 6f), blossomColor, glow)
        drawBlossom(Offset(crownX - 8f, crownY + 8f), blossomColor, glow)
    }
}

private fun DrawScope.drawCloudPad(center: Offset, width: Float, height: Float, deepColor: Color, topColor: Color) {
    val r = width * 0.32f
    val gradient = Brush.radialGradient(
        colors = listOf(topColor, deepColor),
        center = Offset(center.x, center.y - height * 0.2f),
        radius = width * 0.6f
    )

    drawCircle(gradient, radius = r * 0.85f, center = Offset(center.x - width * 0.28f, center.y + 2f))
    drawCircle(gradient, radius = r * 0.9f, center = Offset(center.x + width * 0.28f, center.y + 1f))
    drawCircle(gradient, radius = r * 1.05f, center = Offset(center.x, center.y - 4f))
}

private fun DrawScope.drawLeaf(tip: Offset, angle: Float, size: Float, color: Color) {
    rotate(angle, pivot = tip) {
        val leaf = Path().apply {
            moveTo(tip.x, tip.y)
            cubicTo(tip.x + size * 0.7f, tip.y - size * 0.4f, tip.x + size * 0.8f, tip.y + size * 0.3f, tip.x + size, tip.y)
            cubicTo(tip.x + size * 0.8f, tip.y + size * 0.5f, tip.x + size * 0.3f, tip.y + size * 0.4f, tip.x, tip.y)
            close()
        }
        drawPath(leaf, color)
    }
}

private fun DrawScope.drawBlossom(center: Offset, petalColor: Color, glow: Float) {
    val petalRadius = 6.5f
    // Gentle bloom glow
    drawCircle(petalColor.copy(alpha = 0.3f * glow), radius = 12f, center = center)

    // 5 delicate flower petals
    for (i in 0 until 5) {
        val angle = (i * 72f) * (PI.toFloat() / 180f)
        val offset = Offset(cos(angle) * 5.5f, sin(angle) * 5.5f)
        drawCircle(
            color = lerp(petalColor, Color.White, 0.35f),
            radius = petalRadius,
            center = center + offset
        )
    }
    // Golden center pistil
    drawCircle(Color(0xFFFFD54F), radius = 3.5f, center = center)
}

private fun DrawScope.drawWateringRitual(w: Float, h: Float, progress: Float, color: Color) {
    val dropY = h * 0.15f + progress * (h * 0.55f)
    val dropX = w * 0.5f

    if (progress < 0.75f) {
        // Falling crystalline dew drops
        val dropPath = Path().apply {
            moveTo(dropX, dropY - 14f)
            cubicTo(dropX + 5f, dropY - 6f, dropX + 5f, dropY + 4f, dropX, dropY + 7f)
            cubicTo(dropX - 5f, dropY + 4f, dropX - 5f, dropY - 6f, dropX, dropY - 14f)
            close()
        }
        drawPath(dropPath, Color(0xFF64B5F6))
        drawCircle(Color.White.copy(alpha = 0.8f), radius = 2.5f, center = Offset(dropX - 1.5f, dropY - 3f))
    } else {
        // Splash and ripple burst over soil / plant
        val splashProgress = (progress - 0.75f) / 0.25f
        val rippleRadius = splashProgress * 45f
        val rippleAlpha = (1f - splashProgress).coerceIn(0f, 1f)

        drawCircle(
            color = color.copy(alpha = 0.5f * rippleAlpha),
            radius = rippleRadius,
            center = Offset(dropX, h * 0.70f),
            style = Stroke(width = 3f * (1f - splashProgress))
        )
    }
}
