package com.alwin.moneymanager.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.alwin.moneymanager.R

/** Geist Pixel (variable font, bundled under the SIL Open Font License) used app-wide. */
val GeistPixel = FontFamily(Font(R.font.geist_pixel))

/**
 * DSEG7 Classic (bundled under the SIL Open Font License, https://github.com/keshikan/DSEG) — a
 * real seven-segment LCD digit face, the same style used on calculators/digital clocks. Only
 * digits, `A-Za-z`, space, and a handful of punctuation (`. - : _ !`) have real segment glyphs;
 * anything else (currency symbols, commas) falls back to the system font automatically. Used only
 * for standalone numeric readouts via [LcdAmountText] — not the general Retro LCD typeface, since
 * full sentences set in segment-alphabet glyphs read poorly.
 */
val Dseg7Classic = FontFamily(Font(R.font.dseg7_classic_bold, FontWeight.Bold))

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
