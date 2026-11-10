package com.alwin.moneymanager.ui.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.alwin.moneymanager.data.local.entity.Emi
import com.alwin.moneymanager.data.local.entity.EmiPayment
import com.alwin.moneymanager.data.local.entity.Expense
import com.alwin.moneymanager.data.repository.CategorySpend
import com.alwin.moneymanager.data.repository.SpendRange
import com.alwin.moneymanager.data.repository.EmiWithProgress
import com.alwin.moneymanager.data.repository.HomeSection
import com.alwin.moneymanager.data.repository.HomeSummary
import com.alwin.moneymanager.data.repository.RecentExpenseItem
import com.alwin.moneymanager.ui.common.AppAddFab
import com.alwin.moneymanager.ui.common.EmptyState
import com.alwin.moneymanager.ui.expense.AddCategoryDialog
import com.alwin.moneymanager.ui.expense.AddExpenseDialog
import com.alwin.moneymanager.ui.expense.CategorySpendChart
import com.alwin.moneymanager.ui.expense.CategoryTotal
import com.alwin.moneymanager.ui.expense.extraAmountChip
import com.alwin.moneymanager.ui.emi.EmiProgressRing
import com.alwin.moneymanager.ui.expense.categoryColor
import com.alwin.moneymanager.ui.profile.ProfileAvatar
import com.alwin.moneymanager.ui.profile.ProfilePhotoViewerDialog
import com.alwin.moneymanager.ui.profile.ProfileViewModel
import com.alwin.moneymanager.ui.theme.LcdAmountText
import com.alwin.moneymanager.ui.theme.LocalIsRetroLcdTheme
import com.alwin.moneymanager.ui.theme.MoneyManagerTheme
import com.alwin.moneymanager.util.formatCurrency
import com.alwin.moneymanager.util.timeOfDayGreeting
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

// macOS-style clean cards with smaller, tighter corner radius for a compact look
@Composable
private fun cardShape(defaultCorner: Dp): RoundedCornerShape =
    if (LocalIsRetroLcdTheme.current) RoundedCornerShape(6.dp) else RoundedCornerShape(8.dp)

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
    backupReminderViewModel: BackupReminderViewModel = hiltViewModel(),
) {
    val summary by viewModel.summary.collectAsState()
    val activeEmis by viewModel.activeEmis.collectAsState()
    val recentActivity by viewModel.recentActivity.collectAsState()
    val rangeSpend by viewModel.categorySpend.collectAsState()
    val spendRange by viewModel.spendRange.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val shouldNudgeBackup by backupReminderViewModel.shouldNudge.collectAsState()
    val showThisMonth by sectionsViewModel.visibility.getValue(HomeSection.THIS_MONTH).collectAsState()
    val showPaymentMethod by sectionsViewModel.visibility.getValue(HomeSection.PAYMENT_METHOD).collectAsState()
    val showEmi by sectionsViewModel.visibility.getValue(HomeSection.EMI).collectAsState()
    val showDebts by sectionsViewModel.visibility.getValue(HomeSection.DEBTS).collectAsState()
    val showMonthlyAverage by sectionsViewModel.visibility.getValue(HomeSection.MONTHLY_AVERAGE).collectAsState()
    val showRecentActivity by sectionsViewModel.visibility.getValue(HomeSection.RECENT_ACTIVITY).collectAsState()
    val profileName by profileViewModel.name.collectAsState()
    val profilePhotoPath by profileViewModel.photoPath.collectAsState()
    val categories by viewModel.categories.collectAsState()
    var showAddExpense by remember { mutableStateOf(false) }
    var showAddCategory by remember { mutableStateOf(false) }

    HomeScreenContent(
        summary = summary,
        activeEmis = activeEmis,
        recentActivity = recentActivity,
        monthCategorySpend = rangeSpend.categories,
        extraSpend = rangeSpend.extra,
        spendRange = spendRange,
        onSpendRangeSelected = viewModel::setSpendRange,
        isLoading = isLoading,
        shouldNudgeBackup = shouldNudgeBackup,
        showThisMonth = showThisMonth,
        showPaymentMethod = showPaymentMethod,
        showEmi = showEmi,
        showDebts = showDebts,
        showMonthlyAverage = showMonthlyAverage,
        showRecentActivity = showRecentActivity,
        profileName = profileName,
        profilePhotoPath = profilePhotoPath,
        onSettingsClick = onSettingsClick,
        onProfileClick = onProfileClick,
        onViewAllEmisClick = onViewAllEmisClick,
        onViewAllExpensesClick = onViewAllExpensesClick,
        onEmiClick = onEmiClick,
        onExpenseDateClick = onExpenseDateClick,
        onSnoozeBackup = backupReminderViewModel::snooze,
        onAddClick = {
            if (categories.ready) {
                if (categories.items.isEmpty()) showAddCategory = true else showAddExpense = true
            }
        },
    )
    if (showAddExpense && categories.items.isNotEmpty()) {
        AddExpenseDialog(
            categories = categories.items,
            onDismiss = { showAddExpense = false },
            onConfirm = { categoryId, amount, note, dateMillis, isCreditCard, isExtra ->
                viewModel.addExpense(categoryId, amount, note, dateMillis, isCreditCard, isExtra)
                showAddExpense = false
            },
        )
    }
    if (showAddCategory) {
        AddCategoryDialog(
            onDismiss = { showAddCategory = false },
            onConfirm = { name, budgetLimit ->
                viewModel.addCategory(name, budgetLimit)
                showAddCategory = false
            },
        )
    }
}

