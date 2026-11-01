package com.hkm.streakwidget.widget

import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Structural arrangements. Each one is a complete, self-filling layout; the exact
 * typography, padding and visible content are computed per launcher frame by [WidgetLayoutSpec].
 */
internal enum class WidgetStructure {
    MINI,
    ROW,
    COLUMN,
    STACK
}

enum class WidgetStyle {
    EMBER,
    FOCUS,
    CHECKLIST,
    POSTER;

    companion object {
        fun fromStored(value: String?): WidgetStyle =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: FOCUS
    }
}

/** Everything a RemoteViews needs to make one widget frame look deliberate at its own size. */
internal data class WidgetPlan(
    val structure: WidgetStructure,
    val paddingDp: Int,
    val compactCorners: Boolean,
    val showLabel: Boolean,
    val showName: Boolean,
    val showStatus: Boolean,
    val showProgress: Boolean,
    val showAction: Boolean,
    val countSp: Float,
    val labelSp: Float,
    val nameSp: Float,
    val statusSp: Float,
    val actionSp: Float,
    val actionHorizontalPaddingDp: Int,
    val actionVerticalPaddingDp: Int
)

internal object WidgetLayoutSpec {
    const val DEFAULT_WIDTH_DP = 110
    const val DEFAULT_HEIGHT_DP = 75

    fun structureFor(widthDp: Int, heightDp: Int): WidgetStructure {
        val width = normalized(widthDp, DEFAULT_WIDTH_DP)
        val height = normalized(heightDp, DEFAULT_HEIGHT_DP)
        return when {
            width < 76 && height < 76 -> WidgetStructure.MINI
            width >= 72 && height >= width * 1.45f -> WidgetStructure.COLUMN
            width >= height * 1.25f -> WidgetStructure.ROW
            else -> WidgetStructure.STACK
        }
    }

