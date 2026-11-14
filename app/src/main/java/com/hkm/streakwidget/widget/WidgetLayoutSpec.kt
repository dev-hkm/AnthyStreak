package com.hkm.streakwidget.widget

import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Self-contained layout topologies covering every launcher crop from a 1x1 tile up to
 * full-screen grids, strips, and tall columns.
 */
internal enum class WidgetStructure {
    MICRO_ROW,
    MICRO_STACK,
    STRIP_ROW,
    COMPACT_SPLIT,
    COLUMN,
    SQUARE,
    POSTER_STACK,
    HERO
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

/** Exact launcher frame in dp. */
internal data class WidgetFrame(val widthDp: Int, val heightDp: Int)

/** Complete per-frame geometry, visibility, and typography specification for a widget. */
internal data class WidgetPlan(
    val structure: WidgetStructure,
    val paddingDp: Int,
    val gapDp: Int,
    val flameSizeDp: Int,
    val compactCorners: Boolean,
    val useBalanceSpacers: Boolean,
    val useLongStreakLabel: Boolean,
    val showLabel: Boolean,
    val showEyebrow: Boolean,
    val showName: Boolean,
    val showStatus: Boolean,
    val showProgress: Boolean,
    val showAction: Boolean,
    val nameMaxLines: Int,
    val countSp: Float,
    val labelSp: Float,
    val eyebrowSp: Float,
    val nameSp: Float,
    val statusSp: Float,
    val actionSp: Float,
    val actionHeightDp: Int
)

internal object WidgetLayoutSpec {
    const val DEFAULT_WIDTH_DP = 140
    const val DEFAULT_HEIGHT_DP = 140

    /** Canonical grid of aspect ratios so API 31+ resize drags transition smoothly. */
    val canonicalFrames: List<WidgetFrame> = listOf(
        WidgetFrame(56, 48),
        WidgetFrame(68, 68),
        WidgetFrame(110, 48),
        WidgetFrame(140, 74),
        WidgetFrame(180, 56),
        WidgetFrame(270, 58),
        WidgetFrame(68, 136),
        WidgetFrame(68, 220),
        WidgetFrame(118, 118),
        WidgetFrame(120, 195),
        WidgetFrame(175, 175),
        WidgetFrame(195, 115),
        WidgetFrame(270, 140),
        WidgetFrame(270, 240)
    )

    /**
     * Merges exact launcher frames from [OPTION_APPWIDGET_SIZES] with canonical resize anchors
     * that aren't redundant with an exact frame, keeping Parcel size compact while ensuring
     * 0dp error on settled portrait/landscape sizes and smooth transitions during drag.
     */
    fun responsiveFrames(
        launcherFrames: List<WidgetFrame>,
        fallbackWidthDp: Int,
        fallbackHeightDp: Int
    ): List<WidgetFrame> {
        val validLauncher = launcherFrames
            .filter { it.widthDp in 24..2000 && it.heightDp in 24..2000 }
            .distinct()
        val result = mutableListOf<WidgetFrame>()
        if (validLauncher.isNotEmpty()) {
            result.addAll(validLauncher)
        } else {
            result.add(
                WidgetFrame(
                    normalized(fallbackWidthDp, DEFAULT_WIDTH_DP),
                    normalized(fallbackHeightDp, DEFAULT_HEIGHT_DP)
                )
            )
        }
        canonicalFrames.forEach { anchor ->
            val covered = result.any { existing ->
                abs(existing.widthDp - anchor.widthDp) <= 14 &&
                    abs(existing.heightDp - anchor.heightDp) <= 14
            }
            if (!covered) result.add(anchor)
        }
        return result
    }

    fun closestFrame(frames: List<WidgetFrame>, widthDp: Int, heightDp: Int): WidgetFrame? {
        val width = normalized(widthDp, DEFAULT_WIDTH_DP)
        val height = normalized(heightDp, DEFAULT_HEIGHT_DP)
        return frames
            .filter { it.widthDp in 24..2000 && it.heightDp in 24..2000 }
            .minByOrNull { abs(it.widthDp - width) + abs(it.heightDp - height) }
    }