/** Stateless Home UI so it can be exercised from `@Preview` without a Hilt graph. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HomeScreenContent(
    summary: HomeSummary,
    activeEmis: List<EmiWithProgress>,
    recentActivity: List<RecentExpenseItem>,
    monthCategorySpend: List<CategorySpend> = emptyList(),
    extraSpend: Double = 0.0,
    spendRange: SpendRange = SpendRange.MONTH,
    onSpendRangeSelected: (SpendRange) -> Unit = {},
    isLoading: Boolean,
    shouldNudgeBackup: Boolean,
    showThisMonth: Boolean,
    showPaymentMethod: Boolean,
    showEmi: Boolean,
    showDebts: Boolean,
    showMonthlyAverage: Boolean,
    showRecentActivity: Boolean,
    profileName: String?,
    profilePhotoPath: String?,
    onSettingsClick: () -> Unit,
    onProfileClick: () -> Unit,
    onViewAllEmisClick: () -> Unit,
    onViewAllExpensesClick: () -> Unit,
    onEmiClick: (Long) -> Unit,
    onExpenseDateClick: (Long) -> Unit,
    onSnoozeBackup: () -> Unit,
    onAddClick: () -> Unit = {},
) {
    var showPhotoViewer by remember { mutableStateOf(false) }

    if (showPhotoViewer && profilePhotoPath != null) {
        ProfilePhotoViewerDialog(photoPath = profilePhotoPath, onDismiss = { showPhotoViewer = false })
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ProfileAvatar(
                            photoPath = profilePhotoPath,
                            name = profileName,
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
        },
        floatingActionButton = {
            AppAddFab(contentDescription = "Add expense", onClick = onAddClick)
        },
    ) { innerPadding ->
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            val hasData = activeEmis.isNotEmpty() || recentActivity.isNotEmpty()
            if (shouldNudgeBackup && hasData) {
                BackupNudgeCard(
                    onBackUpClick = onSettingsClick,
                    onDismiss = onSnoozeBackup,
                    modifier = Modifier.padding(bottom = 12.dp),
                )
            }

            if (showThisMonth) {
                val expenseTotal = monthCategorySpend.sumOf { it.amount }
                HeroCard(
                    label = when (spendRange) {
                        SpendRange.MONTH -> "This month"
                        SpendRange.YEAR -> "This year"
                        SpendRange.ALL -> "All time"
                    },
                    value = formatCurrency(expenseTotal),
                    subLabel = if (spendRange == SpendRange.MONTH) {
                        "Including EMI ${formatCurrency(summary.monthIncludingEmi)}"
                    } else {
                        "Expenses"
                    },
                    compareLabel = if (spendRange == SpendRange.MONTH) {
                        monthCompareLabel(expenseTotal, summary.lastMonthToDate)
                    } else {
                        null
                    },
                    selected = spendRange,
                    onSelect = onSpendRangeSelected,
                )
            }

            if (showThisMonth && monthCategorySpend.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                ) {
                    Text(
                        when (spendRange) {
                            SpendRange.MONTH -> "This month"
                            SpendRange.YEAR -> "This year"
                            SpendRange.ALL -> "All time"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    CategorySpendChart(
                        categories = monthCategorySpend.map {
                            CategoryTotal(it.categoryId, it.name, it.amount)
                        },
                        extraAmount = extraSpend,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }

            val quickStats = remember(
                summary,
                showThisMonth,
                showPaymentMethod,
                showEmi,
                showDebts,
                showMonthlyAverage,
            ) {
                buildList {
                    if (showThisMonth) {
                        add(QuickStat("Today's spend", formatCurrency(summary.todayTotal)))
                    }
                    if (showPaymentMethod) {
                        add(QuickStat("Credit card", formatCurrency(summary.creditCardThisMonth)))
                        add(QuickStat("Savings account", formatCurrency(summary.savingsThisMonth)))
                    }
                    if (showThisMonth) {
                        add(QuickStat("Extra this month", formatCurrency(summary.extraThisMonth)))
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
                    if (showDebts) {
                        add(QuickStat("Owed to you", formatCurrency(summary.debtToCollect)))
                        add(QuickStat("You owe", formatCurrency(summary.debtToPay)))
                    }
                    if (showMonthlyAverage) {
                        add(QuickStat("Avg (expenses)", formatCurrency(summary.monthlyAverageExpenseOnly)))
                        add(QuickStat("Avg (incl. EMI)", formatCurrency(summary.monthlyAverageIncludingEmi)))
                    }
                }
            }
            if (quickStats.isNotEmpty()) {
                QuickStatsGrid(stats = quickStats, modifier = Modifier.padding(top = 8.dp))
            }

            val previewEmis = remember(activeEmis) { activeEmis.take(4) }
            if (showEmi && previewEmis.isNotEmpty()) {
                HomeBlock(title = "Active loans", onViewAllClick = onViewAllEmisClick) {
                    previewEmis.forEachIndexed { index, item ->
                        EmiPreviewRow(item = item, onClick = { onEmiClick(item.emi.id) })
                        if (index != previewEmis.lastIndex) HorizontalDivider()
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

            if (activeEmis.isEmpty() && recentActivity.isEmpty()) {
                EmptyState(
                    icon = Icons.Filled.Receipt,
                    title = "Welcome to FinBook",
                    subtitle = "Add your first expense or loan from the tabs below to see it show up here.",
                    modifier = Modifier.padding(top = 24.dp),
                )
            }
        }
        }
    }
}

@Composable
private fun BackupNudgeCard(
    onBackUpClick: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // macOS-style flat alert banner
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f),
                shape = RoundedCornerShape(10.dp)
            )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.CloudUpload,
                    contentDescription = "Backup reminder",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(22.dp),
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    "Time to back up",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = "Dismiss",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
            Text(
                "Export a backup or enable Auto backup in Settings.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp),
            )
            TextButton(
                onClick = onBackUpClick,
                modifier = Modifier.align(Alignment.End).padding(top = 4.dp),
            ) { Text("Back up now") }
        }
    }
}

@Composable
private fun HeroCard(
    label: String,
    value: String,
    subLabel: String,
    compareLabel: String? = null,
    selected: SpendRange,
    onSelect: (SpendRange) -> Unit,
) {
    val shape = RoundedCornerShape(10.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp)
            .border(1.dp, MaterialTheme.colorScheme.outline, shape)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            LcdAmountText(
                value,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 2.dp),
            )
            Text(
                subLabel,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp),
            )
            if (compareLabel != null) {
                Text(
                    compareLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
        Text(
            when (selected) {
                SpendRange.MONTH -> "Month"
                SpendRange.YEAR -> "Year"
                SpendRange.ALL -> "All"
            },
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .padding(start = 8.dp)
                .clip(RoundedCornerShape(20.dp))
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(20.dp))
                .clickable {
                    val next = when (selected) {
                        SpendRange.MONTH -> SpendRange.YEAR
                        SpendRange.YEAR -> SpendRange.ALL
                        SpendRange.ALL -> SpendRange.MONTH
                    }
                    onSelect(next)
                }
                .padding(horizontal = 14.dp, vertical = 6.dp),
        )
    }
}

private fun monthCompareLabel(thisMonth: Double, lastMonth: Double): String {
    val difference = thisMonth - lastMonth
    val amount = formatCurrency(kotlin.math.abs(difference))
    return when {
        kotlin.math.abs(difference) < 1.0 -> "Same as last month"
        difference > 0 -> "$amount more than last month"
        else -> "$amount less than last month"
    }
}

private data class QuickStat(val label: String, val value: String, val emphasize: Boolean = false)

private data class PlacedStat(val stat: QuickStat, val weight: Float, val index: Int)

/** Uneven widths that still add up to a full row. */
private val widthPairs = listOf(
    1.45f to 0.85f,
    0.7f to 1.3f,
    1.15f to 0.95f,
    0.8f to 1.55f,
)

