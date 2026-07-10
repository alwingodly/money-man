package com.alwin.moneymanager.ui.common

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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/**
 * Reusable "you did it!" moment — a one-shot confetti burst behind a spring-bounced emoji and a
 * short message. Purely decorative; dismissing has no side effects. Used for hitting a savings goal
 * (the EMI payoff has its own bespoke version).
 */
@Composable
fun CelebrationDialog(
    emoji: String,
    title: String,
    message: String,
    onDismiss: () -> Unit,
) {
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
                    BouncingEmoji(emoji)
                    Spacer(Modifier.height(16.dp))
                    Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(20.dp))
                    Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("🎉 Yay!") }
                }
            }
        }
    }
}

@Composable
private fun BouncingEmoji(emoji: String) {
    val scale = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        scale.animateTo(
            targetValue = 1f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        )
    }
    Text(
        emoji,
        fontSize = 56.sp,
        modifier = Modifier.graphicsLayer(scaleX = scale.value, scaleY = scale.value),
    )
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
        MaterialTheme.colorScheme.secondary,
    )
    val particles = remember {
        List(34) {
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
        progress.animateTo(1f, animationSpec = tween(durationMillis = 1600, easing = LinearEasing))
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
