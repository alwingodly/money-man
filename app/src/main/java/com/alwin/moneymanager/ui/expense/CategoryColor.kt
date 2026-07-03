package com.alwin.moneymanager.ui.expense

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Validated 8-hue categorical palette (fixed order, CVD-safe adjacent pairs).
private val categoryPaletteLight = listOf(
    Color(0xFF2A78D6), // blue
    Color(0xFF1BAF7A), // aqua
    Color(0xFFEDA100), // yellow
    Color(0xFF008300), // green
    Color(0xFF4A3AA7), // violet
    Color(0xFFE34948), // red
    Color(0xFFE87BA4), // magenta
    Color(0xFFEB6834), // orange
)

private val categoryPaletteDark = listOf(
    Color(0xFF3987E5),
    Color(0xFF199E70),
    Color(0xFFC98500),
    Color(0xFF008300),
    Color(0xFF9085E9),
    Color(0xFFE66767),
    Color(0xFFD55181),
    Color(0xFFD95926),
)

/** Deterministic color per category id, stable across recompositions and app runs. */
@Composable
fun categoryColor(categoryId: Long): Color {
    val palette = if (isSystemInDarkTheme()) categoryPaletteDark else categoryPaletteLight
    val index = ((categoryId % palette.size) + palette.size) % palette.size
    return palette[index.toInt()]
}
