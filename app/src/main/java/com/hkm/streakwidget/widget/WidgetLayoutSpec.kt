package com.hkm.streakwidget.widget

enum class WidgetSize(val minWidthDp: Int, val minHeightDp: Int) {
    TILE(56, 48),
    STRIP(110, 48),
    TALL(56, 80),
    COMPACT(110, 75),
    WIDE(180, 64),
    SQUARE(110, 110),
    TALL_HERO(110, 160),
    HERO(180, 110)
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

object WidgetLayoutSpec {
    /** Ascending buckets so RemoteViews picks the smallest variant that covers the frame. */
    val responsiveSizes: List<WidgetSize> = listOf(
        WidgetSize.TILE,
        WidgetSize.STRIP,
        WidgetSize.TALL,
        WidgetSize.COMPACT,
        WidgetSize.WIDE,
        WidgetSize.SQUARE,
        WidgetSize.TALL_HERO,
        WidgetSize.HERO
    )

    fun classify(widthDp: Int, heightDp: Int): WidgetSize {
        if (widthDp <= 0 || heightDp <= 0) return WidgetSize.COMPACT
        return when {
            widthDp >= 180 && heightDp >= 110 -> WidgetSize.HERO
            widthDp >= 110 && heightDp >= 160 -> WidgetSize.TALL_HERO
            widthDp >= 180 && heightDp >= 64 -> WidgetSize.WIDE
            widthDp >= 110 && heightDp >= 110 -> WidgetSize.SQUARE
            heightDp >= 80 && heightDp >= widthDp -> WidgetSize.TALL
            widthDp >= 110 && heightDp >= 75 -> WidgetSize.COMPACT
            widthDp >= 110 && heightDp >= 48 -> WidgetSize.STRIP
            else -> WidgetSize.TILE
        }
    }
}

fun resolvedStyle(size: WidgetSize, selected: WidgetStyle, hasChecklist: Boolean): WidgetStyle = when {
    size == WidgetSize.TILE || size == WidgetSize.STRIP -> WidgetStyle.EMBER
    selected == WidgetStyle.CHECKLIST && !hasChecklist -> WidgetStyle.FOCUS
    else -> selected
}
