package com.shohan.sohan.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shohan.sohan.adb.ConnectionState
import com.shohan.sohan.ui.theme.TerminalBg
import com.shohan.sohan.ui.theme.TerminalText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    connectionState: ConnectionState.Connected,
    shellOutput: List<String>,
    onRunShell: (String) -> Unit,
    onClearShell: () -> Unit,
    onDisconnect: () -> Unit
) {
    var command by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(shellOutput.size) {
        if (shellOutput.isNotEmpty()) listState.animateScrollToItem(shellOutput.size - 1)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Sohan", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "${connectionState.host}:${connectionState.port}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onClearShell) {
                        Icon(Icons.Filled.Delete, contentDescription = "Clear output")
                    }
                    IconButton(onClick = onDisconnect) {
                        Icon(Icons.Filled.LinkOff, contentDescription = "Disconnect")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // ── Connection status chip ────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.Circle,
                    contentDescription = null,
                    modifier = Modifier.size(10.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    "Connected via Wireless ADB",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // ── Quick-action chips ────────────────────────────────────────────
            Text(
                "Quick commands",
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 4.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val quickCmds = listOf(
                    "id" to "Who am I",
                    "pm list packages -3" to "User apps",
                    "dumpsys battery" to "Battery info",
                    "getprop ro.build.version.release" to "Android version",
                    "df /data" to "Storage"
                )
                quickCmds.forEach { (cmd, label) ->
                    SuggestionChip(
                        onClick = { onRunShell(cmd) },
                        label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // ── Terminal output ───────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .background(TerminalBg, MaterialTheme.shapes.medium)
            ) {
                if (shellOutput.isEmpty()) {
                    Text(
                        "Run a command below to see output.",
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp),
                        color = TerminalText.copy(alpha = 0.4f),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp
                    )
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        items(shellOutput) { line ->
                            Text(
                                line,
                                color = if (line.startsWith("$")) TerminalText else TerminalText.copy(alpha = 0.8f),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            // ── Command input bar ─────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = command,
                    onValueChange = { command = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Enter shell command…", fontSize = 13.sp) },
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = {
                        if (command.isNotBlank()) {
                            onRunShell(command.trim())
                            command = ""
                        }
                    })
                )
                IconButton(
                    onClick = {
                        if (command.isNotBlank()) {
                            onRunShell(command.trim())
                            command = ""
                        }
                    },
                    colors = IconButtonDefaults.filledIconButtonColors()
                ) {
                    Icon(Icons.Filled.Send, contentDescription = "Run")
                }
            }
        }
    }
}
