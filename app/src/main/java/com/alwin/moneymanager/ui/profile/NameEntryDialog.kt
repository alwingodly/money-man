package com.alwin.moneymanager.ui.profile

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun FirstNamePrompt(viewModel: ProfileViewModel = hiltViewModel()) {
    val loaded by viewModel.loadedName.collectAsState()
    if (!loaded.loaded || loaded.name.isNotBlank()) return
    NameEntryDialog(
        title = "What should we call you?",
        initialName = "",
        required = true,
        onSave = viewModel::setName,
        onDismiss = {},
    )
}

@Composable
fun NameEntryDialog(
    title: String,
    initialName: String,
    required: Boolean,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember(initialName) { mutableStateOf(initialName) }
    AlertDialog(
        onDismissRequest = { if (!required) onDismiss() },
        properties = DialogProperties(
            dismissOnClickOutside = false,
            dismissOnBackPress = !required,
        ),
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Your name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank(),
                onClick = { onSave(name.trim()) },
            ) { Text("Save") }
        },
        dismissButton = if (required) {
            null
        } else {
            { TextButton(onClick = onDismiss) { Text("Cancel") } }
        },
    )
}