    /**
     * Builds the frame plan from the size the launcher actually reports. Nothing here is a
     * fixed bucket: every text size is derived from the available dp, so the widget keeps its
     * hierarchy from the smallest crop to a full hero card, and long streak numbers shrink to fit.
     */
    fun plan(
        widthDp: Int,
        heightDp: Int,
        style: WidgetStyle,
        hasChecklist: Boolean,
        countDigits: Int = 2
    ): WidgetPlan {
        val width = normalized(widthDp, DEFAULT_WIDTH_DP)
        val height = normalized(heightDp, DEFAULT_HEIGHT_DP)
        val digits = countDigits.coerceIn(1, 5)
        val showsDetail = style == WidgetStyle.FOCUS || style == WidgetStyle.CHECKLIST
        return when (structureFor(width, height)) {
            WidgetStructure.MINI -> {
                val padding = if (min(width, height) < 52) 2 else 3
                val textWidth = (width - padding * 2) * 0.4f
                WidgetPlan(
                    structure = WidgetStructure.MINI,
                    paddingDp = padding,
                    compactCorners = true,
                    showLabel = false,
                    showName = false,
                    showStatus = false,
                    showProgress = false,
                    showAction = false,
                    countSp = fitText(min(width, height) * 0.30f, textWidth, digits, 9f, 22f),
                    labelSp = 6f,
                    nameSp = 8f,
                    statusSp = 7f,
                    actionSp = 7f,
                    actionHorizontalPaddingDp = 8,
                    actionVerticalPaddingDp = 2
                )
            }

            WidgetStructure.ROW -> {
                val padding = (height * 0.07f).roundToInt().coerceIn(3, 7)
                val showAction = style != WidgetStyle.EMBER && width >= 128 && height >= 58
                val showName = showsDetail && width >= 132 && height >= 72
                val showStatus = showsDetail && width >= 150 && height >= 96
                val showProgress = showsDetail && hasChecklist && width >= 150 && height >= 96
                val flameWidth = min(height.toFloat(), width * 0.34f).coerceAtLeast(26f)
                val actionWidth = if (showAction) (height * 0.52f).coerceIn(42f, 72f) else 0f
                val textWidth = (width - padding * 2 - flameWidth - actionWidth - 10f).coerceAtLeast(40f)
                val lines = 2 + (if (showName) 1 else 0) + (if (showStatus || showProgress) 1 else 0)
                val heightFactor = when (lines) {
                    2 -> 0.46f
                    3 -> 0.40f
                    else -> 0.34f
                }
                WidgetPlan(
                    structure = WidgetStructure.ROW,
                    paddingDp = padding,
                    compactCorners = min(width, height) < 72,
                    showLabel = true,
                    showName = showName,
                    showStatus = showStatus,
                    showProgress = showProgress,
                    showAction = showAction,
                    countSp = fitText(height * heightFactor, textWidth, digits, 11f, 44f),
                    labelSp = (height * 0.13f).coerceIn(6f, 13f),
                    nameSp = (height * 0.19f).coerceIn(9f, 16f),
                    statusSp = (height * 0.12f).coerceIn(7f, 11f),
                    actionSp = (height * 0.15f).coerceIn(7f, 12f),
                    actionHorizontalPaddingDp = (height * 0.12f).roundToInt().coerceIn(7, 16),
                    actionVerticalPaddingDp = (height * 0.035f).roundToInt().coerceIn(2, 5)
                )
            }

            WidgetStructure.COLUMN -> {
                val padding = (min(width, height) * 0.07f).roundToInt().coerceIn(4, 9)
                val showName = showsDetail && height >= 118 && width >= 64
                val showStatus = showsDetail && height >= 168 && width >= 84
                val showProgress = showsDetail && hasChecklist && height >= 168 && width >= 84
                val showAction = style != WidgetStyle.EMBER && height >= 178 && width >= 76
                WidgetPlan(
                    structure = WidgetStructure.COLUMN,
                    paddingDp = padding,
                    compactCorners = min(width, height) < 72,
                    showLabel = true,
                    showName = showName,
                    showStatus = showStatus,
                    showProgress = showProgress,
                    showAction = showAction,
                    countSp = fitText(width * 0.34f, (width - padding * 2).toFloat(), digits, 16f, 40f),
                    labelSp = (width * 0.105f).coerceIn(6f, 12f),
                    nameSp = (width * 0.135f).coerceIn(9f, 15f),
                    statusSp = (width * 0.10f).coerceIn(7f, 11f),
                    actionSp = (width * 0.12f).coerceIn(7f, 12f),
                    actionHorizontalPaddingDp = (width * 0.10f).roundToInt().coerceIn(8, 16),
                    actionVerticalPaddingDp = 3
                )
            }

            WidgetStructure.STACK -> {
                val padding = (min(width, height) * 0.06f).roundToInt().coerceIn(3, 7)
                val showName = showsDetail && height >= 70 && width >= 56
                val showStatus = showsDetail && height >= 120 && width >= 84
                val showProgress = showsDetail && hasChecklist && height >= 120 && width >= 84
                val showAction = style != WidgetStyle.EMBER && height >= 92 && width >= 62
                val countColumnWidth = (width - padding * 2) * 0.42f
                WidgetPlan(
                    structure = WidgetStructure.STACK,
                    paddingDp = padding,
                    compactCorners = min(width, height) < 72,
                    showLabel = height >= 56,
                    showName = showName,
                    showStatus = showStatus,
                    showProgress = showProgress,
                    showAction = showAction,
                    countSp = fitText(min(width, height) * 0.30f, countColumnWidth, digits, 13f, 34f),
                    labelSp = (width * 0.10f).coerceIn(6f, 12f),
                    nameSp = (width * 0.115f).coerceIn(9f, 15f),
                    statusSp = (width * 0.09f).coerceIn(7f, 11f),
                    actionSp = (width * 0.11f).coerceIn(7f, 12f),
                    actionHorizontalPaddingDp = (width * 0.09f).roundToInt().coerceIn(7, 16),
                    actionVerticalPaddingDp = 3
                )
            }
        }
    }

    private fun normalized(value: Int, fallback: Int): Int = if (value in 1..2000) value else fallback

    /** Keeps the number legible without letting it spill over the space reserved for it. */
    private fun fitText(preferredSp: Float, widthDp: Float, digits: Int, minSp: Float, maxSp: Float): Float {
        val byWidth = widthDp / (0.62f * digits)
        return minOf(preferredSp, byWidth).coerceIn(minSp, maxSp)
    }
}

internal fun resolvedStyle(structure: WidgetStructure, selected: WidgetStyle, hasChecklist: Boolean): WidgetStyle = when {
    structure == WidgetStructure.MINI -> WidgetStyle.EMBER
    selected == WidgetStyle.CHECKLIST && !hasChecklist -> WidgetStyle.FOCUS
    else -> selected
}