    fun structureFor(widthDp: Int, heightDp: Int, style: WidgetStyle = WidgetStyle.FOCUS): WidgetStructure {
        val width = normalized(widthDp, DEFAULT_WIDTH_DP)
        val height = normalized(heightDp, DEFAULT_HEIGHT_DP)
        return when {
            width < 104 && height < 94 ->
                if (width >= height * 1.05f) WidgetStructure.MICRO_ROW else WidgetStructure.MICRO_STACK

            (width < 104 && height >= 94) || height >= width * 1.38f ->
                WidgetStructure.COLUMN

            width >= 104 && height < 94 ->
                if (style != WidgetStyle.EMBER && width in 118..175 && height >= 64) {
                    WidgetStructure.COMPACT_SPLIT
                } else {
                    WidgetStructure.STRIP_ROW
                }

            width >= 176 && height >= 94 && width >= height * 1.38f ->
                when {
                    style == WidgetStyle.EMBER && width >= height * 1.65f -> WidgetStructure.STRIP_ROW
                    style == WidgetStyle.EMBER -> WidgetStructure.POSTER_STACK
                    else -> WidgetStructure.HERO
                }

            style == WidgetStyle.EMBER || style == WidgetStyle.POSTER ->
                WidgetStructure.POSTER_STACK

            else ->
                WidgetStructure.SQUARE
        }
    }