private fun unevenRows(stats: List<QuickStat>): List<List<PlacedStat>> {
    val rows = mutableListOf<List<PlacedStat>>()
    var index = 0
    var pair = 0
    while (index < stats.size) {
        if (index == stats.lastIndex) {
            rows += listOf(PlacedStat(stats[index], 1f, index))
            break
        }
        val (left, right) = widthPairs[pair % widthPairs.size]
        rows += listOf(
            PlacedStat(stats[index], left, index),
            PlacedStat(stats[index + 1], right, index + 1),
        )
        index += 2
        pair++
    }
    return rows
}

@Composable
private fun QuickStatsGrid(stats: List<QuickStat>, modifier: Modifier = Modifier) {
    val rows = remember(stats) { unevenRows(stats) }
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                row.forEach { placed ->
                    QuickStatTile(
                        stat = placed.stat,
                        modifier = Modifier.weight(placed.weight).fillMaxHeight(),
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickStatTile(
    stat: QuickStat,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(10.dp)
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .border(1.dp, colors.outline, shape)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            stat.label,
            style = MaterialTheme.typography.labelSmall,
            color = colors.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        LcdAmountText(
            stat.value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = if (stat.emphasize) colors.error else colors.onSurface,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}

@Composable
private fun HomeBlock(
    title: String,
    onViewAllClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text(
            "View all",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .defaultMinSize(minHeight = 48.dp)
                .clickable(role = Role.Button, onClick = onViewAllClick)
                .padding(horizontal = 8.dp, vertical = 12.dp),
        )
    }
    // macOS-style flat list container
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(10.dp)
            )
            .then(
                Modifier.background(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(10.dp)
                )
            )
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp)) {
            content()
        }
    }
}

