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
    // Ordered as a rainbow for the picker (which renders in declaration order). Enum *names* stay
    // stable — a user's choice is persisted as `.name` — so only add/reorder, never rename.
    ROSE("Rose", Color(0xFFF43F5E)),
    RED("Red", Color(0xFFEF4444)),
    ORANGE("Orange", Color(0xFFF97316)),
    AMBER("Amber", Color(0xFFF59E0B)),
    LIME("Lime", Color(0xFF84CC16)),
    GREEN("Green", Color(0xFF22C55E)),
    EMERALD("Emerald", Color(0xFF10B981)),
    TEAL("Teal", Color(0xFF14B8A6)),
    CYAN("Cyan", Color(0xFF06B6D4)),
    BLUE("Sky", Color(0xFF0EA5E9)),
    COBALT("Blue", Color(0xFF3B82F6)),
    PURPLE("Indigo", Color(0xFF6366F1)),
    VIOLET("Violet", Color(0xFF8B5CF6)),
    MAGENTA("Magenta", Color(0xFFD946EF)),
    PINK("Pink", Color(0xFFEC4899)),
    SLATE("Slate", Color(0xFF64748B)),
}

fun appThemeColorFromName(name: String?): AppThemeColor =
    AppThemeColor.entries.firstOrNull { it.name == name } ?: AppThemeColor.PURPLE
