package com.alwin.moneymanager.ui.profile

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.alwin.moneymanager.ui.theme.LcdAmountText
import com.alwin.moneymanager.ui.theme.LocalIsDarkTheme
import com.alwin.moneymanager.util.decodeSampledBitmapFromFile
import com.alwin.moneymanager.util.formatCurrency
import com.alwin.moneymanager.util.timeOfDayGreeting
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private val playfulLines = listOf(
    "Your coins waved hello.",
    "A wallet with a personality.",
    "Spending can wait. You look great.",
    "This name belongs on a banknote.",
    "Small savings, big smile.",
    "The app kept a seat for you.",
)

@Composable
fun ProfileScreen(
    onBack: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val savedName by viewModel.name.collectAsState()
    val photoPath by viewModel.photoPath.collectAsState()
    val pulse by viewModel.pulse.collectAsState()
    var editingName by remember { mutableStateOf(false) }
    var look by remember { mutableIntStateOf(0) }
    var line by remember { mutableIntStateOf(0) }
    val gearAngle = remember { Animatable(0f) }
    val gearScope = rememberCoroutineScope()
    var gearJob by remember { mutableStateOf<Job?>(null) }
    val displayName = savedName?.trim().orEmpty()

    val pickPhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri -> uri?.let(viewModel::setPhoto) }

    val primary = MaterialTheme.colorScheme.primary
    val container = MaterialTheme.colorScheme.primaryContainer
    val funInk = if (LocalIsDarkTheme.current) Color.Black else Color.White

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(32.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(
                            primary.copy(alpha = 0.34f),
                            container.copy(alpha = 0.72f),
                            MaterialTheme.colorScheme.surface,
                        ),
                    ),
                )
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .rotate(gearAngle.value)
                        .pointerInput(Unit) {
                            awaitEachGesture {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                down.consume()
                                val spinning = AtomicBoolean(false)
                                val spinJob = gearScope.launch {
                                    delay(viewConfiguration.longPressTimeoutMillis)
                                    spinning.set(true)
                                    var degreesPerSecond = 24f
                                    var lastFrame = 0L
                                    while (isActive) {
                                        val frame = withFrameNanos { it }
                                        if (lastFrame != 0L) {
                                            val seconds = ((frame - lastFrame).coerceAtMost(32_000_000L)) / 1_000_000_000f
                                            degreesPerSecond *= kotlin.math.exp(0.62f * seconds)
                                            gearAngle.snapTo(gearAngle.value + degreesPerSecond * seconds)
                                        }
                                        lastFrame = frame
                                    }
                                }
                                var released = false
                                try {
                                    while (true) {
                                        val event = awaitPointerEvent()
                                        val change = event.changes.firstOrNull { it.id == down.id }
                                        event.changes.forEach { it.consume() }
                                        if (change == null || !change.pressed) {
                                            released = change != null
                                            break
                                        }
                                    }
                                } finally {
                                    val didSpin = spinning.get()
                                    spinJob.cancel()
                                    if (didSpin) {
                                        gearJob?.cancel()
                                        gearJob = gearScope.launch {
                                            val snapped = (gearAngle.value / 60f).roundToInt() * 60f
                                            gearAngle.animateTo(snapped, tween(durationMillis = 160))
                                        }
                                    } else if (released) {
                                        gearJob?.cancel()
                                        gearJob = gearScope.launch {
                                            val next = (gearAngle.value / 60f).roundToInt() * 60f + 60f
                                            gearAngle.animateTo(next, tween(durationMillis = 280))
                                        }
                                    }
                                }
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    PlayfulFrame(look = look, ink = funInk) {
                        ProfileAvatar(
                            photoPath = photoPath,
                            name = displayName,
                            size = 124.dp,
                        )
                    }
                }
                Text(
                    timeOfDayGreeting(),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 8.dp),
                )
                Text(
                    displayName.ifBlank { "Who’s this?" },
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp),
                )
                Text(
                    playfulLines[line],
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .clickable {
                            line = (line + 1) % playfulLines.size
                            look = (look + 1) % 4
                        },
                )
                Text(
                    "Tap the line for another look",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                PulseTile("Today", pulse?.today, Modifier.weight(1f))
                PulseTile("This month", pulse?.month, Modifier.weight(1f))
                PulseTile("Extra", pulse?.extra, Modifier.weight(1f), emphasize = true)
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(
                    onClick = { editingName = true },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Edit name")
                }
                OutlinedButton(
                    onClick = { pickPhotoLauncher.launch("image/*") },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(if (photoPath == null) "Add photo" else "Change photo")
                }
                if (photoPath != null) {
                    TextButton(
                        onClick = viewModel::clearPhoto,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Remove photo")
                    }
                }
            }
    }
    if (editingName) {
        NameEntryDialog(
            title = "Edit name",
            initialName = displayName,
            required = false,
            onSave = {
                viewModel.setName(it)
                editingName = false
            },
            onDismiss = { editingName = false },
        )
    }
}

