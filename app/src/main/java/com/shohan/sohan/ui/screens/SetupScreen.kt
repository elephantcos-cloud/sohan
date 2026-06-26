package com.shohan.sohan.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Shown on first launch (or when a required permission is missing).
 * Walks the user through every setup step in order.
 */
@Composable
fun SetupScreen(
    hasUsageAccess: Boolean,
    isAdbConnected: Boolean,
    onOpenUsageAccess: () -> Unit,
    onOpenDeveloperOptions: () -> Unit,
    onRetryConnect: () -> Unit,
    onDone: () -> Unit
) {
    val allDone = hasUsageAccess && isAdbConnected

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(16.dp))

        Icon(
            Icons.Filled.Wifi,
            contentDescription = null,
            modifier = Modifier.size(56.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Text("Sohan Setup", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Complete these steps once to get started.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(8.dp))

        // ── Step 1 — Usage Access ─────────────────────────────────────────
        SetupStep(
            number   = 1,
            title    = "Grant Usage Access",
            done     = hasUsageAccess,
            doneText = "Granted",
            todoText = "Needed to read per-app cache sizes. Tap below, find Sohan in the list and toggle it ON.",
            actionLabel = "Open Usage Access Settings",
            onAction = onOpenUsageAccess
        )

        // ── Step 2 — Wireless Debugging ───────────────────────────────────
        SetupStep(
            number   = 2,
            title    = "Enable Wireless Debugging",
            done     = isAdbConnected,
            doneText = "Connected",
            todoText = "Settings → Developer Options → Wireless debugging → turn ON.\n" +
                       "If you don't see Developer Options: Settings → About phone → tap Build number 7 times.",
            actionLabel = "Open Developer Options",
            onAction = onOpenDeveloperOptions
        )

        // ── Step 3 — ADB Connection ───────────────────────────────────────
        SetupStep(
            number   = 3,
            title    = "Allow ADB Debugging",
            done     = isAdbConnected,
            doneText = "ADB Connected",
            todoText = "After enabling Wireless Debugging, come back here and tap Retry. " +
                       "A dialog will appear — tap Allow.",
            actionLabel = "Retry Auto-Connect",
            onAction = onRetryConnect
        )

        Spacer(Modifier.height(8.dp))

        // ── Done button ───────────────────────────────────────────────────
        if (allDone) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Filled.CheckCircle, null,
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(8.dp))
                    Text("All done! Sohan is ready.",
                        style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Filled.ArrowForward, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Go to App")
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun SetupStep(
    number: Int,
    title: String,
    done: Boolean,
    doneText: String,
    todoText: String,
    actionLabel: String,
    onAction: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (done)
                MaterialTheme.colorScheme.secondaryContainer
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Step number circle
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = if (done) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(28.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (done) {
                            Icon(Icons.Filled.Check, null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onPrimary)
                        } else {
                            Text("$number",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimary)
                        }
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(title, style = MaterialTheme.typography.titleSmall)
                    if (done) {
                        Text(doneText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            if (!done) {
                Spacer(Modifier.height(10.dp))
                Text(todoText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = onAction,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(actionLabel)
                }
            }
        }
    }
}
