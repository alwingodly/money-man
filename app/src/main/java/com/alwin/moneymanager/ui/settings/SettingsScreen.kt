package com.alwin.moneymanager.ui.settings

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.alwin.moneymanager.data.repository.HomeSection
import com.alwin.moneymanager.ui.applock.AppLockViewModel
import com.alwin.moneymanager.ui.home.HomeSectionsViewModel
import com.alwin.moneymanager.ui.navigation.NavTabsViewModel
import com.alwin.moneymanager.ui.onboarding.OnboardingViewModel
import com.alwin.moneymanager.ui.theme.AppThemeColor
import com.alwin.moneymanager.ui.theme.AppThemeMode
import com.alwin.moneymanager.ui.theme.AppThemeStyle
import com.alwin.moneymanager.ui.theme.ThemeViewModel
import com.alwin.moneymanager.util.CurrencyType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    appLockViewModel: AppLockViewModel = hiltViewModel(),
    homeSectionsViewModel: HomeSectionsViewModel = hiltViewModel(),
    themeViewModel: ThemeViewModel = hiltViewModel(),
    currencyViewModel: CurrencyViewModel = hiltViewModel(),
    backupViewModel: BackupViewModel = hiltViewModel(),
    onboardingViewModel: OnboardingViewModel = hiltViewModel(),
    navTabsViewModel: NavTabsViewModel = hiltViewModel(),
) {
    val showDebtsTab by navTabsViewModel.showDebts.collectAsState()
    val showSavingsTab by navTabsViewModel.showSavings.collectAsState()
    val isAppLockEnabled by appLockViewModel.isAppLockEnabled.collectAsState()
    val lockOnBackground by appLockViewModel.lockOnBackground.collectAsState()
    val isBackupWorking by backupViewModel.isWorking.collectAsState()
    val backupEvent by backupViewModel.event.collectAsState()
    val isAutoBackupEnabled by backupViewModel.isAutoBackupEnabled.collectAsState()
    val lastBackupMillis by backupViewModel.lastBackupMillis.collectAsState()
    var pendingImportUri by remember { mutableStateOf<android.net.Uri?>(null) }
    val context = LocalContext.current

    LaunchedEffect(backupEvent) {
        val event = backupEvent ?: return@LaunchedEffect
        val message = when (event) {
            BackupEvent.ExportSucceeded -> "Backup saved"
            BackupEvent.ImportSucceeded -> "Data restored from backup"
            BackupEvent.AutoBackupEnabled -> "Auto backup on — file updates when you leave the app"
            is BackupEvent.Failed -> event.message
        }
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        backupViewModel.consumeEvent()
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"),
    ) { uri -> uri?.let(backupViewModel::exportTo) }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri -> uri?.let { pendingImportUri = it } }

    val autoBackupFolderLauncher = rememberLauncherForActivityResult(
        contract = object : ActivityResultContracts.OpenDocumentTree() {
            override fun createIntent(context: android.content.Context, input: Uri?): Intent {
                return super.createIntent(context, input).addFlags(
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                        Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION,
                )
            }
        },
    ) { uri -> uri?.let(backupViewModel::enableAutoBackup) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
            SettingsSection(title = "Security", icon = Icons.Filled.Lock) {
                SettingRow(
                    title = "App Lock",
                    subtitle = "Require biometric or device PIN to open the app",
                    checked = isAppLockEnabled,
                    onCheckedChange = { appLockViewModel.setAppLockEnabled(it) },
                )
                SettingDivider()
                SettingRow(
                    title = "Lock when backgrounded",
                    subtitle = "Also require unlock when returning from another app",
                    checked = lockOnBackground,
                    enabled = isAppLockEnabled,
                    onCheckedChange = { appLockViewModel.setLockOnBackground(it) },
                )
            }

            SettingsSection(title = "Appearance", icon = Icons.Filled.Palette) {
                val themeMode by themeViewModel.themeMode.collectAsState()
                val themeStyle by themeViewModel.themeStyle.collectAsState()
                val colorPickerEnabled = themeStyle == AppThemeStyle.DEFAULT

                Text(
                    "Look",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "Chosen here. Doesn't follow your phone.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    AppThemeMode.entries.forEachIndexed { index, mode ->
                        SegmentedButton(
                            selected = themeMode == mode,
                            onClick = { themeViewModel.setThemeMode(mode) },
                            shape = SegmentedButtonDefaults.itemShape(
                                index = index,
                                count = AppThemeMode.entries.size,
                            ),
                            enabled = colorPickerEnabled,
                            icon = {
                                if (themeMode == mode) {
                                    Icon(
                                        Icons.Filled.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                    )
                                } else {
                                    SegmentedButtonDefaults.Icon(active = false)
                                }
                            },
                        ) {
                            Text(mode.label)
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))

                Text(
                    "Theme style",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                ThemeStylePicker(themeViewModel = themeViewModel)
                Spacer(Modifier.height(16.dp))

                Text(
                    "Accent color",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                ThemeColorPicker(
                    themeViewModel = themeViewModel,
                    enabled = colorPickerEnabled,
                )
            }

            SettingsSection(title = "Currency", icon = Icons.Filled.CurrencyExchange) {
                Text(
                    "Only changes the symbol shown — amounts aren't converted.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                CurrencyPicker(currencyViewModel)
            }

            SettingsSection(title = "Bottom tabs", icon = Icons.Filled.Dashboard) {
                SettingRow(
                    title = "Debts tab",
                    subtitle = "Show the Debts tab in the bottom bar",
                    checked = showDebtsTab,
                    onCheckedChange = { navTabsViewModel.setShowDebts(it) },
                )
                SettingDivider()
                SettingRow(
                    title = "Savings tab",
                    subtitle = "Show the Savings tab in the bottom bar",
                    checked = showSavingsTab,
                    onCheckedChange = { navTabsViewModel.setShowSavings(it) },
                )
            }

            SettingsSection(title = "Customize Home", icon = Icons.Filled.Dashboard) {
                HomeSection.entries.forEachIndexed { index, section ->
                    val visible by homeSectionsViewModel.visibility.getValue(section).collectAsState()
                    SettingRow(
                        title = section.label,
                        subtitle = "Show this section on the Home screen",
                        checked = visible,
                        onCheckedChange = { homeSectionsViewModel.setVisible(section, it) },
                    )
                    if (index != HomeSection.entries.lastIndex) SettingDivider()
                }
            }

            SettingsSection(title = "Backup & Restore", icon = Icons.Filled.Save) {
                SettingRow(
                    title = "Auto backup",
                    subtitle = autoBackupSubtitle(isAutoBackupEnabled, lastBackupMillis),
                    checked = isAutoBackupEnabled,
                    onCheckedChange = { enabled ->
                        if (enabled) {
                            autoBackupFolderLauncher.launch(downloadsTreeHintUri())
                        } else {
                            backupViewModel.disableAutoBackup()
                        }
                    },
                    enabled = !isBackupWorking,
                )
                if (isAutoBackupEnabled) {
                    SettingDivider()
                    ActionRow(
                        title = "Backup folder",
                        subtitle = "Choose a different folder for the auto-backup file",
                        buttonLabel = "Change",
                        enabled = !isBackupWorking,
                        onClick = { autoBackupFolderLauncher.launch(downloadsTreeHintUri()) },
                    )
                }
                SettingDivider()
                ActionRow(
                    title = "Export data",
                    subtitle = "Save a backup file to your device",
                    buttonLabel = "Export",
                    enabled = !isBackupWorking,
                    onClick = {
                        val stamp = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                        exportLauncher.launch("moneymanager-backup-$stamp.json")
                    },
                )
                SettingDivider()
                ActionRow(
                    title = "Import data",
                    subtitle = "Restore from a backup file — replaces everything on this device",
                    buttonLabel = "Import",
                    enabled = !isBackupWorking,
                    onClick = { importLauncher.launch(arrayOf("application/json")) },
                )
                if (isBackupWorking) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp))
                    }
                }
            }

            SettingsSection(title = "Help", icon = Icons.Filled.Help) {
                ActionRow(
                    title = "Replay tutorial",
                    subtitle = "See the welcome walkthrough again",
                    buttonLabel = "Replay",
                    enabled = true,
                    onClick = onboardingViewModel::resetOnboarding,
                )
            }

            AppVersionFooter(modifier = Modifier.padding(top = 8.dp))
        }
    }

    pendingImportUri?.let { uri ->
        AlertDialog(
            onDismissRequest = { pendingImportUri = null },
            title = { Text("Replace all data?") },
            text = {
                Text(
                    "This will delete every EMI, payment, expense, and category currently on " +
                        "this device and replace them with the contents of the selected backup " +
                        "file. This can't be undone."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        backupViewModel.importFrom(uri)
                        pendingImportUri = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) { Text("Replace") }
            },
            dismissButton = {
                TextButton(onClick = { pendingImportUri = null }) { Text("Cancel") }
            },
        )
    }
}

