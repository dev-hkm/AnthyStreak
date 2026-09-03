package com.hkm.streakwidget.ui.screens

import androidx.compose.ui.graphics.Color

data class GoalColorOption(
    val name: String,
    val color: Color,
    val hex: String
)

object GoalPalette {
    val options = listOf(
        GoalColorOption("Ember", Color(0xFFE97B4A), "#E97B4A"),
        GoalColorOption("Amber", Color(0xFFF3A53C), "#F3A53C"),
        GoalColorOption("Red", Color(0xFFE25151), "#E25151"),
        GoalColorOption("Rose", Color(0xFFD85683), "#D85683"),
        GoalColorOption("Violet", Color(0xFFA66BE0), "#A66BE0"),
        GoalColorOption("Periwinkle", Color(0xFF617FD2), "#617FD2"),
        GoalColorOption("Sky", Color(0xFF4296C9), "#4296C9"),
        GoalColorOption("Teal", Color(0xFF23977E), "#23977E"),
        GoalColorOption("Leaf", Color(0xFF5FA954), "#5FA954"),
        GoalColorOption("Olive", Color(0xFFAEA63B), "#AEA63B"),
        GoalColorOption("Umber", Color(0xFFA66B45), "#A66B45"),
        GoalColorOption("Silver", Color(0xFFB6B6B6), "#B6B6B6"),
        GoalColorOption("Sun Gold", Color(0xFFFFD45A), "#FFD45A"),
        GoalColorOption("Lime", Color(0xFF83D34B), "#83D34B"),
        GoalColorOption("Emerald", Color(0xFF20C46A), "#20C46A"),
        GoalColorOption("Aqua", Color(0xFF32CDB1), "#32CDB1"),
        GoalColorOption("Ice Cyan", Color(0xFF5DD7E5), "#5DD7E5"),
        GoalColorOption("Cobalt", Color(0xFF3047C9), "#3047C9"),
        GoalColorOption("Midnight", Color(0xFF29337A), "#29337A"),
        GoalColorOption("Fuchsia", Color(0xFFD84CC6), "#D84CC6"),
        GoalColorOption("Plum", Color(0xFF773A8F), "#773A8F"),
        GoalColorOption("Wine", Color(0xFF842F4F), "#842F4F")
    )
}