@Composable
private fun PulseTile(
    label: String,
    amount: Double?,
    modifier: Modifier = Modifier,
    emphasize: Boolean = false,
) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .border(1.dp, colors.outline, RoundedCornerShape(16.dp))
            .padding(horizontal = 10.dp, vertical = 12.dp),
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = colors.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        LcdAmountText(
            text = amount?.let(::formatCurrency) ?: "—",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = if (emphasize) colors.primary else colors.onSurface,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
private fun PlayfulFrame(
    look: Int,
    ink: Color,
    content: @Composable () -> Unit,
) {
    Box(contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(196.dp)) {
            when (look % 4) {
                0 -> {
                    var start = -90f
                    repeat(6) {
                        drawArc(
                            color = ink,
                            startAngle = start,
                            sweepAngle = 52f,
                            useCenter = false,
                            topLeft = Offset(10f, 10f),
                            size = Size(size.width - 20f, size.height - 20f),
                            style = Stroke(width = 14.dp.toPx()),
                        )
                        start += 60f
                    }
                }
                1 -> {
                    val radius = size.minDimension / 2f - 8.dp.toPx()
                    val center = Offset(size.width / 2f, size.height / 2f)
                    repeat(12) { index ->
                        val angle = Math.toRadians((index * 30 - 90).toDouble())
                        drawCircle(
                            color = ink,
                            radius = 7.dp.toPx(),
                            center = Offset(
                                center.x + cos(angle).toFloat() * radius,
                                center.y + sin(angle).toFloat() * radius,
                            ),
                        )
                    }
                }
                2 -> {
                    drawCircle(
                        color = ink,
                        radius = size.minDimension / 2f - 6.dp.toPx(),
                        style = Stroke(width = 8.dp.toPx()),
                    )
                    drawCircle(
                        color = ink,
                        radius = size.minDimension / 2f - 20.dp.toPx(),
                        style = Stroke(width = 4.dp.toPx()),
                    )
                }
                else -> {
                    drawCircle(ink, radius = 28.dp.toPx(), center = Offset(28.dp.toPx(), 36.dp.toPx()))
                    drawCircle(ink, radius = 22.dp.toPx(), center = Offset(size.width - 24.dp.toPx(), 30.dp.toPx()))
                    drawCircle(ink, radius = 18.dp.toPx(), center = Offset(size.width - 36.dp.toPx(), size.height - 22.dp.toPx()))
                    drawCircle(ink, radius = 16.dp.toPx(), center = Offset(26.dp.toPx(), size.height - 28.dp.toPx()))
                }
            }
        }
        content()
    }
}

@Composable
fun ProfileAvatar(
    photoPath: String?,
    size: Dp,
    modifier: Modifier = Modifier,
    name: String? = null,
) {
    val density = LocalDensity.current
    val bitmap = remember(photoPath, size) {
        photoPath?.let { path ->
            val sizePx = with(density) { size.roundToPx() }
            decodeSampledBitmapFromFile(path, sizePx, sizePx)?.asImageBitmap()
        }
    }
    val initial = name?.trim()?.firstOrNull()?.uppercaseChar()?.toString()
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer)
            .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = "Profile photo",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else if (initial != null) {
            Text(
                initial,
                style = if (size < 48.dp) {
                    MaterialTheme.typography.titleMedium
                } else {
                    MaterialTheme.typography.headlineMedium
                },
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        } else {
            Icon(
                Icons.Filled.Person,
                contentDescription = "No profile photo set",
                modifier = Modifier.size(size * 0.46f),
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}
