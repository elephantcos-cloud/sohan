package com.shohan.sohan.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.shohan.sohan.adb.AutoConnectHelper
import com.shohan.sohan.adb.ConnectionState

@Composable
fun ConnectScreen(
    connectionState: ConnectionState,
    onConnect: (host: String, port: Int) -> Unit,
    onRetryAutoConnect: () -> Unit
) {
    var host by remember { mutableStateOf("127.0.0.1") }
    var port by remember { mutableStateOf("") }
    var portError by remember { mutableStateOf(false) }
    var showManual by remember { mutableStateOf(false) }

    val isConnecting = connectionState is ConnectionState.Connecting
    val wirelessEnabled = AutoConnectHelper.isWirelessDebuggingEnabled()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(24.dp))

        Icon(
            Icons.Filled.Wifi,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Text("Sohan", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Wireless ADB Bridge",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(4.dp))

        // ── Auto-connect status card ──────────────────────────────────────────
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = when {
                    isConnecting -> MaterialTheme.colorScheme.secondaryContainer
                    connectionState is ConnectionState.Error && !wirelessEnabled ->
                        MaterialTheme.colorScheme.errorContainer
                    else -> MaterialTheme.colorScheme.surfaceVariant
                }
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                when {
                    isConnecting -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(10.dp))
                            Text("Auto-connecting…", style = MaterialTheme.typography.titleSmall)
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Sohan is detecting the Wireless ADB port automatically.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                    !wirelessEnabled -> {
                        Icon(Icons.Filled.WifiOff, null, modifier = Modifier.size(28.dp),
                            tint = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.height(6.dp))
                        Text("Wireless Debugging is off", style = MaterialTheme.typography.titleSmall)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Settings → Developer Options → Wireless debugging → turn ON",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(10.dp))
                        Button(onClick = onRetryAutoConnect, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Filled.Refresh, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Retry Auto-Connect")
                        }
                    }
                    connectionState is ConnectionState.Error -> {
                        Icon(Icons.Filled.ErrorOutline, null, modifier = Modifier.size(28.dp),
                            tint = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.height(6.dp))
                        Text("Auto-connect failed", style = MaterialTheme.typography.titleSmall)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            (connectionState as ConnectionState.Error).message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = onRetryAutoConnect,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Filled.Refresh, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Retry")
                            }
                            Button(
                                onClick = { showManual = !showManual },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Filled.Edit, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Manual")
                            }
                        }
                    }
                    else -> {
                        Icon(Icons.Filled.Info, null, modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Wireless Debugging is on. Tap Retry to auto-connect.",
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(10.dp))
                        Button(onClick = onRetryAutoConnect, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Filled.Link, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Auto-Connect")
                        }
                    }
                }
            }
        }

        // ── Manual connect (shown on demand or after auto-connect fails) ───────
        if (showManual || (!isConnecting && !wirelessEnabled)) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Manual connect", style = MaterialTheme.typography.titleSmall)

                    OutlinedTextField(
                        value = host,
                        onValueChange = { host = it },
                        label = { Text("Host") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                        enabled = !isConnecting
                    )

                    OutlinedTextField(
                        value = port,
                        onValueChange = { port = it.filter { c -> c.isDigit() }; portError = false },
                        label = { Text("Port") },
                        placeholder = { Text("e.g. 37891") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = portError,
                        supportingText = if (portError) { { Text("Enter a valid port 1–65535") } } else null,
                        enabled = !isConnecting
                    )

                    Button(
                        onClick = {
                            val p = port.toIntOrNull()
                            if (p == null || p !in 1..65535) portError = true
                            else onConnect(host.trim(), p)
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        enabled = !isConnecting
                    ) {
                        if (isConnecting) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary)
                            Spacer(Modifier.width(8.dp))
                            Text("Connecting…")
                        } else {
                            Icon(Icons.Filled.Link, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Connect")
                        }
                    }
                }
            }
        }

        // ── How-to note ───────────────────────────────────────────────────────
        if (!isConnecting) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("First time?", style = MaterialTheme.typography.labelMedium)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "1. Settings → About phone → tap Build number 7 times\n" +
                        "2. Settings → Developer Options → enable Wireless debugging\n" +
                        "3. Come back here and tap Auto-Connect\n" +
                        "4. A dialog will appear — tap Allow",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}
