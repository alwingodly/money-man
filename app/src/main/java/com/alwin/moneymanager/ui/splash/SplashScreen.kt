package com.alwin.moneymanager.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    val draw = remember { Animatable(0f) }
    val exit = remember { Animatable(1f) }
    val primary = MaterialTheme.colorScheme.primary
    val slices = remember(primary) { splashColors(primary) }

    LaunchedEffect(Unit) {
        draw.animateTo(1f, tween(durationMillis = 1200, easing = FastOutSlowInEasing))
        delay(280)
        exit.animateTo(0f, tween(durationMillis = 320))
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = exit.value }
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = slices[1].copy(alpha = 0.22f),
                radius = size.minDimension * 0.42f,
                center = Offset(size.width * 0.18f, size.height * 0.22f),
            )
            drawCircle(
                color = slices[3].copy(alpha = 0.18f),
                radius = size.minDimension * 0.34f,
                center = Offset(size.width * 0.86f, size.height * 0.78f),
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Canvas(modifier = Modifier.size(148.dp)) {
                val stroke = 16.dp.toPx()
                val diameter = size.minDimension - stroke
                val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
                val ring = Stroke(width = stroke)
                slices.forEachIndexed { index, color ->
                    val startAt = index / slices.size.toFloat()
                    val local = ((draw.value - startAt) / 0.22f).coerceIn(0f, 1f)
                    if (local <= 0f) return@forEachIndexed
                    drawArc(
                        color = color,
                        startAngle = -90f + index * 60f,
                        sweepAngle = 50f * local,
                        useCenter = false,
                        topLeft = topLeft,
                        size = Size(diameter, diameter),
                        style = ring,
                    )
                }
            }
            val titleProgress = ((draw.value - 0.35f) / 0.45f).coerceIn(0f, 1f)
            Text(
                "FinBook",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .padding(top = 22.dp)
                    .graphicsLayer {
                        alpha = titleProgress
                        scaleX = 0.92f + 0.08f * titleProgress
                        scaleY = 0.92f + 0.08f * titleProgress
                    },
            )
            val subtitleProgress = ((draw.value - 0.62f) / 0.38f).coerceIn(0f, 1f)
            Text(
                "Your money, in one place",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .padding(top = 6.dp)
                    .graphicsLayer { alpha = subtitleProgress },
            )
        }
    }
}

private fun splashColors(primary: Color): List<Color> {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(primary.toArgb(), hsv)
    return List(6) { index ->
        Color.hsl(
            hue = (hsv[0] + index * 51f) % 360f,
            saturation = if (index % 2 == 0) 0.7f else 0.52f,
            lightness = if (index % 2 == 0) 0.62f else 0.76f,
        )
    }
}
