package com.alwin.moneymanager.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import com.alwin.moneymanager.R

/** Geist Pixel (variable font, bundled under the SIL Open Font License) used app-wide. */
val GeistPixel = FontFamily(Font(R.font.geist_pixel))

// Start from the Material 3 defaults (sizes, line heights, spacing) and only swap the typeface, so
// every text style across the app renders in Geist Pixel without hand-tuning each one.
private val default = Typography()

val Typography = Typography(
    displayLarge = default.displayLarge.copy(fontFamily = GeistPixel),
    displayMedium = default.displayMedium.copy(fontFamily = GeistPixel),
    displaySmall = default.displaySmall.copy(fontFamily = GeistPixel),
    headlineLarge = default.headlineLarge.copy(fontFamily = GeistPixel),
    headlineMedium = default.headlineMedium.copy(fontFamily = GeistPixel),
    headlineSmall = default.headlineSmall.copy(fontFamily = GeistPixel),
    titleLarge = default.titleLarge.copy(fontFamily = GeistPixel),
    titleMedium = default.titleMedium.copy(fontFamily = GeistPixel),
    titleSmall = default.titleSmall.copy(fontFamily = GeistPixel),
    bodyLarge = default.bodyLarge.copy(fontFamily = GeistPixel),
    bodyMedium = default.bodyMedium.copy(fontFamily = GeistPixel),
    bodySmall = default.bodySmall.copy(fontFamily = GeistPixel),
    labelLarge = default.labelLarge.copy(fontFamily = GeistPixel),
    labelMedium = default.labelMedium.copy(fontFamily = GeistPixel),
    labelSmall = default.labelSmall.copy(fontFamily = GeistPixel),
)
