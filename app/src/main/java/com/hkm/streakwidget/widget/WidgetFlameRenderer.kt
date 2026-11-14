package com.hkm.streakwidget.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import androidx.annotation.ColorInt
import com.hkm.streakwidget.model.VisualTheme
import com.hkm.streakwidget.ui.components.BonsaiGrowthStage
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

internal data class WidgetFlameLayers(
    val glow: Bitmap,
    val outer: Bitmap,
    val mid: Bitmap,
    val inner: Bitmap
)

/**
 * Renders the widget hero graphic (layered CustomFlame or growth-staged CustomBonsai) into
 * cached bitmaps matching the in-app Canvas aesthetics. All four layers share the same 0..1
 * coordinate space so any widget crop scales them identically without proportion drift.
 */
internal object WidgetFlameRenderer {
    private const val GLOW_SIZE = 64
    private const val OUTER_SIZE = 120
    private const val MID_SIZE = 104
    private const val INNER_SIZE = 88
    private const val COMPOSITE_SIZE = 80
    private val cache = LinkedHashMap<String, WidgetFlameLayers>(20, 0.75f, true)
    private val compositeCache = LinkedHashMap<String, Bitmap>(20, 0.75f, true)

    @Synchronized
    fun composite(
        @ColorInt accent: Int,
        lit: Boolean,
        theme: VisualTheme = VisualTheme.FLAME,
        streak: Int = 0
    ): Bitmap {
        val stage = if (theme == VisualTheme.TREE) BonsaiGrowthStage.fromStreak(streak) else null
        val key = "comp:${theme.name}:${stage?.name.orEmpty()}:$accent:$lit"
        compositeCache[key]?.let { return it }
        val layerSet = layers(accent, lit, theme, streak)
        val merged = bitmap(COMPOSITE_SIZE) { canvas, size ->
            val dst = RectF(0f, 0f, size.toFloat(), size.toFloat())
            val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
            canvas.drawBitmap(layerSet.glow, null, dst, paint)
            canvas.drawBitmap(layerSet.outer, null, dst, paint)
            canvas.drawBitmap(layerSet.mid, null, dst, paint)
            canvas.drawBitmap(layerSet.inner, null, dst, paint)
        }
        if (compositeCache.size >= 16) compositeCache.remove(compositeCache.keys.first())
        compositeCache[key] = merged
        return merged
    }

    @Synchronized
    fun layers(
        @ColorInt accent: Int,
        lit: Boolean,
        theme: VisualTheme = VisualTheme.FLAME,
        streak: Int = 0
    ): WidgetFlameLayers {
        val stage = if (theme == VisualTheme.TREE) BonsaiGrowthStage.fromStreak(streak) else null
        val key = "${theme.name}:${stage?.name.orEmpty()}:$accent:$lit"
        cache[key]?.let { return it }
        val rendered = when (theme) {
            VisualTheme.TREE -> renderBonsai(accent, lit, stage ?: BonsaiGrowthStage.SEED)
            VisualTheme.FLAME,
            VisualTheme.FLOWER,
            VisualTheme.MOUNTAIN -> renderFlame(accent, lit)
        }
        if (cache.size >= 16) cache.remove(cache.keys.first())
        cache[key] = rendered
        return rendered
    }

