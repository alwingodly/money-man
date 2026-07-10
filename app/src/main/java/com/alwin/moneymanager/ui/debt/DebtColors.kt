package com.alwin.moneymanager.ui.debt

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * "Money coming to you" green, used for the get/collect direction (the theme's error red is used for
 * the give direction). Material 3 has no standard positive colour, so these are fixed greens tuned
 * for readability in both light and dark themes.
 */
@Composable
fun getColor(): Color = if (isSystemInDarkTheme()) Color(0xFF7FD18B) else Color(0xFF2E7D32)

@Composable
fun getContainerColor(): Color = if (isSystemInDarkTheme()) Color(0xFF1B3A22) else Color(0xFFD7EBD9)
