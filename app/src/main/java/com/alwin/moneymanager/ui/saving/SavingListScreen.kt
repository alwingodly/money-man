package com.alwin.moneymanager.ui.saving

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.alwin.moneymanager.data.local.entity.SavingBucket
import com.alwin.moneymanager.data.repository.SavingWithProgress
import com.alwin.moneymanager.ui.common.AppAddFab
import com.alwin.moneymanager.ui.common.AppListCard
import com.alwin.moneymanager.ui.common.ConfirmDeleteDialog
import com.alwin.moneymanager.ui.common.EmptyState
import com.alwin.moneymanager.ui.theme.LcdAmountText
import com.alwin.moneymanager.util.formatCurrency

@Composable
fun SavingListScreen(
    onSavingClick: (Long) -> Unit,
    viewModel: SavingViewModel = hiltViewModel(),
) {
    val savings by viewModel.savings.collectAsState()
    val totalSaved by viewModel.totalSaved.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val buckets by viewModel.buckets.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    var showAddSaving by remember { mutableStateOf(false) }
    var showAddBucket by remember { mutableStateOf(false) }
    var editingBucket by remember { mutableStateOf<SavingBucket?>(null) }
    var deletingBucket by remember { mutableStateOf<SavingBucket?>(null) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Savings") }) },
        floatingActionButton = {
            AppAddFab(
                contentDescription = if (selectedTab == 0) "New saving" else "Add to bucket list",
                onClick = {
                    if (selectedTab == 0) showAddSaving = true else showAddBucket = true
                },
            )
        },
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Pots") },
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Bucket list") },
                )
            }

            when (selectedTab) {
                0 -> SavingsTab(
                    savings = savings,
                    totalSaved = totalSaved,
                    isLoading = isLoading,
                    onSavingClick = onSavingClick,
                    onAddClick = { showAddSaving = true },
                )
                else -> BucketListTab(
                    buckets = buckets,
                    totalSaved = totalSaved,
                    onToggleDone = { bucket, done -> viewModel.setBucketDone(bucket, done) },
                    onEdit = { editingBucket = it },
                    onAddClick = { showAddBucket = true },
                )
            }
        }
    }

    if (showAddSaving) {
        SavingFormDialog(
            title = "New saving",
            onDismiss = { showAddSaving = false },
            onConfirm = { form ->
                viewModel.addSaving(form)
                showAddSaving = false
            },
        )
    }

    if (showAddBucket) {
        BucketFormDialog(
            title = "Add to bucket list",
            onDismiss = { showAddBucket = false },
            onConfirm = { form ->
                viewModel.addBucket(form)
                showAddBucket = false
            },
        )
    }

    editingBucket?.let { bucket ->
        BucketFormDialog(
            title = "Edit item",
            initialName = bucket.name,
            initialAmount = trimAmount(bucket.amount),
            initialNote = bucket.note,
            onDismiss = { editingBucket = null },
            onConfirm = { form ->
                viewModel.updateBucket(bucket, form)
                editingBucket = null
            },
            onDelete = {
                deletingBucket = bucket
                editingBucket = null
            },
        )
    }

    deletingBucket?.let { bucket ->
        ConfirmDeleteDialog(
            title = "Delete this item?",
            message = "Remove “${bucket.name}” from your bucket list.",
            onConfirm = {
                viewModel.deleteBucket(bucket)
                deletingBucket = null
            },
            onDismiss = { deletingBucket = null },
        )
    }
}

@Composable
private fun SavingsTab(
    savings: List<SavingWithProgress>,
    totalSaved: Double,
    isLoading: Boolean,
    onSavingClick: (Long) -> Unit,
    onAddClick: () -> Unit,
) {
    when {
        isLoading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        savings.isEmpty() -> {
            EmptyState(
                icon = Icons.Filled.Savings,
                title = "No savings yet",
                subtitle = "Start a savings pot — for a goal like a new phone, or just an open-ended stash.",
                modifier = Modifier.fillMaxSize(),
                actionLabel = "New saving",
                onAction = onAddClick,
            )
        }
        else -> {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "Total saved",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                        )
                        LcdAmountText(
                            formatCurrency(totalSaved),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
                items(savings, key = { it.saving.id }) { item ->
                    SavingRow(item = item, onClick = { onSavingClick(item.saving.id) })
                }
            }
        }
    }
}

@Composable
private fun BucketListTab(
    buckets: List<SavingBucket>,
    totalSaved: Double,
    onToggleDone: (SavingBucket, Boolean) -> Unit,
    onEdit: (SavingBucket) -> Unit,
    onAddClick: () -> Unit,
) {
    if (buckets.isEmpty()) {
        EmptyState(
            icon = Icons.AutoMirrored.Filled.List,
            title = "Nothing on the list yet",
            subtitle = "Add something you want — a trip, a phone, anything — and how much it costs.",
            modifier = Modifier.fillMaxSize(),
            actionLabel = "Add to bucket list",
            onAction = onAddClick,
        )
        return
    }

    val openAmount = remember(buckets) { buckets.filter { !it.isDone }.sumOf { it.amount } }
    val remaining = (openAmount - totalSaved).coerceAtLeast(0.0)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Still needed: ${formatCurrency(openAmount)}",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    "Saved: ${formatCurrency(totalSaved)}",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (remaining > 0) {
                Text(
                    "${formatCurrency(remaining)} more to cover the list",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else if (openAmount > 0) {
                Text(
                    "Your savings cover the open list",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
        items(buckets, key = { it.id }) { bucket ->
            BucketRow(
                bucket = bucket,
                onToggleDone = { onToggleDone(bucket, it) },
                onClick = { onEdit(bucket) },
            )
        }
    }
}

@Composable
private fun SavingRow(item: SavingWithProgress, onClick: () -> Unit) {
    val saving = item.saving
    AppListCard(onClick = onClick) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(saving.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    val subtitle = when {
                        item.isAchieved -> "Goal reached!"
                        saving.targetAmount != null -> "of ${formatCurrency(saving.targetAmount)} goal"
                        else -> "Open-ended pot"
                    }
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                LcdAmountText(
                    formatCurrency(item.totalSaved),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            item.progress?.let { progress ->
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun BucketRow(
    bucket: SavingBucket,
    onToggleDone: (Boolean) -> Unit,
    onClick: () -> Unit,
) {
    AppListCard(
        onClick = onClick,
        modifier = Modifier.alpha(if (bucket.isDone) 0.55f else 1f),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(
                checked = bucket.isDone,
                onCheckedChange = onToggleDone,
            )
            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                Text(
                    bucket.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    textDecoration = if (bucket.isDone) TextDecoration.LineThrough else null,
                )
                if (bucket.note.isNotBlank()) {
                    Text(
                        bucket.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            LcdAmountText(
                formatCurrency(bucket.amount),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

private fun trimAmount(value: Double): String =
    if (value % 1.0 == 0.0) value.toLong().toString() else value.toString()
