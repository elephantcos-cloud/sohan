package com.shohan.sohan.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.shohan.sohan.BuildConfig
import com.shohan.sohan.data.PermissionHelper
import com.shohan.sohan.data.ThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    themeMode: ThemeMode,
    hasUsageAccess: Boolean,
    onThemeChange: (ThemeMode) -> Unit
) {
    val context = LocalContext.current
    var showThemeDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Settings") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            // ── Appearance section ────────────────────────────────────────
            SectionHeader("Appearance")

            SettingsCard(
                icon  = Icons.Filled.Palette,
                title = "Theme",
                subtitle = when (themeMode) {
                    ThemeMode.SYSTEM -> "Follow system"
                    ThemeMode.LIGHT  -> "Light"
                    ThemeMode.DARK   -> "Dark"
                },
                onClick = { showThemeDialog = true }
            )

            // ── Permissions section ───────────────────────────────────────
            SectionHeader("Permissions")

            SettingsCard(
                icon = if (hasUsageAccess) Icons.Filled.CheckCircle else Icons.Filled.Warning,
                iconTint = if (hasUsageAccess) MaterialTheme.colorScheme.primary
                           else MaterialTheme.colorScheme.error,
                title = "Usage Access",
                subtitle = if (hasUsageAccess)
                    "Granted — cache sizes are visible"
                else
                    "Not granted — tap to open settings",
                onClick = {
                    if (!hasUsageAccess)
                        PermissionHelper.openUsageAccessSettings(context)
                }
            )

            SettingsCard(
                icon     = Icons.Filled.DeveloperMode,
                title    = "Wireless Debugging",
                subtitle = "Open Developer Options",
                onClick  = { PermissionHelper.openDeveloperOptions(context) }
            )

            // ── About section ─────────────────────────────────────────────
            SectionHeader("About")

            SettingsCard(
                icon     = Icons.Filled.Info,
                title    = "Version",
                subtitle = "Sohan ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                onClick  = {}
            )

            SettingsCard(
                icon     = Icons.Filled.Code,
                title    = "Source Code",
                subtitle = "github.com/elephantcos-cloud/sohan",
                onClick  = {
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW,
                            Uri.parse("https://github.com/elephantcos-cloud/sohan"))
                    )
                }
            )

            SettingsCard(
                icon     = Icons.Filled.Hub,
                title    = "AIDL Service Action",
                subtitle = "com.shohan.sohan.SERVICE",
                onClick  = {}
            )

            SettingsCard(
                icon     = Icons.Filled.Android,
                title    = "Package Name",
                subtitle = "com.shohan.sohan",
                onClick  = {}
            )

            Spacer(Modifier.height(16.dp))
        }
    }

    // ── Theme picker dialog ───────────────────────────────────────────────────
    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text("Choose Theme") },
            text  = {
                Column {
                    ThemeMode.entries.forEach { mode ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onThemeChange(mode)
                                    showThemeDialog = false
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = themeMode == mode,
                                onClick  = {
                                    onThemeChange(mode)
                                    showThemeDialog = false
                                }
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                when (mode) {
                                    ThemeMode.SYSTEM -> "Follow system"
                                    ThemeMode.LIGHT  -> "Light"
                                    ThemeMode.DARK   -> "Dark"
                                },
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 4.dp, top = 4.dp)
    )
}

@Composable
private fun SettingsCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    iconTint: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurfaceVariant,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Filled.ChevronRight, null,
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(18.dp))
        }
    }
}
