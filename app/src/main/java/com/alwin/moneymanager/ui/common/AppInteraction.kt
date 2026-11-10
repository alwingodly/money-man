package com.alwin.moneymanager.ui.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties

/**
 * Shared interaction tokens so every list, tap, and form behaves the same:
 * large enough targets, visible feedback, and no accidental data loss.
 */
object AppInteraction {
    val MinTouchTarget = 48.dp
    val ListRowMinHeight = 64.dp
    val CardCorner = 10.dp
    val CardBorder = 0.5.dp
    val ScreenPadding = 16.dp
    val ListGap = 10.dp
}

/** Forms stay open if the user taps outside — Cancel or Back is required. */
val AppFormDialogProperties = DialogProperties(
    usePlatformDefaultWidth = false,
    dismissOnClickOutside = false,
    dismissOnBackPress = true,
)

fun HapticFeedback.confirmAction() {
    performHapticFeedback(HapticFeedbackType.LongPress)
}

@Composable
fun AppAddFab(
    contentDescription: String,
    onClick: () -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    FloatingActionButton(
        onClick = {
            haptics.confirmAction()
            onClick()
        },
        containerColor = MaterialTheme.colorScheme.primaryContainer,
    ) {
        Icon(Icons.Filled.Add, contentDescription = contentDescription)
    }
}

@Composable
fun AppListCard(
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = AppInteraction.ListRowMinHeight)
            .then(
                if (onClick != null) {
                    Modifier.clickable(role = Role.Button, onClick = onClick)
                } else {
                    Modifier
                }
            ),
        shape = RoundedCornerShape(AppInteraction.CardCorner),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
        ),
        border = BorderStroke(
            AppInteraction.CardBorder,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            contentAlignment = Alignment.CenterStart,
            content = content,
        )
    }
}
