package com.alwin.moneymanager.ui.profile

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.alwin.moneymanager.util.decodeSampledBitmapFromFile

@Composable
fun ProfileScreen(
    onBack: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val savedName by viewModel.name.collectAsState()
    val photoPath by viewModel.photoPath.collectAsState()
    var name by remember { mutableStateOf("") }
    var hasLoadedName by remember { mutableStateOf(false) }

    LaunchedEffect(savedName) {
        if (!hasLoadedName) {
            name = savedName ?: ""
            hasLoadedName = true
        }
    }

    val pickPhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri -> uri?.let(viewModel::setPhoto) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ProfileAvatar(photoPath = photoPath, size = 120.dp)

            OutlinedButton(
                onClick = { pickPhotoLauncher.launch("image/*") },
                modifier = Modifier.padding(top = 12.dp),
            ) { Text("Change photo") }

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Your name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
            )

            Button(
                onClick = { viewModel.setName(name.trim()) },
                enabled = name.trim() != (savedName ?: ""),
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            ) { Text("Save") }
        }
    }
}

@Composable
fun ProfileAvatar(photoPath: String?, size: androidx.compose.ui.unit.Dp, modifier: Modifier = Modifier) {
    val density = LocalDensity.current
    val bitmap = remember(photoPath, size) {
        photoPath?.let { path ->
            val sizePx = with(density) { size.roundToPx() }
            decodeSampledBitmapFromFile(path, sizePx, sizePx)?.asImageBitmap()
        }
    }
    Box(
        modifier = modifier.size(size).clip(CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = "Profile photo",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Icon(
                Icons.Filled.AccountCircle,
                contentDescription = "No profile photo set",
                modifier = Modifier.fillMaxSize(),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}
