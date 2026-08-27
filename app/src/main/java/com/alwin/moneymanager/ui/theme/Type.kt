package com.alwin.moneymanager.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
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

// macOS-style typography - scaled for mobile (14sp base) with macOS proportions
// Clean, readable sizes optimized for all users including elderly
val iOSTypography = Typography(
    // Display sizes - for hero sections (reduced from desktop scale)
    displayLarge = default.displayLarge.copy(fontSize = 28.sp, fontWeight = FontWeight.Bold, lineHeight = 34.sp),
    displayMedium = default.displayMedium.copy(fontSize = 24.sp, fontWeight = FontWeight.Bold, lineHeight = 29.sp),
    displaySmall = default.displaySmall.copy(fontSize = 22.sp, fontWeight = FontWeight.Bold, lineHeight = 28.sp),

    // Headlines - for section headers (proportional to 14sp base)
    headlineLarge = default.headlineLarge.copy(fontSize = 20.sp, fontWeight = FontWeight.Bold, lineHeight = 25.sp),
    headlineMedium = default.headlineMedium.copy(fontSize = 18.sp, fontWeight = FontWeight.SemiBold, lineHeight = 23.sp),
    headlineSmall = default.headlineSmall.copy(fontSize = 17.sp, fontWeight = FontWeight.SemiBold, lineHeight = 22.sp),

    // Titles - for card/list headers (slightly larger than body for hierarchy)
    titleLarge = default.titleLarge.copy(fontSize = 16.sp, fontWeight = FontWeight.SemiBold, lineHeight = 20.sp),
    titleMedium = default.titleMedium.copy(fontSize = 15.sp, fontWeight = FontWeight.Medium, lineHeight = 20.sp),
    titleSmall = default.titleSmall.copy(fontSize = 14.sp, fontWeight = FontWeight.Medium, lineHeight = 18.sp),

    // Body text - main content (14sp base - standard Android size)
    bodyLarge = default.bodyLarge.copy(fontSize = 15.sp, fontWeight = FontWeight.Normal, lineHeight = 20.sp),
    bodyMedium = default.bodyMedium.copy(fontSize = 14.sp, fontWeight = FontWeight.Normal, lineHeight = 18.sp),
    bodySmall = default.bodySmall.copy(fontSize = 13.sp, fontWeight = FontWeight.Normal, lineHeight = 16.sp),

    // Labels - secondary text (compact but readable)
    labelLarge = default.labelLarge.copy(fontSize = 14.sp, fontWeight = FontWeight.Medium, lineHeight = 18.sp),
    labelMedium = default.labelMedium.copy(fontSize = 12.sp, fontWeight = FontWeight.Medium, lineHeight = 16.sp),
    labelSmall = default.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Medium, lineHeight = 14.sp),
)
