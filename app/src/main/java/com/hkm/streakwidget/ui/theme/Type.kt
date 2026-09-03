package com.hkm.streakwidget.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.hkm.streakwidget.R

val EbGaramondFontFamily = FontFamily(
    Font(R.font.eb_garamond_regular, FontWeight.Normal, FontStyle.Normal),
    Font(R.font.eb_garamond_italic, FontWeight.Normal, FontStyle.Italic),
    Font(R.font.eb_garamond_medium, FontWeight.Medium, FontStyle.Normal),
    Font(R.font.eb_garamond_medium_italic, FontWeight.Medium, FontStyle.Italic),
    Font(R.font.eb_garamond_semi_bold, FontWeight.SemiBold, FontStyle.Normal),
    Font(R.font.eb_garamond_semi_bold_italic, FontWeight.SemiBold, FontStyle.Italic),
    Font(R.font.eb_garamond_bold, FontWeight.Bold, FontStyle.Normal),
    Font(R.font.eb_garamond_bold_italic, FontWeight.Bold, FontStyle.Italic),
    Font(R.font.eb_garamond_extra_bold, FontWeight.ExtraBold, FontStyle.Normal),
    Font(R.font.eb_garamond_extra_bold_italic, FontWeight.ExtraBold, FontStyle.Italic)
)

val Typography = Typography(
    displayLarge = typographyStyle(FontWeight.Normal, 57.sp, 64.sp, -0.25.sp),
    displayMedium = typographyStyle(FontWeight.Normal, 45.sp, 52.sp),
    displaySmall = typographyStyle(FontWeight.Normal, 36.sp, 44.sp),
    headlineLarge = typographyStyle(FontWeight.Normal, 32.sp, 40.sp),
    headlineMedium = typographyStyle(FontWeight.Normal, 28.sp, 36.sp),
    headlineSmall = typographyStyle(FontWeight.Normal, 24.sp, 32.sp),
    titleLarge = typographyStyle(FontWeight.Normal, 22.sp, 28.sp),
    titleMedium = typographyStyle(FontWeight.Medium, 16.sp, 24.sp, 0.15.sp),
    titleSmall = typographyStyle(FontWeight.Medium, 14.sp, 20.sp, 0.1.sp),
    bodyLarge = typographyStyle(FontWeight.Normal, 16.sp, 24.sp, 0.5.sp),
    bodyMedium = typographyStyle(FontWeight.Normal, 14.sp, 20.sp, 0.25.sp),
    bodySmall = typographyStyle(FontWeight.Normal, 12.sp, 16.sp, 0.4.sp),
    labelLarge = typographyStyle(FontWeight.Medium, 14.sp, 20.sp, 0.1.sp),
    labelMedium = typographyStyle(FontWeight.Medium, 12.sp, 16.sp, 0.5.sp),
    labelSmall = typographyStyle(FontWeight.Medium, 11.sp, 16.sp, 0.5.sp)
)

private fun typographyStyle(
    fontWeight: FontWeight,
    fontSize: androidx.compose.ui.unit.TextUnit,
    lineHeight: androidx.compose.ui.unit.TextUnit,
    letterSpacing: androidx.compose.ui.unit.TextUnit = 0.sp
) = TextStyle(
    fontFamily = EbGaramondFontFamily,
    fontWeight = fontWeight,
    fontSize = fontSize,
    lineHeight = lineHeight,
    letterSpacing = letterSpacing
)
