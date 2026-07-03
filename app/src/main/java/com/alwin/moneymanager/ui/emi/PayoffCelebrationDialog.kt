package com.alwin.moneymanager.ui.emi

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/**
 * Celebratory dialog shown when an EMI's final installment is marked paid. A plain
 * icon+text `AlertDialog` felt flat for a "you just finished paying off a loan" moment, so this
 * layers a one-shot confetti burst (Canvas, no animation library needed) behind a spring-bounced
 * icon instead. Purely decorative — dismissing it has no side effects, unlike the reopen-warning
 * dialog next to it in `EmiDetailScreen`.
 */
@Composable
fun PayoffCelebrationDialog(emiName: String, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth().height(340.dp)) {
            ConfettiBurst(modifier = Modifier.fillMaxWidth().height(340.dp))
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Column(
                    modifier = Modifier.padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    BouncingCelebrationIcon()
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Loan paid off!",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "$emiName is fully paid. Great work staying on track.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(20.dp))
                    Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Nice!") }
                }
            }
        }
    }
}

@Composable
private fun BouncingCelebrationIcon() {
    val scale = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        scale.animateTo(
            targetValue = 1f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        )
    }
    Box(
        modifier = Modifier
            .size(64.dp)
            .graphicsLayer(scaleX = scale.value, scaleY = scale.value),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.Filled.Celebration,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(56.dp),
        )
    }
}

private class ConfettiParticle(
    val startXFraction: Float,
    val fallDistanceFraction: Float,
    val driftAmplitude: Float,
    val phase: Float,
    val size: Float,
    val colorIndex: Int,
    val rotationSpeed: Float,
    val startDelay: Float,
)

@Composable
private fun ConfettiBurst(modifier: Modifier = Modifier) {
    val colors = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.tertiary,
        MaterialTheme.colorScheme.error,
        MaterialTheme.colorScheme.onSurfaceVariant,
    )
    val particles = remember {
        List(30) {
            ConfettiParticle(
                startXFraction = Random.nextFloat(),
                fallDistanceFraction = 0.55f + Random.nextFloat() * 0.4f,
                driftAmplitude = Random.nextFloat() * 50f - 25f,
                phase = Random.nextFloat() * (2f * PI.toFloat()),
                size = 5f + Random.nextFloat() * 5f,
                colorIndex = Random.nextInt(4),
                rotationSpeed = Random.nextFloat() * 8f - 4f,
                startDelay = Random.nextFloat() * 0.15f,
            )
        }
    }
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(1f, animationSpec = tween(durationMillis = 1500, easing = LinearEasing))
    }
    Canvas(modifier = modifier) {
        val t = progress.value
        particles.forEach { p ->
            val local = ((t - p.startDelay) / (1f - p.startDelay)).coerceIn(0f, 1f)
            if (local <= 0f) return@forEach
            val fall = local * local
            val y = fall * p.fallDistanceFraction * size.height
            val x = p.startXFraction * size.width + sin(local * 6f + p.phase) * p.driftAmplitude
            val alpha = (1f - local).coerceIn(0f, 1f)
            if (alpha <= 0f) return@forEach
            rotate(degrees = local * 360f * p.rotationSpeed, pivot = Offset(x, y)) {
                drawRect(
                    color = colors[p.colorIndex].copy(alpha = alpha),
                    topLeft = Offset(x - p.size / 2, y - p.size),
                    size = Size(p.size, p.size * 1.8f),
                )
            }
        }
    }
}
