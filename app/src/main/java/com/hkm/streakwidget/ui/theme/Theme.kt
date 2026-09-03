package com.hkm.streakwidget.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/* Every Material role is explicit so no baseline Material purple/green token can leak into UI. */
private val LightScheme = lightColorScheme(
    primary = Color(0xFF151515), onPrimary = Color.White,
    primaryContainer = Color(0xFFE9E9E9), onPrimaryContainer = Color(0xFF151515), inversePrimary = Color(0xFFD5D5D5),
    secondary = Color(0xFF3D3D3D), onSecondary = Color.White,
    secondaryContainer = Color(0xFFEAEAEA), onSecondaryContainer = Color(0xFF151515),
    tertiary = Color(0xFF555555), onTertiary = Color.White,
    tertiaryContainer = Color(0xFFE8E8E8), onTertiaryContainer = Color(0xFF151515),
    background = Color(0xFFFCFCFC), onBackground = Color(0xFF151515),
    surface = Color(0xFFFCFCFC), onSurface = Color(0xFF151515),
    surfaceVariant = Color(0xFFF1F1F1), onSurfaceVariant = Color(0xFF626262), surfaceTint = Color.Transparent,
    inverseSurface = Color(0xFF202020), inverseOnSurface = Color(0xFFF7F7F7),
    error = Color(0xFF303030), onError = Color.White, errorContainer = Color(0xFFE8E8E8), onErrorContainer = Color(0xFF1A1A1A),
    outline = Color(0xFF747474), outlineVariant = Color(0xFFD6D6D6), scrim = Color.Black,
    surfaceBright = Color(0xFFFFFFFF), surfaceDim = Color(0xFFF2F2F2),
    surfaceContainer = Color(0xFFF7F7F7), surfaceContainerHigh = Color(0xFFF3F3F3), surfaceContainerHighest = Color(0xFFEDEDED),
    surfaceContainerLow = Color(0xFFFAFAFA), surfaceContainerLowest = Color.White,
    primaryFixed = Color(0xFFE7E7E7), primaryFixedDim = Color(0xFFD4D4D4), onPrimaryFixed = Color(0xFF161616), onPrimaryFixedVariant = Color(0xFF4A4A4A),
    secondaryFixed = Color(0xFFE7E7E7), secondaryFixedDim = Color(0xFFD4D4D4), onSecondaryFixed = Color(0xFF161616), onSecondaryFixedVariant = Color(0xFF4A4A4A),
    tertiaryFixed = Color(0xFFE7E7E7), tertiaryFixedDim = Color(0xFFD4D4D4), onTertiaryFixed = Color(0xFF161616), onTertiaryFixedVariant = Color(0xFF4A4A4A)
)

private val DarkScheme = darkColorScheme(
    primary = Color(0xFFF2F2F2), onPrimary = Color(0xFF151515),
    primaryContainer = Color(0xFF2A2A2A), onPrimaryContainer = Color(0xFFF2F2F2), inversePrimary = Color(0xFF3B3B3B),
    secondary = Color(0xFFD0D0D0), onSecondary = Color(0xFF202020),
    secondaryContainer = Color(0xFF303030), onSecondaryContainer = Color(0xFFF2F2F2),
    tertiary = Color(0xFFBFBFBF), onTertiary = Color(0xFF202020),
    tertiaryContainer = Color(0xFF353535), onTertiaryContainer = Color(0xFFF2F2F2),
    background = Color(0xFF101010), onBackground = Color(0xFFF2F2F2),
    surface = Color(0xFF101010), onSurface = Color(0xFFF2F2F2),
    surfaceVariant = Color(0xFF202020), onSurfaceVariant = Color(0xFFB8B8B8), surfaceTint = Color.Transparent,
    inverseSurface = Color(0xFFE7E7E7), inverseOnSurface = Color(0xFF202020),
    error = Color(0xFFE0E0E0), onError = Color(0xFF1A1A1A), errorContainer = Color(0xFF303030), onErrorContainer = Color(0xFFF2F2F2),
    outline = Color(0xFF929292), outlineVariant = Color(0xFF3D3D3D), scrim = Color.Black,
    surfaceBright = Color(0xFF373737), surfaceDim = Color(0xFF101010),
    surfaceContainer = Color(0xFF161616), surfaceContainerHigh = Color(0xFF202020), surfaceContainerHighest = Color(0xFF2A2A2A),
    surfaceContainerLow = Color(0xFF131313), surfaceContainerLowest = Color(0xFF0B0B0B),
    primaryFixed = Color(0xFFE7E7E7), primaryFixedDim = Color(0xFFD4D4D4), onPrimaryFixed = Color(0xFF161616), onPrimaryFixedVariant = Color(0xFF4A4A4A),
    secondaryFixed = Color(0xFFE7E7E7), secondaryFixedDim = Color(0xFFD4D4D4), onSecondaryFixed = Color(0xFF161616), onSecondaryFixedVariant = Color(0xFF4A4A4A),
    tertiaryFixed = Color(0xFFE7E7E7), tertiaryFixedDim = Color(0xFFD4D4D4), onTertiaryFixed = Color(0xFF161616), onTertiaryFixedVariant = Color(0xFF4A4A4A)
)

@Composable
fun StreakWidgetTheme(mode: String = "SYSTEM", content: @Composable () -> Unit) {
    val dark = when (mode) { "DARK" -> true; "LIGHT" -> false; else -> isSystemInDarkTheme() }
    val scheme = if (dark) DarkScheme else LightScheme
    val view = LocalView.current
    SideEffect {
        val window = (view.context as? Activity)?.window ?: return@SideEffect
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = if (dark) 0xFF101010.toInt() else 0xFFFCFCFC.toInt()
        window.navigationBarColor = if (dark) 0xFF101010.toInt() else 0xFFFCFCFC.toInt()
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = !dark
            isAppearanceLightNavigationBars = !dark
        }
    }
    MaterialTheme(colorScheme = scheme, typography = Typography, content = content)
}