private fun autoBackupSubtitle(enabled: Boolean, lastBackupMillis: Long?): String {
    if (!enabled) {
        return "Saves a JSON copy to a folder you choose. Survives uninstall."
    }
    if (lastBackupMillis == null || lastBackupMillis == 0L) {
        return "Updates moneymanager-auto-backup.json when you leave the app"
    }
    val stamp = SimpleDateFormat("d MMM yyyy, h:mm a", Locale.getDefault()).format(Date(lastBackupMillis))
    return "Last saved $stamp"
}

private fun downloadsTreeHintUri(): Uri? {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return null
    return Uri.parse("content://com.android.externalstorage.documents/document/primary%3ADownload")
}

@Composable
private fun SettingsSection(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit,
) {
    // macOS-style flat section card
    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp)
            )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp),
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Spacer(Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
private fun SettingDivider() {
    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
}

@Composable
private fun ThemeStylePicker(
    themeViewModel: ThemeViewModel,
) {
    val selected by themeViewModel.themeStyle.collectAsState()
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        AppThemeStyle.entries.forEachIndexed { index, style ->
            SegmentedButton(
                selected = style == selected,
                onClick = { themeViewModel.setThemeStyle(style) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = AppThemeStyle.entries.size),
                icon = {
                    if (style == selected) {
                        Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    } else {
                        SegmentedButtonDefaults.Icon(active = style == selected)
                    }
                },
            ) {
                Text(style.label)
            }
        }
    }
}