    private fun renderFlame(@ColorInt accent: Int, lit: Boolean): WidgetFlameLayers {
        // Match CustomFlame.kt: idle uses charcoal grayscale until today's check-in lights it up.
        val outerTop = if (lit) mix(accent, Color.WHITE, 0.16f) else Color.rgb(0x62, 0x62, 0x62)
        val outerBottom = if (lit) mix(accent, Color.BLACK, 0.16f) else Color.rgb(0x36, 0x36, 0x36)
        val innerTop = if (lit) mix(accent, Color.WHITE, 0.82f) else Color.rgb(0x90, 0x90, 0x90)
        val innerBottom = if (lit) mix(accent, Color.WHITE, 0.36f) else Color.rgb(0x59, 0x59, 0x59)

        return WidgetFlameLayers(
            glow = bitmap(GLOW_SIZE) { canvas, size ->
                if (lit) {
                    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        shader = RadialGradient(
                            size * 0.5f,
                            size * 0.54f,
                            size * 0.46f,
                            intArrayOf(
                                withAlpha(accent, 0.32f),
                                withAlpha(accent, 0.09f),
                                Color.TRANSPARENT
                            ),
                            floatArrayOf(0f, 0.54f, 1f),
                            Shader.TileMode.CLAMP
                        )
                    }
                    canvas.drawCircle(size * 0.5f, size * 0.54f, size * 0.46f, paint)
                }
            },
            outer = bitmap(OUTER_SIZE) { canvas, size ->
                val paint = gradientPaint(size, 0.09f, 0.93f, outerTop, outerBottom)
                canvas.drawPath(outerFlamePath(size), paint)
            },
            mid = bitmap(MID_SIZE) { canvas, size ->
                // Soft internal heat bloom rather than an artificial hard-edged third silhouette.
                val bloomColor = if (lit) mix(accent, Color.WHITE, 0.48f) else Color.rgb(0x78, 0x78, 0x78)
                val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    shader = RadialGradient(
                        size * 0.5f,
                        size * 0.64f,
                        size * 0.34f,
                        intArrayOf(
                            withAlpha(bloomColor, if (lit) 0.46f else 0.18f),
                            withAlpha(bloomColor, if (lit) 0.14f else 0.05f),
                            Color.TRANSPARENT
                        ),
                        floatArrayOf(0f, 0.58f, 1f),
                        Shader.TileMode.CLAMP
                    )
                }
                canvas.drawPath(outerFlamePath(size), paint)
            },
            inner = bitmap(INNER_SIZE) { canvas, size ->
                val paint = gradientPaint(size, 0.43f, 0.88f, innerTop, innerBottom)
                canvas.drawPath(innerFlamePath(size), paint)
                if (lit) {
                    val coreHot = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        shader = RadialGradient(
                            size * 0.5f,
                            size * 0.72f,
                            size * 0.16f,
                            intArrayOf(withAlpha(Color.WHITE, 0.55f), Color.TRANSPARENT),
                            floatArrayOf(0f, 1f),
                            Shader.TileMode.CLAMP
                        )
                    }
                    canvas.drawPath(innerFlamePath(size), coreHot)
                }
            }
        )
    }

    private fun renderBonsai(
        @ColorInt accent: Int,
        lit: Boolean,
        stage: BonsaiGrowthStage
    ): WidgetFlameLayers {
        val vitality = if (lit) 1f else 0.45f
        val foliageTop = mix(Color.rgb(0x2E, 0x7D, 0x32), accent, vitality * 0.65f)
        val foliageDeep = Color.rgb(0x1B, 0x5E, 0x20)
        val sproutColor = mix(Color.rgb(0x7C, 0xB3, 0x42), accent, vitality)
        val saplingLeaf = mix(Color.rgb(0x66, 0xBB, 0x6A), accent, vitality)
        val woodDark = Color.rgb(0x4E, 0x34, 0x2E)
        val woodLight = Color.rgb(0x79, 0x55, 0x48)

        return WidgetFlameLayers(
            glow = bitmap(GLOW_SIZE) { canvas, size ->
                val alphaScale = if (lit) 1f else 0.4f
                val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    shader = RadialGradient(
                        size * 0.5f,
                        size * 0.54f,
                        size * 0.45f,
                        intArrayOf(
                            withAlpha(accent, 0.22f * alphaScale),
                            withAlpha(accent, 0.06f * alphaScale),
                            Color.TRANSPARENT
                        ),
                        floatArrayOf(0f, 0.55f, 1f),
                        Shader.TileMode.CLAMP
                    )
                }
                canvas.drawCircle(size * 0.5f, size * 0.54f, size * 0.45f, paint)
            },
            outer = bitmap(OUTER_SIZE) { canvas, s ->
                // Ceramic pot + soil + moss + wood trunk/stem (matches CustomBonsai.kt)
                drawBonsaiPot(canvas, s, vitality)
                when (stage) {
                    BonsaiGrowthStage.SEED -> Unit
                    BonsaiGrowthStage.SPROUT -> {
                        val stem = Path().apply {
                            moveTo(s * 0.5f, s * 0.75f)
                            cubicTo(s * 0.49f, s * 0.65f, s * 0.52f, s * 0.55f, s * 0.5f, s * 0.47f)
                        }
                        canvas.drawPath(stem, strokePaint(sproutColor, s * 0.028f))
                    }
                    BonsaiGrowthStage.SAPLING -> {
                        val stem = Path().apply {
                            moveTo(s * 0.5f, s * 0.75f)
                            cubicTo(s * 0.48f, s * 0.62f, s * 0.53f, s * 0.50f, s * 0.49f, s * 0.38f)
                        }
                        canvas.drawPath(stem, strokePaint(Color.rgb(0x6D, 0x4C, 0x41), s * 0.036f))
                        val branchPaint = strokePaint(Color.rgb(0x6D, 0x4C, 0x41), s * 0.022f)
                        canvas.drawLine(s * 0.51f, s * 0.56f, s * 0.38f, s * 0.49f, branchPaint)
                        canvas.drawLine(s * 0.49f, s * 0.47f, s * 0.63f, s * 0.41f, branchPaint)
                    }
                    BonsaiGrowthStage.BONSAI,
                    BonsaiGrowthStage.BLOOMING -> {
                        val trunk = Path().apply {
                            moveTo(s * 0.46f, s * 0.75f)
                            cubicTo(s * 0.39f, s * 0.65f, s * 0.62f, s * 0.52f, s * 0.43f, s * 0.36f)
                            lineTo(s * 0.45f, s * 0.35f)
                            cubicTo(s * 0.66f, s * 0.51f, s * 0.44f, s * 0.65f, s * 0.56f, s * 0.75f)
                            close()
                        }
                        val trunkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            shader = LinearGradient(
                                s * 0.4f,
                                0f,
                                s * 0.6f,
                                0f,
                                intArrayOf(woodDark, woodLight, woodDark),
                                null,
                                Shader.TileMode.CLAMP
                            )
                        }
                        canvas.drawPath(trunk, trunkPaint)
                        val b1 = Path().apply {
                            moveTo(s * 0.44f, s * 0.60f)
                            cubicTo(s * 0.32f, s * 0.62f, s * 0.28f, s * 0.56f, s * 0.24f, s * 0.53f)
                        }
                        canvas.drawPath(b1, strokePaint(woodDark, s * 0.034f))
                        val b2 = Path().apply {
                            moveTo(s * 0.54f, s * 0.49f)
                            cubicTo(s * 0.63f, s * 0.47f, s * 0.68f, s * 0.45f, s * 0.74f, s * 0.43f)
                        }
                        canvas.drawPath(b2, strokePaint(woodDark, s * 0.030f))
                    }
                }
            },
            mid = bitmap(MID_SIZE) { canvas, s ->
                // Foliage / leaves / seed body
                when (stage) {
                    BonsaiGrowthStage.SEED -> {
                        val cx = s * 0.5f
                        val cy = s * 0.72f
                        val seed = Path().apply {
                            moveTo(cx, cy - s * 0.07f)
                            cubicTo(cx + s * 0.06f, cy - s * 0.03f, cx + s * 0.05f, cy + s * 0.05f, cx, cy + s * 0.06f)
                            cubicTo(cx - s * 0.05f, cy + s * 0.05f, cx - s * 0.06f, cy - s * 0.03f, cx, cy - s * 0.07f)
                            close()
                        }
                        canvas.drawPath(
                            seed,
                            gradientPaint(s, 0.65f, 0.78f, Color.rgb(0xD4, 0xAF, 0x37), Color.rgb(0x8B, 0x5A, 0x2B))
                        )
                    }
                    BonsaiGrowthStage.SPROUT -> {
                        drawWidgetLeaf(canvas, s * 0.5f, s * 0.47f, -148f, s * 0.14f, sproutColor)
                        drawWidgetLeaf(canvas, s * 0.5f, s * 0.47f, -32f, s * 0.15f, mix(sproutColor, Color.BLACK, 0.18f))
                    }
                    BonsaiGrowthStage.SAPLING -> {
                        drawWidgetLeaf(canvas, s * 0.38f, s * 0.49f, -145f, s * 0.13f, saplingLeaf)
                        drawWidgetLeaf(canvas, s * 0.63f, s * 0.41f, -35f, s * 0.14f, saplingLeaf)
                        drawWidgetLeaf(canvas, s * 0.49f, s * 0.38f, -105f, s * 0.15f, saplingLeaf)
                        drawWidgetLeaf(canvas, s * 0.49f, s * 0.38f, -65f, s * 0.15f, mix(saplingLeaf, Color.WHITE, 0.15f))
                    }
                    BonsaiGrowthStage.BONSAI,
                    BonsaiGrowthStage.BLOOMING -> {
                        drawCloudPad(canvas, s * 0.24f, s * 0.53f, s * 0.29f, s * 0.15f, foliageDeep, foliageTop)
                        drawCloudPad(canvas, s * 0.74f, s * 0.43f, s * 0.33f, s * 0.17f, foliageDeep, foliageTop)
                        drawCloudPad(canvas, s * 0.35f, s * 0.42f, s * 0.24f, s * 0.13f, foliageDeep, foliageTop)
                        drawCloudPad(canvas, s * 0.44f, s * 0.34f, s * 0.40f, s * 0.20f, foliageDeep, foliageTop)
                    }
                }
            },
            inner = bitmap(INNER_SIZE) { canvas, s ->
                when (stage) {
                    BonsaiGrowthStage.SEED -> {
                        canvas.drawLine(
                            s * 0.5f,
                            s * 0.65f,
                            s * 0.515f,
                            s * 0.61f,
                            strokePaint(accent, s * 0.022f)
                        )
                    }
                    BonsaiGrowthStage.SPROUT,
                    BonsaiGrowthStage.SAPLING,
                    BonsaiGrowthStage.BONSAI -> {
                        if (lit) {
                            val highlight = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                                shader = RadialGradient(
                                    s * 0.48f,
                                    s * 0.35f,
                                    s * 0.18f,
                                    intArrayOf(withAlpha(accent, 0.24f), Color.TRANSPARENT),
                                    floatArrayOf(0f, 1f),
                                    Shader.TileMode.CLAMP
                                )
                            }
                            canvas.drawCircle(s * 0.48f, s * 0.35f, s * 0.18f, highlight)
                        }
                    }
                    BonsaiGrowthStage.BLOOMING -> {
                        val blossomColor = if (lit) accent else mix(accent, Color.GRAY, 0.45f)
                        drawBlossom(canvas, s * 0.20f, s * 0.50f, s, blossomColor)
                        drawBlossom(canvas, s * 0.30f, s * 0.55f, s, blossomColor)
                        drawBlossom(canvas, s * 0.68f, s * 0.39f, s, blossomColor)
                        drawBlossom(canvas, s * 0.80f, s * 0.42f, s, blossomColor)
                        drawBlossom(canvas, s * 0.36f, s * 0.30f, s, blossomColor)
                        drawBlossom(canvas, s * 0.44f, s * 0.27f, s, blossomColor)
                        drawBlossom(canvas, s * 0.54f, s * 0.32f, s, blossomColor)
                    }
                }
            }
        )
    }

    private fun drawBonsaiPot(canvas: Canvas, s: Float, vitality: Float) {
        val potTop = s * 0.76f
        val potBottom = s * 0.88f
        val potLeft = s * 0.28f
        val potRight = s * 0.72f
        val potPath = Path().apply {
            moveTo(potLeft - s * 0.02f, potTop)
            lineTo(potRight + s * 0.02f, potTop)
            lineTo(potRight - s * 0.055f, potBottom)
            lineTo(potLeft + s * 0.055f, potBottom)
            close()
        }
        canvas.drawPath(
            potPath,
            gradientPaint(s, 0.76f, 0.88f, Color.rgb(0x2C, 0x2F, 0x33), Color.rgb(0x1E, 0x21, 0x24))
        )
        val rimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                potLeft,
                0f,
                potRight,
                0f,
                Color.rgb(0x3F, 0x44, 0x48),
                Color.rgb(0x23, 0x27, 0x2A),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRoundRect(
            RectF(potLeft - s * 0.03f, potTop - s * 0.02f, potRight + s * 0.03f, potTop + s * 0.024f),
            s * 0.02f,
            s * 0.02f,
            rimPaint
        )
        val footPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(0x1A, 0x1C, 0x1E) }
        canvas.drawRoundRect(
            RectF(potLeft + s * 0.065f, potBottom, potLeft + s * 0.13f, potBottom + s * 0.028f),
            s * 0.01f,
            s * 0.01f,
            footPaint
        )
        canvas.drawRoundRect(
            RectF(potRight - s * 0.13f, potBottom, potRight - s * 0.065f, potBottom + s * 0.028f),
            s * 0.01f,
            s * 0.01f,
            footPaint
        )
        val soilPath = Path().apply {
            moveTo(potLeft - s * 0.01f, potTop)
            cubicTo(
                potLeft + (potRight - potLeft) * 0.25f,
                potTop - s * 0.038f,
                potRight - (potRight - potLeft) * 0.25f,
                potTop - s * 0.038f,
                potRight + s * 0.01f,
                potTop
            )
            close()
        }
        canvas.drawPath(
            soilPath,
            gradientPaint(s, 0.72f, 0.76f, Color.rgb(0x42, 0x2C, 0x1D), Color.rgb(0x2B, 0x1C, 0x12))
        )
        val mossPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = mix(Color.rgb(0x43, 0x5A, 0x38), Color.rgb(0x6B, 0x8E, 0x23), vitality)
        }
        canvas.drawCircle(s * 0.42f, potTop - s * 0.01f, s * 0.045f, mossPaint)
        canvas.drawCircle(s * 0.58f, potTop - s * 0.006f, s * 0.036f, mossPaint)
    }

    private fun drawCloudPad(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        width: Float,
        height: Float,
        @ColorInt deepColor: Int,
        @ColorInt topColor: Int
    ) {
        val r = width * 0.32f
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                cx,
                cy - height * 0.2f,
                width * 0.6f,
                topColor,
                deepColor,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawCircle(cx - width * 0.28f, cy + height * 0.06f, r * 0.85f, paint)
        canvas.drawCircle(cx + width * 0.28f, cy + height * 0.03f, r * 0.90f, paint)
        canvas.drawCircle(cx, cy - height * 0.12f, r * 1.05f, paint)
    }

    private fun drawWidgetLeaf(
        canvas: Canvas,
        x: Float,
        y: Float,
        angleDeg: Float,
        len: Float,
        @ColorInt color: Int
    ) {
        canvas.save()
        canvas.rotate(angleDeg, x, y)
        val leaf = Path().apply {
            moveTo(x, y)
            cubicTo(x + len * 0.7f, y - len * 0.4f, x + len * 0.8f, y + len * 0.3f, x + len, y)
            cubicTo(x + len * 0.8f, y + len * 0.5f, x + len * 0.3f, y + len * 0.4f, x, y)
            close()
        }
        canvas.drawPath(leaf, Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color })
        canvas.restore()
    }

    private fun drawBlossom(canvas: Canvas, cx: Float, cy: Float, s: Float, @ColorInt petalColor: Int) {
        val glow = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = withAlpha(petalColor, 0.32f) }
        canvas.drawCircle(cx, cy, s * 0.052f, glow)
        val petalPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = mix(petalColor, Color.WHITE, 0.35f) }
        val r = s * 0.027f
        val offsetR = s * 0.023f
        for (i in 0 until 5) {
            val angle = (i * 72f) * (PI.toFloat() / 180f)
            canvas.drawCircle(cx + cos(angle) * offsetR, cy + sin(angle) * offsetR, r, petalPaint)
        }
        canvas.drawCircle(cx, cy, s * 0.015f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(0xFF, 0xD5, 0x4F) })
    }

    private fun strokePaint(@ColorInt color: Int, width: Float): Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        style = Paint.Style.STROKE
        strokeWidth = width
        strokeCap = Paint.Cap.ROUND
    }

    private fun bitmap(size: Int, draw: (Canvas, Float) -> Unit): Bitmap {
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        draw(Canvas(bitmap), size.toFloat())
        return bitmap
    }

    private fun gradientPaint(
        size: Float,
        topY: Float,
        bottomY: Float,
        @ColorInt top: Int,
        @ColorInt bottom: Int
    ): Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = LinearGradient(
            0f,
            size * topY,
            0f,
            size * bottomY,
            top,
            bottom,
            Shader.TileMode.CLAMP
        )
    }

    private fun outerFlamePath(size: Float): Path = Path().apply {
        moveTo(size * 0.5f, size * 0.09f)
        cubicTo(size * 0.79f, size * 0.31f, size * 0.92f, size * 0.69f, size * 0.5f, size * 0.93f)
        cubicTo(size * 0.08f, size * 0.69f, size * 0.21f, size * 0.31f, size * 0.5f, size * 0.09f)
        close()
    }

    private fun innerFlamePath(size: Float): Path = Path().apply {
        moveTo(size * 0.5f, size * 0.43f)
        cubicTo(size * 0.68f, size * 0.57f, size * 0.78f, size * 0.77f, size * 0.5f, size * 0.88f)
        cubicTo(size * 0.22f, size * 0.77f, size * 0.32f, size * 0.57f, size * 0.5f, size * 0.43f)
        close()
    }

    @ColorInt
    private fun mix(@ColorInt from: Int, @ColorInt to: Int, amount: Float): Int = Color.rgb(
        channel(Color.red(from), Color.red(to), amount),
        channel(Color.green(from), Color.green(to), amount),
        channel(Color.blue(from), Color.blue(to), amount)
    )

    private fun channel(from: Int, to: Int, amount: Float): Int =
        (from + (to - from) * amount).roundToInt().coerceIn(0, 255)

    @ColorInt
    private fun withAlpha(@ColorInt color: Int, alpha: Float): Int = Color.argb(
        (alpha * 255).roundToInt().coerceIn(0, 255),
        Color.red(color),
        Color.green(color),
        Color.blue(color)
    )
}
