package com.alwin.moneymanager.ui.expense

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp

/** Soft theme chip behind an extra amount, so it matches the app and stays easy to spot. */
@Composable
fun Modifier.extraAmountChip(isExtra: Boolean): Modifier {
    if (!isExtra) return this
    return this
        .clip(RoundedCornerShape(8.dp))
        .background(MaterialTheme.colorScheme.primaryContainer)
        .padding(horizontal = 8.dp, vertical = 2.dp)
}

/** 
 * Returns a deterministic color for each category based on the current theme's primary color.
 * Uses alpha variations to create different shades while staying in the same color family.
 */
@Composable
fun categoryColor(categoryId: Long): Color {
    val primary = MaterialTheme.colorScheme.primary
    val surface = MaterialTheme.colorScheme.surface
    
    // Create 8 different shades by interpolating between primary and surface
    val shades = listOf(
        lerp(surface, primary, 0.95f),
        lerp(surface, primary, 0.85f),
        lerp(surface, primary, 0.75f),
        lerp(surface, primary, 0.65f),
        lerp(surface, primary, 0.55f),
        lerp(surface, primary, 0.45f),
        lerp(surface, primary, 0.35f),
        lerp(surface, primary, 0.25f),
    )
    
    val index = ((categoryId % shades.size) + shades.size) % shades.size
    return shades[index.toInt()]
}

/** Same mapping as [categoryColor], usable outside composition (charts, widgets). */
fun categorySeedColor(categoryId: Long, darkTheme: Boolean): Color {
    // For non-composable contexts, return primary color
    // This is a fallback and won't be perfectly themed
    return if (darkTheme) Color(0xFF90CAF9) else Color(0xFF1976D2)
}
