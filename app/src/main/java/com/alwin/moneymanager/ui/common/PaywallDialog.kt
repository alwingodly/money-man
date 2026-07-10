package com.alwin.moneymanager.ui.common

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

/**
 * Friendly, in-the-moment paywall — shown right when a free-tier limit is hit (a 6th category, a
 * 3rd EMI, a locked color, Retro LCD), not as a separate onboarding step nobody reads.
 */
@Composable
fun PaywallDialog(
    message: String,
    onDismiss: () -> Unit,
    onUnlock: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Filled.LocalCafe,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        },
        title = { Text("Buy me a coffee ☕") },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onUnlock) { Text("Unlock — ₹9") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Maybe later") }
        },
    )
}