@Composable
private fun EmiPreviewRow(item: EmiWithProgress, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .defaultMinSize(minHeight = 56.dp)
            .padding(vertical = 10.dp),
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
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .defaultMinSize(minHeight = 56.dp)
            .padding(vertical = 10.dp),
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
        LcdAmountText(
            formatCurrency(item.expense.amount),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = if (item.expense.isExtra) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            modifier = Modifier
                .padding(start = 8.dp)
                .extraAmountChip(item.expense.isExtra),
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

private val previewSummary = HomeSummary(
    todayTotal = 450.0,
    monthExpenseOnly = 18250.0,
    monthIncludingEmi = 26400.0,
    monthlyAverageExpenseOnly = 17500.0,
    monthlyAverageIncludingEmi = 25100.0,
    emiDueThisMonth = 8150.0,
    totalEmiOutstanding = 96400.0,
    creditCardThisMonth = 6200.0,
    savingsThisMonth = 12050.0,
    extraThisMonth = 800.0,
    lastMonthToDate = 16000.0,
    debtToCollect = 2000.0,
    debtToPay = 500.0,
)

private val previewActiveEmis = listOf(
    EmiWithProgress(
        emi = Emi(id = 1, name = "Car loan", monthlyAmount = 8150.0, totalMonths = 36, startDateMillis = 0),
        payments = List(10) { EmiPayment(emiId = 1, monthNumber = it + 1, paidDateMillis = 0) },
    ),
    EmiWithProgress(
        emi = Emi(id = 2, name = "Laptop EMI", monthlyAmount = 3200.0, totalMonths = 12, startDateMillis = 0),
        payments = List(9) { EmiPayment(emiId = 2, monthNumber = it + 1, paidDateMillis = 0) },
    ),
)

private val previewRecentActivity = listOf(
    RecentExpenseItem(
        expense = Expense(id = 1, categoryId = 1, amount = 450.0, dateMillis = System.currentTimeMillis(), isCreditCard = true),
        categoryName = "Groceries",
    ),
    RecentExpenseItem(
        expense = Expense(id = 2, categoryId = 2, amount = 1200.0, dateMillis = System.currentTimeMillis() - 86_400_000),
        categoryName = "Utilities",
    ),
    RecentExpenseItem(
        expense = Expense(id = 3, categoryId = 3, amount = 320.0, dateMillis = System.currentTimeMillis() - 86_400_000),
        categoryName = "Dining out",
    ),
)

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    MoneyManagerTheme {
        HomeScreenContent(
            summary = previewSummary,
            activeEmis = previewActiveEmis,
            recentActivity = previewRecentActivity,
            isLoading = false,
            shouldNudgeBackup = true,
            showThisMonth = true,
            showPaymentMethod = true,
            showEmi = true,
            showDebts = true,
            showMonthlyAverage = true,
            showRecentActivity = true,
            profileName = "Alwin",
            profilePhotoPath = null,
            onSettingsClick = {},
            onProfileClick = {},
            onViewAllEmisClick = {},
            onViewAllExpensesClick = {},
            onEmiClick = {},
            onExpenseDateClick = {},
            onSnoozeBackup = {},
        )
    }
}

@Preview(showBackground = true, name = "Empty state")
@Composable
private fun HomeScreenEmptyPreview() {
    MoneyManagerTheme {
        HomeScreenContent(
            summary = previewSummary.copy(
                todayTotal = 0.0,
                monthExpenseOnly = 0.0,
                monthIncludingEmi = 0.0,
                emiDueThisMonth = 0.0,
                totalEmiOutstanding = 0.0,
                creditCardThisMonth = 0.0,
                savingsThisMonth = 0.0,
                debtToCollect = 0.0,
                debtToPay = 0.0,
            ),
            activeEmis = emptyList(),
            recentActivity = emptyList(),
            isLoading = false,
            shouldNudgeBackup = false,
            showThisMonth = true,
            showPaymentMethod = true,
            showEmi = true,
            showDebts = true,
            showMonthlyAverage = true,
            showRecentActivity = true,
            profileName = null,
            profilePhotoPath = null,
            onSettingsClick = {},
            onProfileClick = {},
            onViewAllEmisClick = {},
            onViewAllExpensesClick = {},
            onEmiClick = {},
            onExpenseDateClick = {},
            onSnoozeBackup = {},
        )
    }
}
