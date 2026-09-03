package com.hkm.streakwidget.widget

enum class WidgetSize(val minWidthDp: Int, val minHeightDp: Int) {
    TILE(56, 56),
    STRIP(112, 48),
    COMPACT(112, 88),
    SQUARE(112, 168),
    WIDE(240, 88),
    HERO(240, 168)
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
    val responsiveSizes: List<WidgetSize> = listOf(
        WidgetSize.TILE,
        WidgetSize.STRIP,
        WidgetSize.COMPACT,
        WidgetSize.SQUARE,
        WidgetSize.WIDE,
        WidgetSize.HERO
    )

    fun classify(widthDp: Int, heightDp: Int): WidgetSize {
        if (widthDp <= 0 || heightDp <= 0) return WidgetSize.COMPACT
        return when {
            widthDp >= WidgetSize.HERO.minWidthDp && heightDp >= WidgetSize.HERO.minHeightDp -> WidgetSize.HERO
            widthDp >= WidgetSize.WIDE.minWidthDp && heightDp >= WidgetSize.WIDE.minHeightDp -> WidgetSize.WIDE
            widthDp >= WidgetSize.SQUARE.minWidthDp && heightDp >= WidgetSize.SQUARE.minHeightDp -> WidgetSize.SQUARE
            widthDp >= WidgetSize.COMPACT.minWidthDp && heightDp >= WidgetSize.COMPACT.minHeightDp -> WidgetSize.COMPACT
            widthDp >= WidgetSize.STRIP.minWidthDp && heightDp >= WidgetSize.STRIP.minHeightDp -> WidgetSize.STRIP
            else -> WidgetSize.TILE
        }
    }
}

fun resolvedStyle(size: WidgetSize, selected: WidgetStyle, hasChecklist: Boolean): WidgetStyle = when {
    size == WidgetSize.TILE -> WidgetStyle.EMBER
    selected == WidgetStyle.CHECKLIST && !hasChecklist -> WidgetStyle.FOCUS
    else -> selected
}
