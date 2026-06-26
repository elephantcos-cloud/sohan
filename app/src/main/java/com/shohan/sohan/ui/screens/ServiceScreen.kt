package com.shohan.sohan.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServiceScreen(
    serviceRunning: Boolean,
    adbConnected: Boolean,
    authorizedApps: List<String>,
    onStartService: () -> Unit,
    onStopService: () -> Unit,
    onGrantPermission: (String) -> Unit,
    onRevokePermission: (String) -> Unit
) {
    var addPackageText by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        // ── Service status card ───────────────────────────────────────────────
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (serviceRunning)
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (serviceRunning) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                            contentDescription = null,
                            tint = if (serviceRunning)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (serviceRunning) "Service Running" else "Service Stopped",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        if (serviceRunning)
                            "SohanService is active in the background. Other apps can bind and use the ADB bridge."
                        else
                            "Start the service to allow other apps to use shell-level commands via Sohan.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.Circle,
                            contentDescription = null,
                            modifier = Modifier.size(8.dp),
                            tint = if (adbConnected)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.error
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "ADB: ${if (adbConnected) "Connected" else "Disconnected"}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = { if (serviceRunning) onStopService() else onStartService() },
                        colors = if (serviceRunning)
                            ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        else
                            ButtonDefaults.buttonColors(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            if (serviceRunning) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                            contentDescription = null
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(if (serviceRunning) "Stop Service" else "Start Service")
                    }
                }
            }
        }

        // ── How to use from other apps ────────────────────────────────────────
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Use from other apps", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "1. Copy ISohanService.aidl into your project\n" +
                        "2. Bind to SohanService:\n" +
                        "   intent.setPackage(\"com.shohan.sohan\")\n" +
                        "   action = \"com.shohan.sohan.SERVICE\"\n" +
                        "3. Cast: ISohanService.Stub.asInterface(binder)\n" +
                        "4. Call: service.shell(\"your command\")\n" +
                        "5. Add your package name below to authorize it",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // ── Authorized apps section ───────────────────────────────────────────
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Authorized Apps (${authorizedApps.size})", style = MaterialTheme.typography.titleSmall)
                IconButton(onClick = { showAddDialog = true }) {
                    Icon(Icons.Filled.AddCircle, contentDescription = "Add app")
                }
            }
        }

        if (authorizedApps.isEmpty()) {
            item {
                Text(
                    "No apps authorized yet. Tap + to add a package name.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
        } else {
            items(authorizedApps, key = { it }) { pkg ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.Android,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            pkg,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { onRevokePermission(pkg) }) {
                            Icon(
                                Icons.Filled.DeleteOutline,
                                contentDescription = "Revoke",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }

    // ── Add package dialog ────────────────────────────────────────────────────
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false; addPackageText = "" },
            title = { Text("Authorize App") },
            text = {
                Column {
                    Text(
                        "Enter the package name of the app you want to authorize.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = addPackageText,
                        onValueChange = { addPackageText = it },
                        label = { Text("Package name") },
                        placeholder = { Text("com.example.myapp") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = {
                            if (addPackageText.isNotBlank()) {
                                onGrantPermission(addPackageText.trim())
                                showAddDialog = false
                                addPackageText = ""
                            }
                        })
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (addPackageText.isNotBlank()) {
                            onGrantPermission(addPackageText.trim())
                            showAddDialog = false
                            addPackageText = ""
                        }
                    }
                ) { Text("Grant") }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false; addPackageText = "" }) {
                    Text("Cancel")
                }
            }
        )
    }
}
