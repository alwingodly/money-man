package com.alwin.moneymanager.ui.theme

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp

/**
 * Renders a standalone numeric readout (a currency amount, on its own, not embedded in a
 * sentence) in a real seven-segment LCD digit face when the Retro LCD theme is active. Falls back
 * to a plain [Text] under any other theme, so call sites don't need an `if (retro)` branch of
 * their own.
 *
 * An earlier version also drew a dim "all segments lit" ghost behind the digits (the unlit-segment
 * bleed real LCDs show) — dropped because it read as doubled/blurry text and made amounts harder
 * to read at a glance, which matters more here than the extra authenticity.
 *
 * Deliberately not used for prose that merely *contains* an amount ("Outstanding: ₹500") — DSEG7
 * only has segment-alphabet approximations for letters, which read poorly at sentence length.
 * Non-digit characters DSEG7 doesn't cover (₹, commas) fall back to the system font per-glyph,
 * which is fine since digits carry the LCD look.
 */
@Composable
fun LcdAmountText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
    fontWeight: FontWeight? = null,
    textAlign: TextAlign? = null,
) {
    val isRetro = LocalIsRetroLcdTheme.current
    Text(
        text = text,
        modifier = modifier,
        style = if (isRetro) {
            style.copy(fontFamily = Dseg7Classic, fontWeight = fontWeight ?: style.fontWeight, letterSpacing = 0.5.sp)
        } else {
            style
        },
        color = color,
        fontWeight = if (isRetro) null else fontWeight,
        textAlign = textAlign,
    )
}
