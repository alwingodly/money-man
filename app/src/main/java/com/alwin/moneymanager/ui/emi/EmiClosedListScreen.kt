package com.alwin.moneymanager.ui.emi

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.alwin.moneymanager.ui.common.EmptyState

@Composable
fun EmiClosedListScreen(
    onBack: () -> Unit,
    onEmiClick: (Long) -> Unit,
    viewModel: EmiViewModel = hiltViewModel(),
) {
    val closedEmiList by viewModel.closedEmiList.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Closed loans") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        }
    ) { innerPadding ->
        if (closedEmiList.isEmpty()) {
            EmptyState(
                icon = Icons.Filled.CheckCircle,
                title = "No closed loans yet",
                subtitle = "Loans you finish paying off will show up here.",
                modifier = Modifier.fillMaxSize().padding(innerPadding),
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp, innerPadding.calculateTopPadding(), 16.dp, 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(closedEmiList, key = { it.emi.id }) { item ->
                    EmiCard(item = item, onClick = { onEmiClick(item.emi.id) })
                }
            }
        }
    }
}
