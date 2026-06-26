package com.shohan.sohan.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.shohan.sohan.data.AppInfo
import com.shohan.sohan.viewmodel.MainViewModel

enum class AppListFilter { ALL, USER, SYSTEM }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppListScreen(
    apps: List<AppInfo>,
    isLoading: Boolean,
    isAdbConnected: Boolean,
    bulkProgress: Int?,
    onClearCache: (String) -> Unit,
    onForceStop: (String) -> Unit,
    onClearAllCache: () -> Unit,
    onRefresh: () -> Unit
) {
    var filter by remember { mutableStateOf(AppListFilter.ALL) }
    var confirmDialog by remember { mutableStateOf<Pair<String, String>?>(null) }
    var showBulkConfirm by remember { mutableStateOf(false) }

    val filtered = remember(apps, filter) {
        when (filter) {
            AppListFilter.ALL    -> apps
            AppListFilter.USER   -> apps.filter { !it.isSystemApp }
            AppListFilter.SYSTEM -> apps.filter { it.isSystemApp }
        }
    }

    val totalCache = filtered.sumOf { it.cacheBytes }
    val isBulkRunning = bulkProgress != null

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("App Cache") },
                actions = {
                    IconButton(onClick = onRefresh, enabled = !isLoading && !isBulkRunning) {
                        Icon(Icons.Filled.Refresh, "Refresh")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            // ── Summary card ──────────────────────────────────────────────
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Reclaimable Cache", style = MaterialTheme.typography.labelMedium)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            MainViewModel.formatBytes(totalCache),
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            "${filtered.size} apps",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )

                        if (!isAdbConnected) {
                            Spacer(Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Warning, null,
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.error)
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    "ADB not connected — go to Connection tab first",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        // ── Bulk clear button / progress ──────────────────
                        if (isBulkRunning) {
                            Text(
                                "Clearing all cache… $bulkProgress%",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Spacer(Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = bulkProgress!! / 100f,
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            Button(
                                onClick = { showBulkConfirm = true },
                                enabled = isAdbConnected && filtered.isNotEmpty(),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Filled.CleaningServices, null,
                                    modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Clear All Cache (${filtered.size} apps)")
                            }
                        }
                    }
                }
            }

            // ── Filter chips ──────────────────────────────────────────────
            item {
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AppListFilter.entries.forEach { f ->
                        FilterChip(
                            selected = filter == f,
                            onClick  = { filter = f },
                            label = {
                                Text(when (f) {
                                    AppListFilter.ALL    -> "All"
                                    AppListFilter.USER   -> "User Apps"
                                    AppListFilter.SYSTEM -> "System Apps"
                                })
                            }
                        )
                    }
                }
            }

            // ── Loading / empty state ─────────────────────────────────────
            if (isLoading) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Spacer(Modifier.height(12.dp))
                            Text("Loading cache sizes…",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            } else if (filtered.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No cached data found.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                items(filtered, key = { it.packageName }) { app ->
                    AppCacheRow(
                        app          = app,
                        maxCache     = filtered.first().cacheBytes,
                        adbConnected = isAdbConnected && !isBulkRunning,
                        onClearCache = { confirmDialog = "cache" to app.packageName },
                        onForceStop  = { confirmDialog = "stop"  to app.packageName }
                    )
                }
            }
        }
    }

    // ── Bulk confirm dialog ───────────────────────────────────────────────────
    if (showBulkConfirm) {
        AlertDialog(
            onDismissRequest = { showBulkConfirm = false },
            icon  = { Icon(Icons.Filled.CleaningServices, null) },
            title = { Text("Clear All Cache?") },
            text  = {
                Text(
                    "This will clear cache of all ${filtered.size} apps " +
                    "(${MainViewModel.formatBytes(totalCache)}).\n\n" +
                    "Your app data and settings are NOT affected.\n" +
                    "This may take a moment."
                )
            },
            confirmButton = {
                Button(onClick = { onClearAllCache(); showBulkConfirm = false }) {
                    Text("Clear All")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showBulkConfirm = false }) { Text("Cancel") }
            }
        )
    }

    // ── Single app confirm dialog ─────────────────────────────────────────────
    confirmDialog?.let { (action, pkg) ->
        val isStop = action == "stop"
        AlertDialog(
            onDismissRequest = { confirmDialog = null },
            icon  = {
                Icon(
                    if (isStop) Icons.Filled.Stop else Icons.Filled.Delete,
                    null,
                    tint = if (isStop) MaterialTheme.colorScheme.error
                           else MaterialTheme.colorScheme.primary
                )
            },
            title = { Text(if (isStop) "Force Stop?" else "Clear Cache?") },
            text  = {
                Text(
                    if (isStop)
                        "Force stop $pkg?\n\nUnsaved data may be lost."
                    else
                        "Clear cache of $pkg?\n\nData and settings are safe."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (isStop) onForceStop(pkg) else onClearCache(pkg)
                        confirmDialog = null
                    },
                    colors = if (isStop)
                        ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    else ButtonDefaults.buttonColors()
                ) { Text(if (isStop) "Force Stop" else "Clear Cache") }
            },
            dismissButton = {
                OutlinedButton(onClick = { confirmDialog = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun AppCacheRow(
    app: AppInfo,
    maxCache: Long,
    adbConnected: Boolean,
    onClearCache: () -> Unit,
    onForceStop: () -> Unit
) {
    val barProgress by animateFloatAsState(
        targetValue = if (maxCache > 0) app.cacheBytes.toFloat() / maxCache else 0f,
        label = "cache_bar"
    )

    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(app.appName, style = MaterialTheme.typography.titleSmall,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(app.packageName, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Spacer(Modifier.width(8.dp))
                Text(MainViewModel.formatBytes(app.cacheBytes),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = barProgress,
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onClearCache, enabled = adbConnected,
                    modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.CleaningServices, null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Clear Cache", style = MaterialTheme.typography.labelSmall)
                }
                OutlinedButton(onClick = onForceStop, enabled = adbConnected,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error)) {
                    Icon(Icons.Filled.Stop, null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Force Stop", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}
