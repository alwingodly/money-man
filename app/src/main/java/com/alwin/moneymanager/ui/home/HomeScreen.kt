package com.alwin.moneymanager.ui.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.alwin.moneymanager.data.repository.EmiWithProgress
import com.alwin.moneymanager.data.repository.HomeSection
import com.alwin.moneymanager.data.repository.RecentExpenseItem
import com.alwin.moneymanager.ui.emi.EmiProgressRing
import com.alwin.moneymanager.ui.expense.categoryColor
import com.alwin.moneymanager.ui.profile.ProfileAvatar
import com.alwin.moneymanager.ui.profile.ProfilePhotoViewerDialog
import com.alwin.moneymanager.ui.profile.ProfileViewModel
import com.alwin.moneymanager.util.formatCurrency
import com.alwin.moneymanager.util.timeOfDayGreeting
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    onSettingsClick: () -> Unit,
    onProfileClick: () -> Unit,
    onViewAllEmisClick: () -> Unit,
    onViewAllExpensesClick: () -> Unit,
    onEmiClick: (Long) -> Unit,
    onExpenseDateClick: (Long) -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
    sectionsViewModel: HomeSectionsViewModel = hiltViewModel(),
    profileViewModel: ProfileViewModel = hiltViewModel(),
) {
    val summary by viewModel.summary.collectAsState()
    val activeEmis by viewModel.activeEmis.collectAsState()
    val recentActivity by viewModel.recentActivity.collectAsState()
    val showThisMonth by sectionsViewModel.visibility.getValue(HomeSection.THIS_MONTH).collectAsState()
    val showPaymentMethod by sectionsViewModel.visibility.getValue(HomeSection.PAYMENT_METHOD).collectAsState()
    val showEmi by sectionsViewModel.visibility.getValue(HomeSection.EMI).collectAsState()
    val showMonthlyAverage by sectionsViewModel.visibility.getValue(HomeSection.MONTHLY_AVERAGE).collectAsState()
    val showRecentActivity by sectionsViewModel.visibility.getValue(HomeSection.RECENT_ACTIVITY).collectAsState()
    val profileName by profileViewModel.name.collectAsState()
    val profilePhotoPath by profileViewModel.photoPath.collectAsState()
    var showPhotoViewer by remember { mutableStateOf(false) }

    if (showPhotoViewer && profilePhotoPath != null) {
        ProfilePhotoViewerDialog(photoPath = profilePhotoPath!!, onDismiss = { showPhotoViewer = false })
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ProfileAvatar(
                            photoPath = profilePhotoPath,
                            size = 36.dp,
                            modifier = Modifier.combinedClickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = onProfileClick,
                                onLongClick = { if (profilePhotoPath != null) showPhotoViewer = true },
                            ),
                        )
                        Spacer(Modifier.width(10.dp))
                        Text("${timeOfDayGreeting()}, ${profileName?.ifBlank { null } ?: "Welcome"}")
                    }
                },
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                },
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            if (showThisMonth) {
                HeroCard(
                    label = "This month, incl. EMI",
                    value = formatCurrency(summary.monthIncludingEmi),
                    subLabel = "Expenses only: ${formatCurrency(summary.monthExpenseOnly)}",
                )
            }

            val quickStats = buildList {
                if (showThisMonth) {
                    add(QuickStat("Today's spend", formatCurrency(summary.todayTotal)))
                }
                if (showPaymentMethod) {
                    add(QuickStat("Credit card", formatCurrency(summary.creditCardThisMonth)))
                    add(QuickStat("Savings account", formatCurrency(summary.savingsThisMonth)))
                }
                if (showEmi) {
                    add(
                        QuickStat(
                            "EMI still due",
                            formatCurrency(summary.emiDueThisMonth),
                            emphasize = summary.emiDueThisMonth > 0,
                        )
                    )
                    add(QuickStat("Total outstanding", formatCurrency(summary.totalEmiOutstanding)))
                }
                if (showMonthlyAverage) {
                    add(QuickStat("Avg (expenses)", formatCurrency(summary.monthlyAverageExpenseOnly)))
                    add(QuickStat("Avg (incl. EMI)", formatCurrency(summary.monthlyAverageIncludingEmi)))
                }
            }
            if (quickStats.isNotEmpty()) {
                QuickStatsGrid(stats = quickStats, modifier = Modifier.padding(top = 8.dp))
            }

            if (showEmi && activeEmis.isNotEmpty()) {
                HomeBlock(title = "Active loans", onViewAllClick = onViewAllEmisClick) {
                    activeEmis.take(4).forEachIndexed { index, item ->
                        EmiPreviewRow(item = item, onClick = { onEmiClick(item.emi.id) })
                        if (index != activeEmis.take(4).lastIndex) HorizontalDivider()
                    }
                }
            }

            if (showRecentActivity && recentActivity.isNotEmpty()) {
                HomeBlock(title = "Recent activity", onViewAllClick = onViewAllExpensesClick) {
                    recentActivity.forEachIndexed { index, item ->
                        ActivityRow(item = item, onClick = { onExpenseDateClick(item.expense.dateMillis) })
                        if (index != recentActivity.lastIndex) HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroCard(label: String, value: String, subLabel: String) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Text(
                value,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(top = 4.dp),
            )
            Text(
                subLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}

private data class QuickStat(val label: String, val value: String, val emphasize: Boolean = false)

@Composable
private fun QuickStatsGrid(stats: List<QuickStat>, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        stats.chunked(2).forEach { rowStats ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                rowStats.forEach { stat -> QuickStatTile(stat, modifier = Modifier.weight(1f)) }
                if (rowStats.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun QuickStatTile(stat: QuickStat, modifier: Modifier = Modifier) {
    ElevatedCard(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                stat.label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                stat.value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (stat.emphasize) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Composable
private fun HomeBlock(
    title: String,
    onViewAllClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 20.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        Text(
            "View all",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.clickable(onClick = onViewAllClick),
        )
    }
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            content()
        }
    }
}

@Composable
private fun EmiPreviewRow(item: EmiWithProgress, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
                item.emi.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            item.nextDueDateMillis?.let { dueMillis ->
                Text(
                    "Next due ${shortDateLabel(dueMillis)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
        EmiProgressRing(
            paidMonths = item.paidMonths,
            totalMonths = item.emi.totalMonths,
            progress = item.progressPercent,
            size = 48.dp,
        )
    }
}

@Composable
private fun ActivityRow(item: RecentExpenseItem, onClick: () -> Unit) {
    val color = categoryColor(item.expense.categoryId)
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(40.dp).background(color.copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                item.categoryName.take(1).uppercase(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = color,
            )
        }
        Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    item.categoryName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (item.expense.isCreditCard) {
                    Icon(
                        Icons.Filled.CreditCard,
                        contentDescription = "Paid by credit card",
                        modifier = Modifier.padding(start = 6.dp).size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Text(
                relativeDayLabel(item.expense.dateMillis),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        Text(
            formatCurrency(item.expense.amount),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

private val shortDateFormatter = DateTimeFormatter.ofPattern("d MMM", Locale.getDefault())

private fun shortDateLabel(millis: Long): String =
    Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate().format(shortDateFormatter)

private fun relativeDayLabel(millis: Long): String {
    val date = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
    val today = LocalDate.now()
    return when (date) {
        today -> "Today"
        today.minusDays(1) -> "Yesterday"
        else -> date.format(shortDateFormatter)
    }
}
