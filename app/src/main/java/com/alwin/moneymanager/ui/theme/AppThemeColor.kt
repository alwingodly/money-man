package com.alwin.moneymanager.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Accent presets for Settings. Kept small and muted so the app reads as a finance tool,
 * not a rainbow of SaaS marketing colors. Enum names are stored in DataStore — [appThemeColorFromName]
 * maps older playful names onto this set so existing installs don't lose their choice.
 */
enum class AppThemeColor(val label: String, val seed: Color) {
    NAVY("Navy", Color(0xFF1E4E79)),
    FOREST("Forest", Color(0xFF2D6A4F)),
    TEAL("Teal", Color(0xFF0F766E)),
    INDIGO("Indigo", Color(0xFF3D4C7C)),
    SLATE("Slate", Color(0xFF475569)),
    BURGUNDY("Burgundy", Color(0xFF6B3A4A)),
    GRAPHITE("Graphite", Color(0xFF3D4A54)),
    OLIVE("Olive", Color(0xFF5C6B3A)),
}

fun appThemeColorFromName(name: String?): AppThemeColor {
    if (name == null) return AppThemeColor.NAVY
    AppThemeColor.entries.firstOrNull { it.name == name }?.let { return it }
    return when (name) {
        "PURPLE", "VIOLET", "COBALT", "BLUE" -> AppThemeColor.NAVY
        "GREEN", "EMERALD", "LIME" -> AppThemeColor.FOREST
        "CYAN" -> AppThemeColor.TEAL
        "ROSE", "RED", "PINK", "MAGENTA" -> AppThemeColor.BURGUNDY
        "ORANGE", "AMBER" -> AppThemeColor.OLIVE
        else -> AppThemeColor.NAVY
    }
}

/**
 * Light or dark, chosen in Settings. The app does not follow the phone's system theme.
 */
enum class AppThemeMode(val label: String) {
    LIGHT("Light"),
    DARK("Dark"),
}

fun appThemeModeFromName(name: String?): AppThemeMode =
    AppThemeMode.entries.firstOrNull { it.name == name } ?: AppThemeMode.LIGHT

/**
 * Overall theme look, orthogonal to [AppThemeColor]. [RETRO_LCD] replaces the whole Material
 * color scheme and typography (calculator/old-phone LCD screen) rather than just swapping the
 * accent color. Retro LCD is always dark; light/dark mode applies to [DEFAULT] only.
 */
enum class AppThemeStyle(val label: String) {
    DEFAULT("Default"),
    RETRO_LCD("Retro LCD"),
}

fun appThemeStyleFromName(name: String?): AppThemeStyle =
    AppThemeStyle.entries.firstOrNull { it.name == name } ?: AppThemeStyle.DEFAULT
