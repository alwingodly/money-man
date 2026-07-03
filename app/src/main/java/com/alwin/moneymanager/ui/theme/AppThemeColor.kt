package com.alwin.moneymanager.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Curated primary-color presets a user can pick from in Settings — not a free hex-input field.
 * Values are the Tailwind-500 shades (a widely-used, pre-vetted "vibrant SaaS app" palette) —
 * swapped in from the original, more muted dataviz-derived hues because they read as flat/dull
 * once turned into a full app theme (as opposed to small chart dots, where the original set came
 * from). Enum constant *names* are kept stable even though labels/hexes changed, since a user's
 * choice is persisted as `AppThemeColor.name` in DataStore (`ThemeRepository`) — renaming the
 * constants would silently reset anyone who'd already picked a color back to the default.
 */
enum class AppThemeColor(val label: String, val seed: Color) {
    PURPLE("Indigo", Color(0xFF6366F1)),
    BLUE("Sky", Color(0xFF0EA5E9)),
    TEAL("Teal", Color(0xFF14B8A6)),
    GREEN("Green", Color(0xFF22C55E)),
    VIOLET("Violet", Color(0xFF8B5CF6)),
    RED("Red", Color(0xFFEF4444)),
    MAGENTA("Magenta", Color(0xFFD946EF)),
    ORANGE("Orange", Color(0xFFF97316)),
}

fun appThemeColorFromName(name: String?): AppThemeColor =
    AppThemeColor.entries.firstOrNull { it.name == name } ?: AppThemeColor.PURPLE
