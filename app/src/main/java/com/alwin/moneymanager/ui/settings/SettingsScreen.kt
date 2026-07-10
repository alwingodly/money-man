package com.alwin.moneymanager.ui.settings

import android.app.Activity
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LocalCafe
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.alwin.moneymanager.data.repository.HomeSection
import com.alwin.moneymanager.ui.applock.AppLockViewModel
import com.alwin.moneymanager.ui.common.PaywallDialog
import com.alwin.moneymanager.ui.home.HomeSectionsViewModel
import com.alwin.moneymanager.ui.navigation.NavTabsViewModel
import com.alwin.moneymanager.ui.onboarding.OnboardingViewModel
import com.alwin.moneymanager.ui.theme.AppThemeColor
import com.alwin.moneymanager.ui.theme.AppThemeStyle
import com.alwin.moneymanager.ui.theme.FREE_THEME_COLORS
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
    var pendingImportUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var themePaywallMessage by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val activity = context as Activity
    val isPremium by themeViewModel.isPremium.collectAsState()

    LaunchedEffect(backupEvent) {
        val event = backupEvent ?: return@LaunchedEffect
        val message = when (event) {
            BackupEvent.ExportSucceeded -> "Backup saved"
            BackupEvent.ImportSucceeded -> "Data restored from backup"
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
                Text(
                    "Theme style",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                ThemeStylePicker(
                    themeViewModel = themeViewModel,
                    isPremium = isPremium,
                    onLockedStyleClick = {
                        themePaywallMessage = "Retro LCD is a premium look — grab me a ₹9 " +
                            "coffee and unlock it (plus every other color) for good."
                    },
                )
                Spacer(Modifier.height(16.dp))

                val themeStyle by themeViewModel.themeStyle.collectAsState()
                val colorPickerEnabled = themeStyle == AppThemeStyle.DEFAULT
                Text(
                    "Theme color",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                ThemeColorPicker(
                    themeViewModel = themeViewModel,
                    enabled = colorPickerEnabled,
                    isPremium = isPremium,
                    onLockedColorClick = {
                        themePaywallMessage = "That one's a premium color — grab me a ₹9 " +
                            "coffee and unlock the whole crazy palette (plus Retro LCD)."
                    },
                )
            }

            SettingsSection(title = "Premium", icon = Icons.Filled.LocalCafe) {
                if (isPremium) {
                    Text(
                        "You've unlocked premium — thank you for the coffee! ☕",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                } else {
                    ActionRow(
                        title = "Buy me a coffee",
                        subtitle = "Unlock unlimited categories, unlimited loans, every theme " +
                            "color, and Retro LCD",
                        buttonLabel = "₹9",
                        enabled = true,
                        onClick = { themeViewModel.purchasePremium(activity) },
                    )
                }
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

    themePaywallMessage?.let { message ->
        PaywallDialog(
            message = message,
            onDismiss = { themePaywallMessage = null },
            onUnlock = {
                themeViewModel.purchasePremium(activity)
                themePaywallMessage = null
            },
        )
    }
}

@Composable
private fun SettingsSection(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit,
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Spacer(Modifier.height(4.dp))
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
    isPremium: Boolean,
    onLockedStyleClick: () -> Unit,
) {
    val selected by themeViewModel.themeStyle.collectAsState()
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        AppThemeStyle.entries.forEachIndexed { index, style ->
            val locked = style == AppThemeStyle.RETRO_LCD && !isPremium
            SegmentedButton(
                selected = style == selected,
                onClick = { if (locked) onLockedStyleClick() else themeViewModel.setThemeStyle(style) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = AppThemeStyle.entries.size),
                icon = {
                    if (locked) {
                        Icon(Icons.Filled.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
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
    isPremium: Boolean,
    onLockedColorClick: () -> Unit,
) {
    val selected by themeViewModel.themeColor.collectAsState()
    LazyRow(
        modifier = Modifier.alpha(if (enabled) 1f else 0.4f),
        contentPadding = PaddingValues(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(AppThemeColor.entries) { color ->
            val locked = color !in FREE_THEME_COLORS && !isPremium
            ColorSwatch(
                color = color,
                isSelected = color == selected,
                enabled = enabled,
                locked = locked,
                onClick = { if (locked) onLockedColorClick() else themeViewModel.setThemeColor(color) },
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
            .clickable(onClick = onClick)
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
    locked: Boolean = false,
) {
    Box(
        modifier = Modifier
            .size(40.dp)
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
        when {
            locked -> Icon(Icons.Filled.Lock, contentDescription = "${color.label} — premium", tint = Color.White, modifier = Modifier.size(18.dp))
            isSelected -> Icon(Icons.Filled.Check, contentDescription = color.label, tint = Color.White)
        }
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
        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
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
        Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
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
        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
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
