package com.alwin.moneymanager.ui.emi

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun EmiProgressRing(
    paidMonths: Int,
    totalMonths: Int,
    progress: Float,
    modifier: Modifier = Modifier,
    size: Dp = 64.dp,
) {
    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxSize(),
            strokeWidth = 6.dp,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )
        Text(
            "$paidMonths/$totalMonths",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            fontSize = if (size < 72.dp) 11.sp else 13.sp,
            textAlign = TextAlign.Center,
        )
    }
}
