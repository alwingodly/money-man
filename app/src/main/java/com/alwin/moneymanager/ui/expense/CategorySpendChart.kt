package com.alwin.moneymanager.ui.expense

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.alwin.moneymanager.util.formatCurrency
import kotlin.math.roundToInt

private const val VISIBLE_ROWS = 5

/** Light, separated colors. The biggest slice stays on the theme hue. */
private fun sliceColor(index: Int, primary: Color): Color {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(primary.toArgb(), hsv)
    val hue = (hsv[0] + index * 51f) % 360f
    val saturation = if (index % 2 == 0) 0.72f else 0.5f
    val lightness = if (index % 2 == 0) 0.62f else 0.78f
    return Color.hsl(hue, saturation, lightness)
}

@Composable
fun CategorySpendChart(
    categories: List<CategoryTotal>,
    modifier: Modifier = Modifier,
    detailed: Boolean = false,
    extraAmount: Double? = null,
) {
    if (categories.isEmpty()) return

    val total = categories.sumOf { it.amount }
    val slices = categories.filter { it.amount > 0.0 }
    val primary = MaterialTheme.colorScheme.primary

    if (detailed) {
        Column(
            modifier = modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            SpendCanvas(slices = slices, total = total, donut = false, chartSize = 220.dp, primary = primary)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                categories.forEach { category ->
                    CategoryRow(category, slices, total, showShare = true, primary = primary)
                }
            }
        }
        return
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        var showSplit by rememberSaveable { mutableStateOf(false) }
        val actualAmount = ((extraAmount?.let { total - it }) ?: total).coerceAtLeast(0.0)
        val splitCategories = listOf(
            CategoryTotal(-1L, "Actual", actualAmount),
            CategoryTotal(-2L, "Extra", (extraAmount ?: 0.0).coerceAtLeast(0.0)),
        )
        val shown = if (showSplit && extraAmount != null) splitCategories else categories
        val shownSlices = shown.filter { it.amount > 0.0 }
        Box(contentAlignment = Alignment.Center) {
            SpendCanvas(
                slices = shownSlices,
                total = shown.sumOf { it.amount },
                donut = true,
                chartSize = 156.dp,
                primary = primary,
            )
            if (extraAmount != null) {
                Text(
                    if (showSplit) "Back" else "Extra",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                        .clickable { showSplit = !showSplit }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }
        }
        var showAll by rememberSaveable { mutableStateOf(false) }
        val visible = if (showSplit || showAll) shown else shown.take(VISIBLE_ROWS)
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            visible.forEach { category ->
                CategoryRow(category, shownSlices, shown.sumOf { it.amount }, showShare = false, primary = primary)
            }
            if (!showSplit && categories.size > VISIBLE_ROWS) {
                Text(
                    if (showAll) "Show less" else "Show all ${categories.size}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clickable { showAll = !showAll }
                        .padding(top = 2.dp),
                )
            }
        }
    }
}

@Composable
private fun SpendCanvas(
    slices: List<CategoryTotal>,
    total: Double,
    donut: Boolean,
    chartSize: Dp,
    primary: Color,
) {
    Canvas(modifier = Modifier.size(chartSize)) {
        val strokeWidth = 28.dp.toPx()
        val diameter = if (donut) size.minDimension - strokeWidth else size.minDimension
        val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
        val ring = Stroke(width = strokeWidth)
        if (slices.isEmpty() || total <= 0.0) {
            drawArc(
                color = Color(0xFFE4E4E4),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = !donut,
                topLeft = topLeft,
                size = Size(diameter, diameter),
                style = if (donut) ring else Fill,
            )
            return@Canvas
        }
        var start = -90f
        slices.forEachIndexed { index, category ->
            val sweep = (category.amount / total * 360.0).toFloat()
            drawArc(
                color = sliceColor(index, primary),
                startAngle = start,
                sweepAngle = if (index == slices.lastIndex) sweep else sweep + if (donut) 0.6f else 1.2f,
                useCenter = !donut,
                topLeft = topLeft,
                size = Size(diameter, diameter),
                style = if (donut) ring else Fill,
            )
            start += sweep
        }
    }
}

@Composable
private fun CategoryRow(
    category: CategoryTotal,
    slices: List<CategoryTotal>,
    total: Double,
    showShare: Boolean,
    primary: Color,
) {
    val dot = if (category.amount > 0.0) {
        val sliceIndex = slices.indexOfFirst { it.categoryId == category.categoryId }
        sliceColor(sliceIndex.coerceAtLeast(0), primary)
    } else {
        Color(0xFFBDBDBD)
    }
    val share = if (total > 0.0) (category.amount / total * 100.0).roundToInt() else 0
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(dot),
        )
        Text(
            category.categoryName,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp),
        )
        if (showShare) {
            Text(
                "$share%",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = 10.dp),
            )
        }
        Text(
            formatCurrency(category.amount),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
