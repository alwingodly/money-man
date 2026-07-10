package com.alwin.moneymanager.ui.settings

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private const val APP_VERSION = "1.0"
private const val TAPS_TO_UNLOCK = 7

/**
 * Muted app-version line at the bottom of Settings — and a little easter egg: tap it 7 times to
 * pop the "secret money stash" with a spinning coin. Purely cosmetic and self-contained.
 */
@Composable
fun AppVersionFooter(modifier: Modifier = Modifier) {
    var taps by remember { mutableIntStateOf(0) }
    var showStash by remember { mutableStateOf(false) }

    Text(
        text = "Money Manager · v$APP_VERSION",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) {
                taps++
                if (taps >= TAPS_TO_UNLOCK) {
                    taps = 0
                    showStash = true
                }
            }
            .padding(vertical = 16.dp),
    )

    if (showStash) {
        MoneyStashDialog(onDismiss = { showStash = false })
    }
}

@Composable
private fun MoneyStashDialog(onDismiss: () -> Unit) {
    val transition = rememberInfiniteTransition(label = "coin")
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "coinSpin",
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("You found the secret stash!") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                // rotationY spins the coin like it's flipping on a table.
                Text("🪙", fontSize = 64.sp, modifier = Modifier.graphicsLayer { rotationY = rotation })
                Text(
                    "A rupee saved is a rupee earned.\nThanks for using Money Manager 💛",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 16.dp),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Nice!") }
        },
    )
}