@Composable
private fun ThemeColorPicker(
    themeViewModel: ThemeViewModel,
    enabled: Boolean = true,
) {
    val selected by themeViewModel.themeColor.collectAsState()
    LazyRow(
        modifier = Modifier.alpha(if (enabled) 1f else 0.4f),
        contentPadding = PaddingValues(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(AppThemeColor.entries) { color ->
            ColorSwatch(
                color = color,
                isSelected = color == selected,
                enabled = enabled,
                onClick = { themeViewModel.setThemeColor(color) },
            )
        }
    }
}

@Composable
private fun CurrencyPicker(currencyViewModel: CurrencyViewModel) {
    val selected by currencyViewModel.currency.collectAsState()
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        CurrencyType.entries.forEach { type ->
            CurrencyChip(
                type = type,
                isSelected = type == selected,
                onClick = { currencyViewModel.setCurrency(type) },
            )
        }
    }
}

@Composable
private fun CurrencyChip(type: CurrencyType, isSelected: Boolean, onClick: () -> Unit) {
    val backgroundColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
    val contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        modifier = Modifier
            .background(backgroundColor, MaterialTheme.shapes.small)
            .defaultMinSize(minHeight = 48.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("${type.symbol}  ${type.label}", color = contentColor, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun ColorSwatch(
    color: AppThemeColor,
    isSelected: Boolean,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(color.seed, CircleShape)
                .then(
                    if (isSelected) {
                        Modifier.border(2.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                    } else {
                        Modifier
                    }
                )
                .clickable(enabled = enabled, onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            if (isSelected) {
                val mark = if (color.seed.luminance() > 0.62f) Color.Black else Color.White
                Icon(Icons.Filled.Check, contentDescription = color.label, tint = mark)
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            color.label,
            style = MaterialTheme.typography.labelSmall,
            color = if (isSelected) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
    }
}

@Composable
private fun SettingRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 56.dp)
            .clickable(enabled = enabled, role = Role.Switch) { onCheckedChange(!checked) }
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = checked, onCheckedChange = null, enabled = enabled)
    }
}

@Composable
private fun ActionRow(
    title: String,
    subtitle: String,
    buttonLabel: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 56.dp)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        OutlinedButton(onClick = onClick, enabled = enabled) { Text(buttonLabel) }
    }
}
