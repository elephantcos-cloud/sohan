package com.shohan.sohan.viewmodel

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.shohan.sohan.adb.AdbManager
import com.shohan.sohan.adb.AutoConnectHelper
import com.shohan.sohan.adb.ConnectionState
import com.shohan.sohan.data.AppInfo
import com.shohan.sohan.data.AppListRepository
import com.shohan.sohan.data.ConnectionPreference
import com.shohan.sohan.data.PermissionHelper
import com.shohan.sohan.data.ThemeMode
import com.shohan.sohan.data.ThemePreference
import com.shohan.sohan.service.AppPermissionManager
import com.shohan.sohan.service.PrivilegedActions
import com.shohan.sohan.service.SohanService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val connectionPref = ConnectionPreference(application)
    private val appListRepo    = AppListRepository(application)
    private val themePref      = ThemePreference(application)

    // ── Theme ─────────────────────────────────────────────────────────────────

    private val _themeMode = MutableStateFlow(ThemeMode.SYSTEM)
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    // ── Permissions ───────────────────────────────────────────────────────────

    private val _hasUsageAccess = MutableStateFlow(false)
    val hasUsageAccess: StateFlow<Boolean> = _hasUsageAccess.asStateFlow()

    // True once both usage access and ADB are ready — skips SetupScreen
    private val _setupDone = MutableStateFlow(false)
    val setupDone: StateFlow<Boolean> = _setupDone.asStateFlow()

    // ── Connection ────────────────────────────────────────────────────────────

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    // ── Service ───────────────────────────────────────────────────────────────

    private val _serviceRunning = MutableStateFlow(SohanService.isRunning)
    val serviceRunning: StateFlow<Boolean> = _serviceRunning.asStateFlow()

    // ── App list ──────────────────────────────────────────────────────────────

    private val _appList = MutableStateFlow<List<AppInfo>>(emptyList())
    val appList: StateFlow<List<AppInfo>> = _appList.asStateFlow()

    private val _appListLoading = MutableStateFlow(false)
    val appListLoading: StateFlow<Boolean> = _appListLoading.asStateFlow()

    private val _bulkProgress = MutableStateFlow<Int?>(null)
    val bulkProgress: StateFlow<Int?> = _bulkProgress.asStateFlow()

    // ── Authorized apps ───────────────────────────────────────────────────────

    private val _authorizedApps = MutableStateFlow<List<String>>(emptyList())
    val authorizedApps: StateFlow<List<String>> = _authorizedApps.asStateFlow()

    // ── Permission request from external app ──────────────────────────────────

    private val _pendingPermission = MutableStateFlow<String?>(null)
    val pendingPermission: StateFlow<String?> = _pendingPermission.asStateFlow()

    // ── Shell ─────────────────────────────────────────────────────────────────

    private val _shellOutput = MutableStateFlow<List<String>>(emptyList())
    val shellOutput: StateFlow<List<String>> = _shellOutput.asStateFlow()

    // ── Snackbar ──────────────────────────────────────────────────────────────

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()
    fun clearMessage() { _message.value = null }

    private var keepAliveJob: Job? = null

    // ─────────────────────────────────────────────────────────────────────────

    init {
        viewModelScope.launch {
            themePref.themeMode.collect { _themeMode.value = it }
        }
        refreshPermissions()
        refreshAuthorizedApps()
        autoConnect()
        observePendingPermissions()
    }

    // ── Setup ─────────────────────────────────────────────────────────────────

    fun refreshPermissions() {
        val ctx = getApplication<Application>()
        val usageOk = PermissionHelper.hasUsageAccess(ctx)
        _hasUsageAccess.value = usageOk
        updateSetupDone()
    }

    private fun updateSetupDone() {
        _setupDone.value = _hasUsageAccess.value &&
            _connectionState.value is ConnectionState.Connected
    }

    fun markSetupDone() { _setupDone.value = true }

    // ── Theme ─────────────────────────────────────────────────────────────────

    fun setTheme(mode: ThemeMode) {
        viewModelScope.launch { themePref.set(mode) }
    }

    // ── Auto-connect ──────────────────────────────────────────────────────────

    fun autoConnect() {
        if (_connectionState.value is ConnectionState.Connecting) return
        viewModelScope.launch {
            _connectionState.value = ConnectionState.Connecting

            val livePort = AutoConnectHelper.getAdbPort()
            if (livePort > 0 && connectInternal(AutoConnectHelper.HOST, livePort)) return@launch

            val savedPort = connectionPref.savedPort.first()
            val savedHost = connectionPref.savedHost.first()
            if (savedPort > 0 && connectInternal(savedHost, savedPort)) return@launch

            _connectionState.value = ConnectionState.Error(
                if (!AutoConnectHelper.isWirelessDebuggingEnabled())
                    "Wireless Debugging is off.\nSettings → Developer Options → Wireless debugging → ON"
                else
                    "Auto-connect failed. Try the Manual option."
            )
            updateSetupDone()
        }
    }

    fun connect(host: String, port: Int) {
        if (_connectionState.value is ConnectionState.Connecting) return
        viewModelScope.launch {
            _connectionState.value = ConnectionState.Connecting
            if (!connectInternal(host, port)) {
                _connectionState.value = ConnectionState.Error(
                    "Could not connect to $host:$port."
                )
                updateSetupDone()
            }
        }
    }

    private suspend fun connectInternal(host: String, port: Int): Boolean {
        val result = AdbManager.connect(host, port, getApplication<Application>())
        return if (result.isSuccess) {
            _connectionState.value = ConnectionState.Connected(host, port)
            connectionPref.save(host, port)
            if (SohanService.isRunning) nudgeService()
            loadAppList()
            startKeepAlive(host, port)
            updateSetupDone()
            true
        } else false
    }

    fun disconnect() {
        keepAliveJob?.cancel()
        AdbManager.disconnect()
        _connectionState.value = ConnectionState.Disconnected
        _shellOutput.value = emptyList()
        updateSetupDone()
        if (SohanService.isRunning) nudgeService()
    }

    // ── Keepalive ─────────────────────────────────────────────────────────────

    private fun startKeepAlive(host: String, port: Int) {
        keepAliveJob?.cancel()
        keepAliveJob = viewModelScope.launch {
            while (isActive) {
                delay(30_000)
                if (!AdbManager.isConnected) {
                    _message.value = "ADB session lost — reconnecting…"
                    connectInternal(host, port)
                } else {
                    AdbManager.shell("echo ping")
                }
            }
        }
    }

    // ── App list ──────────────────────────────────────────────────────────────

    fun loadAppList() {
        if (_appListLoading.value) return
        viewModelScope.launch {
            _appListLoading.value = true
            _appList.value = appListRepo.loadApps()
            _appListLoading.value = false
        }
    }

    fun clearAllCache() {
        val list = _appList.value
        if (list.isEmpty()) return
        viewModelScope.launch {
            _bulkProgress.value = 0
            var done = 0; var success = 0
            list.forEach { app ->
                if (PrivilegedActions.clearCache(app.packageName).isSuccess) success++
                done++
                _bulkProgress.value = done * 100 / list.size
            }
            _bulkProgress.value = null
            _message.value = "Cleared cache of $success / ${list.size} apps"
            loadAppList()
        }
    }

    fun clearCache(packageName: String) {
        viewModelScope.launch {
            val r = PrivilegedActions.clearCache(packageName)
            _message.value = if (r.isSuccess) "Cache cleared: $packageName"
                             else "Failed: ${r.exceptionOrNull()?.message}"
            if (r.isSuccess) loadAppList()
        }
    }

    fun forceStop(packageName: String) {
        viewModelScope.launch {
            val r = PrivilegedActions.forceStop(packageName)
            _message.value = if (r.isSuccess) "Force stopped: $packageName"
                             else "Failed: ${r.exceptionOrNull()?.message}"
            if (r.isSuccess) loadAppList()
        }
    }

    // ── Shell ─────────────────────────────────────────────────────────────────

    fun runShell(command: String) {
        viewModelScope.launch {
            val result = AdbManager.shell(command)
            if (result.isSuccess) {
                append("$ $command", result.getOrDefault("").trimEnd())
            } else {
                append("$ $command", "Error: ${result.exceptionOrNull()?.message}")
                if (!AdbManager.isConnected) autoConnect()
            }
        }
    }

    fun clearShell() { _shellOutput.value = emptyList() }

    private fun append(vararg lines: String) {
        _shellOutput.value = _shellOutput.value + lines.toList()
    }

    // ── Service ───────────────────────────────────────────────────────────────

    fun startService() {
        getApplication<Application>().startForegroundService(
            Intent(getApplication(), SohanService::class.java)
        )
        _serviceRunning.value = true
    }

    fun stopService() {
        getApplication<Application>().startService(
            Intent(getApplication(), SohanService::class.java)
                .apply { action = SohanService.ACTION_STOP }
        )
        _serviceRunning.value = false
    }

    fun refreshServiceState() { _serviceRunning.value = SohanService.isRunning }

    // ── Permissions ───────────────────────────────────────────────────────────

    fun refreshAuthorizedApps() {
        _authorizedApps.value = AppPermissionManager.getAuthorizedPackages(getApplication())
    }

    fun grantPermission(packageName: String) {
        AppPermissionManager.grant(getApplication(), packageName)
        refreshAuthorizedApps()
        _message.value = "Granted: $packageName"
    }

    fun revokePermission(packageName: String) {
        AppPermissionManager.revoke(getApplication(), packageName)
        refreshAuthorizedApps()
        _message.value = "Revoked: $packageName"
    }

    fun allowPendingPermission() {
        _pendingPermission.value?.let { grantPermission(it) }
        _pendingPermission.value = null
        SohanService.clearPendingPermission()
    }

    fun denyPendingPermission() {
        _pendingPermission.value = null
        SohanService.clearPendingPermission()
    }

    private fun observePendingPermissions() {
        viewModelScope.launch {
            SohanService.pendingPermission.collect { _pendingPermission.value = it }
        }
    }

    private fun nudgeService() {
        getApplication<Application>().startService(
            Intent(getApplication(), SohanService::class.java)
        )
    }

    override fun onCleared() {
        super.onCleared()
        keepAliveJob?.cancel()
        if (!SohanService.isRunning) AdbManager.disconnect()
    }

    companion object {
        fun formatBytes(bytes: Long): String {
            if (bytes <= 0) return "0 B"
            val units = arrayOf("B", "KB", "MB", "GB", "TB")
            var value = bytes.toDouble(); var i = 0
            while (value >= 1024 && i < units.size - 1) { value /= 1024; i++ }
            return String.format("%.1f %s", value, units[i])
        }
    }
}
