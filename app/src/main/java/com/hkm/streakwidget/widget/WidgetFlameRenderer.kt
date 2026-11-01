package com.hkm.streakwidget.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import androidx.annotation.ColorInt
import kotlin.math.roundToInt

internal data class WidgetFlameLayers(
    val glow: Bitmap,
    val outer: Bitmap,
    val mid: Bitmap,
    val inner: Bitmap
)

/**
 * Draws the same teardrop, vertical-gradient flame the Compose app uses, but into bitmaps a
 * RemoteViews ImageView can display. Flat vector fills and color filters are what made the old
 * widget ember look cheap next to the in-app flame.
 */
internal object WidgetFlameRenderer {
    private const val SIZE = 128
    private val cache = LinkedHashMap<String, WidgetFlameLayers>(16, 0.75f, true)

    @Synchronized
    fun layers(@ColorInt accent: Int, lit: Boolean): WidgetFlameLayers {
        val key = "$accent:$lit"
        cache[key]?.let { return it }
        val rendered = render(accent, lit)
        if (cache.size >= 12) cache.remove(cache.keys.first())
        cache[key] = rendered
        return rendered
    }

    private fun render(@ColorInt accent: Int, lit: Boolean): WidgetFlameLayers {
        val outerTop = if (lit) mix(accent, Color.WHITE, 0.16f) else Color.rgb(0x62, 0x62, 0x62)
        val outerBottom = if (lit) mix(accent, Color.BLACK, 0.16f) else Color.rgb(0x36, 0x36, 0x36)
        val midTop = if (lit) mix(accent, Color.WHITE, 0.52f) else Color.rgb(0x8C, 0x8C, 0x8C)
        val midBottom = if (lit) mix(accent, Color.WHITE, 0.12f) else Color.rgb(0x4A, 0x4A, 0x4A)
        val innerTop = if (lit) mix(accent, Color.WHITE, 0.88f) else Color.rgb(0xA8, 0xA8, 0xA8)
        val innerBottom = if (lit) mix(accent, Color.WHITE, 0.45f) else Color.rgb(0x6E, 0x6E, 0x6E)

        return WidgetFlameLayers(
            glow = bitmap { canvas ->
                if (lit) {
                    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        shader = RadialGradient(
                            SIZE * 0.5f,
                            SIZE * 0.58f,
                            SIZE * 0.72f,
                            intArrayOf(withAlpha(accent, 0.30f), withAlpha(accent, 0.09f), Color.TRANSPARENT),
                            floatArrayOf(0f, 0.5f, 1f),
                            Shader.TileMode.CLAMP
                        )
                    }
                    canvas.drawRect(0f, 0f, SIZE.toFloat(), SIZE.toFloat(), paint)
                }
            },
            outer = bitmap { canvas ->
                val paint = gradientPaint(0.09f, 0.93f, outerTop, outerBottom)
                canvas.drawPath(outerPath(), paint)
            },
            mid = bitmap { canvas ->
                val paint = gradientPaint(0.09f, 0.93f, midTop, midBottom)
                canvas.save()
                canvas.scale(0.72f, 0.72f, SIZE * 0.5f, SIZE * 0.93f)
                canvas.drawPath(outerPath(), paint)
                canvas.restore()
            },
            inner = bitmap { canvas ->
                val paint = gradientPaint(0.43f, 0.88f, innerTop, innerBottom)
                canvas.drawPath(innerPath(), paint)
            }
        )
    }

    private fun bitmap(draw: (Canvas) -> Unit): Bitmap {
        val bitmap = Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888)
        draw(Canvas(bitmap))
        return bitmap
    }

    private fun gradientPaint(topY: Float, bottomY: Float, @ColorInt top: Int, @ColorInt bottom: Int): Paint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f,
                SIZE * topY,
                0f,
                SIZE * bottomY,
                top,
                bottom,
                Shader.TileMode.CLAMP
            )
        }

    private fun outerPath(): Path {
        val w = SIZE.toFloat()
        val h = SIZE.toFloat()
        return Path().apply {
            moveTo(w * 0.5f, h * 0.09f)
            cubicTo(w * 0.79f, h * 0.31f, w * 0.92f, h * 0.69f, w * 0.5f, h * 0.93f)
            cubicTo(w * 0.08f, h * 0.69f, w * 0.21f, h * 0.31f, w * 0.5f, h * 0.09f)
            close()
        }
    }

    private fun innerPath(): Path {
        val w = SIZE.toFloat()
        val h = SIZE.toFloat()
        return Path().apply {
            moveTo(w * 0.5f, h * 0.43f)
            cubicTo(w * 0.68f, h * 0.57f, w * 0.78f, h * 0.77f, w * 0.5f, h * 0.88f)
            cubicTo(w * 0.22f, h * 0.77f, w * 0.32f, h * 0.57f, w * 0.5f, h * 0.43f)
            close()
        }
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
