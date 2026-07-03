package com.alwin.moneymanager.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

// Neutral zinc-gray scale — a white/near-black surface base with no hue tint of its own, so the
// picked theme color reads as a single clear accent (shadcn/ui style) instead of the whole app
// being awash in tinted containers. See [tonalColorScheme].
private object Zinc {
    val White = Color(0xFFFFFFFF)
    val Zinc50 = Color(0xFFFAFAFA)
    val Zinc100 = Color(0xFFF4F4F5)
    val Zinc150 = Color(0xFFECECEE)
    val Zinc200 = Color(0xFFE4E4E7)
    val Zinc300 = Color(0xFFD4D4D8)
    val Zinc400 = Color(0xFFA1A1AA)
    val Zinc500 = Color(0xFF71717A)
    val Zinc700 = Color(0xFF3F3F46)
    val Zinc800 = Color(0xFF27272A)
    val Zinc850 = Color(0xFF1F1F23)
    val Zinc900 = Color(0xFF18181B)
    val Zinc950 = Color(0xFF09090B)
    val Black = Color(0xFF000000)
}

/**
 * Builds a full light/dark [ColorScheme] from a single [seed] color by varying saturation/value
 * at a fixed hue — not a Material-You HCT tonal palette (that needs the material-color-utilities
 * library), but good enough contrast for a small curated set of presets (see [AppThemeColor]).
 *
 * Surfaces/backgrounds are a true neutral zinc gray scale, not derived from the seed hue — only
 * `primary`/`onPrimary`/`primaryContainer`/`onPrimaryContainer` carry the seed color, so the app
 * reads as "white background, one accent color" (shadcn/ui style) rather than every container
 * tinted a different hue. `secondary`/`tertiary` roles are neutral for the same reason — anywhere
 * that wants the accent explicitly (bottom nav selection, selected filter chips) sets `primary`
 * directly instead of relying on `secondaryContainer`/`tertiaryContainer`.
 */
private fun tonalColorScheme(seed: Color, dark: Boolean): ColorScheme {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(seed.toArgb(), hsv)
    val hue = hsv[0]

    fun tone(saturation: Float, value: Float): Color = Color(
        android.graphics.Color.HSVToColor(
            floatArrayOf(hue, saturation.coerceIn(0f, 1f), value.coerceIn(0f, 1f))
        )
    )

    return if (dark) {
        darkColorScheme(
            primary = tone(0.45f, 0.85f),
            onPrimary = tone(0.55f, 0.15f),
            primaryContainer = tone(0.45f, 0.28f),
            onPrimaryContainer = tone(0.35f, 0.92f),
            secondary = Zinc.Zinc400,
            onSecondary = Zinc.Zinc900,
            secondaryContainer = Zinc.Zinc800,
            onSecondaryContainer = Zinc.Zinc50,
            tertiary = Zinc.Zinc400,
            onTertiary = Zinc.Zinc900,
            tertiaryContainer = Zinc.Zinc850,
            onTertiaryContainer = Zinc.Zinc50,
            background = Zinc.Zinc950,
            onBackground = Zinc.Zinc50,
            surface = Zinc.Zinc950,
            onSurface = Zinc.Zinc50,
            surfaceVariant = Zinc.Zinc800,
            onSurfaceVariant = Zinc.Zinc400,
            outline = Zinc.Zinc700,
            outlineVariant = Zinc.Zinc800,
            surfaceContainerLowest = Zinc.Black,
            surfaceContainerLow = Zinc.Zinc900,
            surfaceContainer = Zinc.Zinc850,
            surfaceContainerHigh = Zinc.Zinc800,
            surfaceContainerHighest = Zinc.Zinc700,
            inverseSurface = Zinc.Zinc200,
            inverseOnSurface = Zinc.Zinc900,
        )
    } else {
        lightColorScheme(
            primary = tone(0.65f, 0.6f),
            onPrimary = Color.White,
            primaryContainer = tone(0.25f, 0.97f),
            onPrimaryContainer = tone(0.75f, 0.25f),
            secondary = Zinc.Zinc500,
            onSecondary = Color.White,
            secondaryContainer = Zinc.Zinc100,
            onSecondaryContainer = Zinc.Zinc900,
            tertiary = Zinc.Zinc500,
            onTertiary = Color.White,
            tertiaryContainer = Zinc.Zinc150,
            onTertiaryContainer = Zinc.Zinc900,
            background = Zinc.White,
            onBackground = Zinc.Zinc900,
            surface = Zinc.White,
            onSurface = Zinc.Zinc900,
            surfaceVariant = Zinc.Zinc100,
            onSurfaceVariant = Zinc.Zinc500,
            outline = Zinc.Zinc300,
            outlineVariant = Zinc.Zinc200,
            surfaceContainerLowest = Zinc.White,
            surfaceContainerLow = Zinc.Zinc50,
            surfaceContainer = Zinc.Zinc100,
            surfaceContainerHigh = Zinc.Zinc150,
            surfaceContainerHighest = Zinc.Zinc200,
            inverseSurface = Zinc.Zinc800,
            inverseOnSurface = Zinc.Zinc50,
        )
    }
}

@Composable
fun MoneyManagerTheme(
    seedColor: Color = AppThemeColor.PURPLE.seed,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    // Dynamic (wallpaper-based) color is deliberately not used — the user picks a color
    // explicitly in Settings instead, and that should always win regardless of OS version.
    val colorScheme = tonalColorScheme(seedColor, darkTheme)

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content,
    )
}
