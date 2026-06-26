package com.shohan.sohan.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.shohan.sohan.adb.AdbManager
import com.shohan.sohan.adb.ConnectionState
import com.shohan.sohan.data.PermissionHelper
import com.shohan.sohan.ui.screens.*
import com.shohan.sohan.viewmodel.MainViewModel
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalContext

@Composable
fun SohanRoot(viewModel: MainViewModel) {
    val context           = LocalContext.current
    val setupDone         by viewModel.setupDone.collectAsState()
    val connectionState   by viewModel.connectionState.collectAsState()
    val serviceRunning    by viewModel.serviceRunning.collectAsState()
    val shellOutput       by viewModel.shellOutput.collectAsState()
    val authorizedApps    by viewModel.authorizedApps.collectAsState()
    val appList           by viewModel.appList.collectAsState()
    val appListLoading    by viewModel.appListLoading.collectAsState()
    val bulkProgress      by viewModel.bulkProgress.collectAsState()
    val hasUsageAccess    by viewModel.hasUsageAccess.collectAsState()
    val themeMode         by viewModel.themeMode.collectAsState()
    val message           by viewModel.message.collectAsState()
    val pendingPermission by viewModel.pendingPermission.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope             = rememberCoroutineScope()
    var selectedTab       by remember { mutableStateOf(0) }

    LaunchedEffect(message) {
        message?.let {
            scope.launch { snackbarHostState.showSnackbar(it) }
            viewModel.clearMessage()
        }
    }

    // ── First-launch / permission setup screen ────────────────────────────────
    if (!setupDone) {
        SetupScreen(
            hasUsageAccess       = hasUsageAccess,
            isAdbConnected       = connectionState is ConnectionState.Connected,
            onOpenUsageAccess    = { PermissionHelper.openUsageAccessSettings(context) },
            onOpenDeveloperOptions = { PermissionHelper.openDeveloperOptions(context) },
            onRetryConnect       = viewModel::autoConnect,
            onDone               = viewModel::markSetupDone
        )
        return
    }

    // ── Main app (4 tabs) ─────────────────────────────────────────────────────
    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick  = { selectedTab = 0 },
                    icon     = { Icon(Icons.Filled.Terminal, null) },
                    label    = { Text("Connection") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick  = {
                        selectedTab = 1
                        if (appList.isEmpty()) viewModel.loadAppList()
                    },
                    icon     = { Icon(Icons.Filled.Apps, null) },
                    label    = { Text("Apps") }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick  = {
                        selectedTab = 2
                        viewModel.refreshServiceState()
                        viewModel.refreshAuthorizedApps()
                    },
                    icon     = { Icon(Icons.Filled.Hub, null) },
                    label    = { Text("Service") }
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick  = {
                        selectedTab = 3
                        viewModel.refreshPermissions()
                    },
                    icon     = { Icon(Icons.Filled.Settings, null) },
                    label    = { Text("Settings") }
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (selectedTab) {
                0 -> when (val state = connectionState) {
                    is ConnectionState.Connected -> HomeScreen(
                        connectionState = state,
                        shellOutput     = shellOutput,
                        onRunShell      = viewModel::runShell,
                        onClearShell    = viewModel::clearShell,
                        onDisconnect    = viewModel::disconnect
                    )
                    else -> ConnectScreen(
                        connectionState    = connectionState,
                        onConnect          = { h, p -> viewModel.connect(h, p) },
                        onRetryAutoConnect = viewModel::autoConnect
                    )
                }
                1 -> AppListScreen(
                    apps            = appList,
                    isLoading       = appListLoading,
                    isAdbConnected  = AdbManager.isConnected,
                    bulkProgress    = bulkProgress,
                    onClearCache    = viewModel::clearCache,
                    onForceStop     = viewModel::forceStop,
                    onClearAllCache = viewModel::clearAllCache,
                    onRefresh       = viewModel::loadAppList
                )
                2 -> ServiceScreen(
                    serviceRunning     = serviceRunning,
                    adbConnected       = AdbManager.isConnected,
                    authorizedApps     = authorizedApps,
                    onStartService     = viewModel::startService,
                    onStopService      = viewModel::stopService,
                    onGrantPermission  = viewModel::grantPermission,
                    onRevokePermission = viewModel::revokePermission
                )
                3 -> SettingsScreen(
                    themeMode      = themeMode,
                    hasUsageAccess = hasUsageAccess,
                    onThemeChange  = viewModel::setTheme
                )
            }
        }
    }

    // ── Permission dialog (over any tab) ──────────────────────────────────────
    pendingPermission?.let { pkg ->
        AlertDialog(
            onDismissRequest = { viewModel.denyPendingPermission() },
            icon  = { Icon(Icons.Filled.Hub, null) },
            title = { Text("Permission Request") },
            text  = {
                Text(
                    "An app wants to use Sohan's ADB bridge:\n\n" +
                    "  $pkg\n\n" +
                    "Allow it to run shell commands with ADB-level permissions?\n\n" +
                    "Only allow apps you trust."
                )
            },
            confirmButton = {
                Button(onClick = { viewModel.allowPendingPermission() }) { Text("Allow") }
            },
            dismissButton = {
                OutlinedButton(onClick = { viewModel.denyPendingPermission() }) { Text("Deny") }
            }
        )
    }
}
