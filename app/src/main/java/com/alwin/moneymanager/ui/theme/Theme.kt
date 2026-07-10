package com.alwin.moneymanager.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp

/**
 * True when [AppThemeStyle.RETRO_LCD] is active. Read by components that need to change shape
 * (not just color/type) to sell the calculator-LCD look — e.g. hardcoded `RoundedCornerShape`
 * card corners, or [LcdAmountText] choosing the seven-segment face — since Material3's own
 * `MaterialTheme.shapes`/`.typography` slots don't reach every corner of a component (Button's
 * shape in particular is a hardcoded pill token, not theme-driven).
 */
val LocalIsRetroLcdTheme = compositionLocalOf { false }

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

// Digital-display negative palette: true AMOLED black "screen" with near-white "lit segment"
// text — not an attempt at LCD green, which never reads right on an emissive phone display the
// way it does on a reflective calculator screen. Black-background/white-foreground is also the
// one polarity that actually benefits from AMOLED (true black pixels are off, not just dark gray).
// Dark-only: this ignores the `darkTheme` param entirely, same as the light-only version it
// replaced — a digital-display readout doesn't get a second "light mode".
private object Lcd {
    val Background = Color(0xFF000000)
    val SurfaceVariant = Color(0xFF1A1A18)
    val SurfaceContainerLow = Color(0xFF121210)
    val SurfaceContainer = Color(0xFF1A1A18)
    val SurfaceContainerHigh = Color(0xFF242422)
    val SurfaceContainerHighest = Color(0xFF2E2E2B)
    val Text = Color(0xFFEDEDE6)
    // Mid-light gray accent — the equivalent of the old palette's dark "ink" accent, just
    // inverted: against a black screen the accent has to be *lighter* than background to read.
    val Accent = Color(0xFFB8B8AE)
    val AccentLight = Color(0xFF86867C)
    val AccentContainer = Color(0xFF3A3A36)
    val Error = Color(0xFFE0A0A0)
    // A dark tint of Error, not Error itself — call sites like DebtListScreen's "You'll give"
    // tile paint `errorContainer` as the background and `error` as the text color on top of it,
    // same pattern as every other *Container/on*Container pair; reusing Error for both makes
    // that text invisible against its own background.
    val ErrorContainer = Color(0xFF4A2626)
}

private val retroLcdColorScheme: ColorScheme = darkColorScheme(
    primary = Lcd.Text,
    onPrimary = Lcd.Background,
    primaryContainer = Lcd.Accent,
    onPrimaryContainer = Lcd.Background,
    secondary = Lcd.Accent,
    onSecondary = Lcd.Background,
    secondaryContainer = Lcd.AccentContainer,
    onSecondaryContainer = Lcd.Text,
    tertiary = Lcd.Accent,
    onTertiary = Lcd.Background,
    tertiaryContainer = Lcd.AccentContainer,
    onTertiaryContainer = Lcd.Text,
    background = Lcd.Background,
    onBackground = Lcd.Text,
    surface = Lcd.Background,
    onSurface = Lcd.Text,
    surfaceVariant = Lcd.SurfaceVariant,
    onSurfaceVariant = Lcd.Accent,
    outline = Lcd.Accent,
    outlineVariant = Lcd.AccentLight,
    surfaceContainerLowest = Lcd.Background,
    surfaceContainerLow = Lcd.SurfaceContainerLow,
    surfaceContainer = Lcd.SurfaceContainer,
    surfaceContainerHigh = Lcd.SurfaceContainerHigh,
    surfaceContainerHighest = Lcd.SurfaceContainerHighest,
    inverseSurface = Lcd.Text,
    inverseOnSurface = Lcd.Background,
    error = Lcd.Error,
    onError = Lcd.Background,
    errorContainer = Lcd.ErrorContainer,
    onErrorContainer = Lcd.Error,
)

// Same type scale as the app-wide [Typography] (sizes/spacing), just swapped to a monospace
// face — sells the "digital display" feel the Retro LCD theme is going for.
private val retroLcdTypography: Typography = run {
    val base = Typography
    Typography(
        displayLarge = base.displayLarge.copy(fontFamily = FontFamily.Monospace),
        displayMedium = base.displayMedium.copy(fontFamily = FontFamily.Monospace),
        displaySmall = base.displaySmall.copy(fontFamily = FontFamily.Monospace),
        headlineLarge = base.headlineLarge.copy(fontFamily = FontFamily.Monospace),
        headlineMedium = base.headlineMedium.copy(fontFamily = FontFamily.Monospace),
        headlineSmall = base.headlineSmall.copy(fontFamily = FontFamily.Monospace),
        titleLarge = base.titleLarge.copy(fontFamily = FontFamily.Monospace),
        titleMedium = base.titleMedium.copy(fontFamily = FontFamily.Monospace),
        titleSmall = base.titleSmall.copy(fontFamily = FontFamily.Monospace),
        bodyLarge = base.bodyLarge.copy(fontFamily = FontFamily.Monospace),
        bodyMedium = base.bodyMedium.copy(fontFamily = FontFamily.Monospace),
        bodySmall = base.bodySmall.copy(fontFamily = FontFamily.Monospace),
        labelLarge = base.labelLarge.copy(fontFamily = FontFamily.Monospace),
        labelMedium = base.labelMedium.copy(fontFamily = FontFamily.Monospace),
        labelSmall = base.labelSmall.copy(fontFamily = FontFamily.Monospace),
    )
}

// Chunky, mostly-square "injection-molded plastic key" corners — a small fixed radius rather
// than Material 3's default pill/large-radius scale — since sharp rectangles read as a modern
// flat-design choice (terminal/DOS), not a physical calculator, which always has *some* corner
// rounding. Note this only reaches components that read `MaterialTheme.shapes.*` (Card, Dialog,
// TextField, Chip, NavigationBar indicator, FAB); Button's shape is a hardcoded M3 token and
// components with a literal `RoundedCornerShape(..)` need their own [LocalIsRetroLcdTheme] check.
private val retroLcdShapes = Shapes(
    extraSmall = RoundedCornerShape(3.dp),
    small = RoundedCornerShape(4.dp),
    medium = RoundedCornerShape(6.dp),
    large = RoundedCornerShape(6.dp),
    extraLarge = RoundedCornerShape(8.dp),
)

@Composable
fun MoneyManagerTheme(
    seedColor: Color = AppThemeColor.PURPLE.seed,
    themeStyle: AppThemeStyle = AppThemeStyle.DEFAULT,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val isRetro = themeStyle == AppThemeStyle.RETRO_LCD

    // Dynamic (wallpaper-based) color is deliberately not used — the user picks a color
    // explicitly in Settings instead, and that should always win regardless of OS version.
    val colorScheme = if (isRetro) retroLcdColorScheme else tonalColorScheme(seedColor, darkTheme)
    val typography = if (isRetro) retroLcdTypography else Typography
    val shapes = if (isRetro) retroLcdShapes else Shapes()

    CompositionLocalProvider(LocalIsRetroLcdTheme provides isRetro) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            shapes = shapes,
            content = content,
        )
    }
}
