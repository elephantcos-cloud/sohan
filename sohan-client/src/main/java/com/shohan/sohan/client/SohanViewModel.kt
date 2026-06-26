package com.shohan.sohan.client

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Optional base ViewModel for apps that use [SohanClient].
 *
 * Handles connection lifecycle and exposes ready-to-use StateFlows.
 * Extend this in your own ViewModel:
 *
 * ```kotlin
 * class MyViewModel(app: Application) : SohanViewModel(app) {
 *
 *     fun clearMyAppCache() = viewModelScope.launch {
 *         when (val r = shell("pm clear --cache-only com.myapp")) {
 *             is SohanResult.Success -> { /* done */ }
 *             is SohanResult.Error   -> { /* show error */ }
 *         }
 *     }
 * }
 * ```
 */
open class SohanViewModel(application: Application) : AndroidViewModel(application) {

    protected val sohan = SohanClient(application)

    private val _sohanState = MutableStateFlow<SohanResult<Int>?>(null)
    val sohanState: StateFlow<SohanResult<Int>?> = _sohanState.asStateFlow()

    init {
        viewModelScope.launch {
            val result = sohan.connect()
            _sohanState.value = result

            // Auto-request permission if not yet authorized
            if (result is SohanResult.Error &&
                result.error == SohanError.NOT_AUTHORIZED) {
                sohan.requestPermission()
            }
        }
    }

    // ── Convenience wrappers ──────────────────────────────────────────────────

    protected suspend fun shell(command: String): SohanResult<String> =
        sohan.shell(command)

    protected suspend fun clearCache(packageName: String): SohanResult<String> =
        sohan.clearCache(packageName)

    protected suspend fun forceStop(packageName: String): SohanResult<Unit> =
        sohan.forceStop(packageName)

    protected suspend fun uninstall(packageName: String): SohanResult<String> =
        sohan.uninstall(packageName)

    override fun onCleared() {
        super.onCleared()
        sohan.disconnect()
    }
}