    fun plan(
        widthDp: Int,
        heightDp: Int,
        style: WidgetStyle,
        hasChecklist: Boolean,
        countDigits: Int = 2,
        actionCharCount: Int = 8
    ): WidgetPlan {
        val width = normalized(widthDp, DEFAULT_WIDTH_DP)
        val height = normalized(heightDp, DEFAULT_HEIGHT_DP)
        val digits = countDigits.coerceIn(1, 5)
        val actionChars = actionCharCount.coerceIn(4, 18)
        val effectiveStyle = resolvedStyle(width, height, style, hasChecklist)
        val structure = structureFor(width, height, effectiveStyle)
        val compactCorners = min(width, height) < 88

        return when (structure) {
            WidgetStructure.MICRO_ROW -> {
                val pad = (min(width, height) * 0.08f).roundToInt().coerceIn(3, 6)
                val innerW = (width - pad * 2).toFloat()
                val innerH = (height - pad * 2).toFloat()
                val countWidth = innerW * 0.46f
                val showLabel = width >= 80 && height >= 58
                WidgetPlan(
                    structure = structure,
                    paddingDp = pad,
                    gapDp = 3,
                    flameSizeDp = innerH.roundToInt().coerceAtLeast(24),
                    compactCorners = true,
                    useBalanceSpacers = false,
                    useLongStreakLabel = false,
                    showLabel = showLabel,
                    showEyebrow = false,
                    showName = false,
                    showStatus = false,
                    showProgress = false,
                    showAction = false,
                    nameMaxLines = 1,
                    countSp = fitNumericSp(min(innerH * 0.46f, innerW * 0.28f), countWidth, digits, 8.5f, 24f),
                    labelSp = (min(width, height) * 0.105f).coerceIn(6.5f, 8.5f),
                    eyebrowSp = 7f,
                    nameSp = 10f,
                    statusSp = 7f,
                    actionSp = 8f,
                    actionHeightDp = 22
                )
            }

            WidgetStructure.MICRO_STACK -> {
                val pad = (min(width, height) * 0.085f).roundToInt().coerceIn(3, 6)
                val innerW = (width - pad * 2).toFloat()
                val innerH = (height - pad * 2).toFloat()
                val showLabel = height >= 74 && width >= 66
                WidgetPlan(
                    structure = structure,
                    paddingDp = pad,
                    gapDp = 2,
                    flameSizeDp = min(innerW, innerH * 0.6f).roundToInt().coerceAtLeast(22),
                    compactCorners = true,
                    useBalanceSpacers = false,
                    useLongStreakLabel = false,
                    showLabel = showLabel,
                    showEyebrow = false,
                    showName = false,
                    showStatus = false,
                    showProgress = false,
                    showAction = false,
                    nameMaxLines = 1,
                    countSp = fitNumericSp(innerH * 0.27f, innerW, digits, 9.5f, 24f),
                    labelSp = (innerW * 0.105f).coerceIn(6.5f, 8.5f),
                    eyebrowSp = 7f,
                    nameSp = 10f,
                    statusSp = 7f,
                    actionSp = 8f,
                    actionHeightDp = 22
                )
            }

            WidgetStructure.COMPACT_SPLIT -> {
                val pad = (height * 0.09f).roundToInt().coerceIn(5, 8)
                val innerW = (width - pad * 2).toFloat()
                val innerH = (height - pad * 2).toFloat()
                val gap = 6
                val leftW = (innerW - gap) * (0.82f / 2.20f)
                val rightW = (innerW - gap) * (1.38f / 2.20f)
                val showLabel = height >= 78
                val showProgress = effectiveStyle == WidgetStyle.CHECKLIST && hasChecklist && height >= 80
                val showStatus = effectiveStyle != WidgetStyle.CHECKLIST && height >= 84
                val nameMaxLines = if (!showStatus && !showProgress && height >= 78) 2 else 1
                val actionH = (innerH * 0.34f).roundToInt().coerceIn(22, 27)
                WidgetPlan(
                    structure = structure,
                    paddingDp = pad,
                    gapDp = gap,
                    flameSizeDp = min(leftW, innerH * 0.55f).roundToInt().coerceAtLeast(24),
                    compactCorners = true,
                    useBalanceSpacers = false,
                    useLongStreakLabel = false,
                    showLabel = showLabel,
                    showEyebrow = false,
                    showName = true,
                    showStatus = showStatus,
                    showProgress = showProgress,
                    showAction = true,
                    nameMaxLines = nameMaxLines,
                    countSp = fitNumericSp(innerH * 0.26f, leftW, digits, 11f, 22f),
                    labelSp = (height * 0.088f).coerceIn(6.5f, 8f),
                    eyebrowSp = 7f,
                    nameSp = (height * 0.165f).coerceIn(11f, 14f),
                    statusSp = (height * 0.10f).coerceIn(7f, 8.5f),
                    actionSp = fitLabelSp(8.5f, rightW - 10f, actionChars, 6.2f, 9.5f),
                    actionHeightDp = actionH
                )
            }

            WidgetStructure.STRIP_ROW -> {
                val pad = (height * 0.095f).roundToInt().coerceIn(4, 14)
                val innerW = (width - pad * 2).toFloat()
                val innerH = (height - pad * 2).toFloat()
                val flameDp = innerH.roundToInt().coerceIn(28, 110)
                val showLabel = height >= 52
                val countSp = fitNumericSp(innerH * 0.44f, (innerW * 0.26f).coerceAtLeast(30f), digits, 12f, 40f)
                val streakBadgeW = (digits * countSp * 0.65f + 4f).coerceAtLeast(if (showLabel) 32f else 22f)
                val gap = (height * 0.11f).roundToInt().coerceIn(6, 14)
                val remAfterStreak = innerW - flameDp - streakBadgeW - gap
                val minDetailW = 54f
                val actionEstW = (actionChars * 5.3f + 20f).coerceIn(52f, 92f)
                val allowsDetail = effectiveStyle != WidgetStyle.EMBER
                val showAction = allowsDetail && remAfterStreak >= minDetailW + actionEstW + gap * 2
                val showName = allowsDetail && (showAction || remAfterStreak >= minDetailW + gap)
                val detailW = (remAfterStreak - (if (showAction) actionEstW + gap * 2f else gap.toFloat())).coerceAtLeast(40f)
                val showEyebrow = showName && height >= 78 && detailW >= 88f
                val showProgress = showName && effectiveStyle == WidgetStyle.CHECKLIST && hasChecklist && height >= 58
                val showStatus = showName && height >= 58 && (!showProgress || height >= 84)
                val nameMaxLines = if (showName && !showEyebrow && !showStatus && !showProgress && height >= 72) 2 else 1
                val actionH = (innerH * 0.56f).roundToInt().coerceIn(26, 40)
                WidgetPlan(
                    structure = structure,
                    paddingDp = pad,
                    gapDp = gap,
                    flameSizeDp = flameDp,
                    compactCorners = compactCorners,
                    useBalanceSpacers = false,
                    useLongStreakLabel = false,
                    showLabel = showLabel,
                    showEyebrow = showEyebrow,
                    showName = showName,
                    showStatus = showStatus,
                    showProgress = showProgress,
                    showAction = showAction,
                    nameMaxLines = nameMaxLines,
                    countSp = countSp,
                    labelSp = (innerH * 0.145f).coerceIn(6.5f, 10.5f),
                    eyebrowSp = (innerH * 0.125f).coerceIn(7f, 10f),
                    nameSp = (innerH * 0.25f).coerceIn(12f, 19f),
                    statusSp = (innerH * 0.15f).coerceIn(7.5f, 11f),
                    actionSp = fitLabelSp((innerH * 0.17f).coerceIn(8f, 11f), actionEstW - 14f, actionChars, 7f, 11f),
                    actionHeightDp = actionH
                )
            }

            WidgetStructure.COLUMN -> {
                val pad = (min(width, height) * 0.085f).roundToInt().coerceIn(5, 16)
                val innerW = (width - pad * 2).toFloat()
                val innerH = (height - pad * 2).toFloat()
                val isEmber = effectiveStyle == WidgetStyle.EMBER
                val isPoster = effectiveStyle == WidgetStyle.POSTER
                val showLabel = width >= 54
                val showName = !isEmber && height >= 112
                val showEyebrow = showName && !isPoster && width >= 110 && height >= 195
                val showProgress = showName && effectiveStyle == WidgetStyle.CHECKLIST && hasChecklist && height >= 162 && width >= 72
                val showStatus = showName && !isPoster && height >= 172 && width >= 76 && (!showProgress || height >= 218)
                val showAction = !isEmber && width >= 62 && height >= 132
                val nameMaxLines = if (showName && height >= 148 && width >= 82) 2 else 1
                val countTarget = min(innerW * 0.34f, innerH * 0.18f) * (if (isPoster) 1.15f else 1f)
                val actionH = (innerH * 0.15f).roundToInt().coerceIn(24, 38)
                WidgetPlan(
                    structure = structure,
                    paddingDp = pad,
                    gapDp = 6,
                    flameSizeDp = min(innerW, innerH * 0.46f).roundToInt().coerceAtLeast(28),
                    compactCorners = compactCorners,
                    useBalanceSpacers = height >= width * 1.85f,
                    useLongStreakLabel = width >= 96,
                    showLabel = showLabel,
                    showEyebrow = showEyebrow,
                    showName = showName,
                    showStatus = showStatus,
                    showProgress = showProgress,
                    showAction = showAction,
                    nameMaxLines = nameMaxLines,
                    countSp = fitNumericSp(countTarget, innerW, digits, 14f, 44f),
                    labelSp = (innerW * 0.105f).coerceIn(6.5f, 10.5f),
                    eyebrowSp = (innerW * 0.085f).coerceIn(7.5f, 10f),
                    nameSp = (innerW * 0.145f).coerceIn(10f, 18f),
                    statusSp = (innerW * 0.095f).coerceIn(7.5f, 10.5f),
                    actionSp = fitLabelSp((innerW * 0.10f).coerceIn(7.5f, 10.5f), innerW - 10f, actionChars, 6f, 10.5f),
                    actionHeightDp = actionH
                )
            }

            WidgetStructure.SQUARE -> {
                val minSide = min(width, height)
                val pad = (minSide * 0.078f).roundToInt().coerceIn(7, 20)
                val innerW = (width - pad * 2).toFloat()
                val innerH = (height - pad * 2).toFloat()
                val countW = innerW * 0.44f
                val showEyebrow = height >= 185
                val showProgress = effectiveStyle == WidgetStyle.CHECKLIST && hasChecklist && height >= 132
                val showStatus = height >= 136 && (!showProgress || height >= 175)
                val nameMaxLines = if (height >= 162) 2 else 1
                val actionH = (innerH * 0.20f).roundToInt().coerceIn(24, 42)
                WidgetPlan(
                    structure = structure,
                    paddingDp = pad,
                    gapDp = (minSide * 0.05f).roundToInt().coerceIn(6, 14),
                    flameSizeDp = min(innerW * 0.48f, innerH * 0.48f).roundToInt().coerceAtLeast(36),
                    compactCorners = false,
                    useBalanceSpacers = false,
                    useLongStreakLabel = width >= 140,
                    showLabel = true,
                    showEyebrow = showEyebrow,
                    showName = true,
                    showStatus = showStatus,
                    showProgress = showProgress,
                    showAction = true,
                    nameMaxLines = nameMaxLines,
                    countSp = fitNumericSp(min(innerW * 0.23f, innerH * 0.22f), countW, digits, 15f, 46f),
                    labelSp = (minSide * 0.068f).coerceIn(7f, 11.5f),
                    eyebrowSp = (minSide * 0.058f).coerceIn(7.5f, 10.5f),
                    nameSp = (minSide * 0.112f).coerceIn(12f, 21f),
                    statusSp = (minSide * 0.068f).coerceIn(7.5f, 11f),
                    actionSp = fitLabelSp((minSide * 0.075f).coerceIn(8f, 11.5f), innerW - 14f, actionChars, 7f, 11.5f),
                    actionHeightDp = actionH
                )
            }

            WidgetStructure.POSTER_STACK -> {
                val minSide = min(width, height)
                val pad = (minSide * 0.08f).roundToInt().coerceIn(7, 20)
                val innerW = (width - pad * 2).toFloat()
                val innerH = (height - pad * 2).toFloat()
                val isPoster = effectiveStyle == WidgetStyle.POSTER
                val showName = isPoster
                val showStatus = isPoster && height >= 168
                val showAction = isPoster && height >= 126
                val nameMaxLines = if (isPoster && height >= 195) 2 else 1
                val actionH = (innerH * 0.18f).roundToInt().coerceIn(24, 40)
                WidgetPlan(
                    structure = structure,
                    paddingDp = pad,
                    gapDp = 6,
                    flameSizeDp = min(innerW * 0.72f, innerH * 0.48f).roundToInt().coerceAtLeast(40),
                    compactCorners = false,
                    useBalanceSpacers = false,
                    useLongStreakLabel = width >= 110,
                    showLabel = true,
                    showEyebrow = false,
                    showName = showName,
                    showStatus = showStatus,
                    showProgress = false,
                    showAction = showAction,
                    nameMaxLines = nameMaxLines,
                    countSp = fitNumericSp(min(innerW * 0.27f, innerH * 0.25f), innerW, digits, 18f, 54f),
                    labelSp = (minSide * 0.068f).coerceIn(7.5f, 11.5f),
                    eyebrowSp = 8f,
                    nameSp = (minSide * 0.105f).coerceIn(11f, 19f),
                    statusSp = (minSide * 0.065f).coerceIn(7.5f, 11f),
                    actionSp = fitLabelSp((minSide * 0.074f).coerceIn(8f, 11.5f), innerW - 14f, actionChars, 7f, 11.5f),
                    actionHeightDp = actionH
                )
            }

            WidgetStructure.HERO -> {
                val minSide = min(width, height)
                val pad = (minSide * 0.095f).roundToInt().coerceIn(9, 22)
                val innerW = (width - pad * 2).toFloat()
                val innerH = (height - pad * 2).toFloat()
                val gap = (width * 0.045f).roundToInt().coerceIn(8, 18)
                val isPoster = effectiveStyle == WidgetStyle.POSTER
                val leftW = (innerW - gap) * (0.9f / 2.4f)
                val rightW = (innerW - gap) * (1.5f / 2.4f)
                val showEyebrow = !isPoster && height >= 112
                val showProgress = effectiveStyle == WidgetStyle.CHECKLIST && hasChecklist
                val showStatus = !isPoster && (!showProgress || height >= 138)
                val nameMaxLines = if (height >= 126) 2 else 1
                val actionH = (innerH * 0.26f).roundToInt().coerceIn(28, 44)
                val countTarget = min(leftW * 0.42f, innerH * 0.28f) * (if (isPoster) 1.12f else 1f)
                WidgetPlan(
                    structure = structure,
                    paddingDp = pad,
                    gapDp = gap,
                    flameSizeDp = min(leftW, innerH * 0.56f).roundToInt().coerceAtLeast(40),
                    compactCorners = false,
                    useBalanceSpacers = false,
                    useLongStreakLabel = leftW >= 62f,
                    showLabel = true,
                    showEyebrow = showEyebrow,
                    showName = true,
                    showStatus = showStatus,
                    showProgress = showProgress,
                    showAction = true,
                    nameMaxLines = nameMaxLines,
                    countSp = fitNumericSp(countTarget, leftW, digits, 16f, 48f),
                    labelSp = (minSide * 0.068f).coerceIn(8f, 11.5f),
                    eyebrowSp = (minSide * 0.062f).coerceIn(8f, 10.5f),
                    nameSp = (minSide * 0.132f).coerceIn(14f, 24f),
                    statusSp = (minSide * 0.07f).coerceIn(8.5f, 11.5f),
                    actionSp = fitLabelSp((minSide * 0.078f).coerceIn(9f, 12f), rightW - 16f, actionChars, 7.5f, 12f),
                    actionHeightDp = actionH
                )
            }
        }
    }

    private fun normalized(value: Int, fallback: Int): Int =
        if (value > 0) value.coerceIn(40, 2000) else fallback

    private fun fitNumericSp(
        targetSp: Float,
        availableWidthDp: Float,
        digits: Int,
        minSp: Float,
        maxSp: Float
    ): Float {
        val widthCap = availableWidthDp / (digits * 0.65f + 0.25f)
        return min(targetSp, widthCap).coerceIn(minSp, maxSp)
    }

    private fun fitLabelSp(
        targetSp: Float,
        availableWidthDp: Float,
        charCount: Int,
        minSp: Float,
        maxSp: Float
    ): Float {
        val widthCap = availableWidthDp.coerceAtLeast(24f) / (charCount.coerceAtLeast(4) * 0.62f)
        return min(targetSp, widthCap).coerceIn(minSp, maxSp)
    }
}

internal fun resolvedStyle(
    widthDp: Int,
    heightDp: Int,
    selected: WidgetStyle,
    hasChecklist: Boolean
): WidgetStyle = when {
    widthDp < 104 && heightDp < 94 -> WidgetStyle.EMBER
    selected == WidgetStyle.CHECKLIST && !hasChecklist -> WidgetStyle.FOCUS
    else -> selected
}
