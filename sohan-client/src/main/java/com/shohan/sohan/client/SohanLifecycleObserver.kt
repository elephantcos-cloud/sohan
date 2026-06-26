package com.shohan.sohan.client

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Lifecycle-aware wrapper around [SohanClient].
 *
 * Automatically connects on [onStart] and disconnects on [onStop],
 * so you never have to manage the binding manually.
 *
 * ## Usage (in an Activity or Fragment)
 *
 * ```kotlin
 * class MyActivity : AppCompatActivity() {
 *
 *     private lateinit var sohan: SohanLifecycleObserver
 *
 *     override fun onCreate(savedInstanceState: Bundle?) {
 *         super.onCreate(savedInstanceState)
 *
 *         sohan = SohanLifecycleObserver(this) { result ->
 *             // Called when Sohan is ready (or failed to connect)
 *             when (result) {
 *                 is SohanResult.Success -> {
 *                     // Sohan is connected — start using it
 *                     lifecycleScope.launch {
 *                         val out = sohan.client.shell("id")
 *                         Log.d("Sohan", out.getOrNull() ?: "error")
 *                     }
 *                 }
 *                 is SohanResult.Error -> {
 *                     if (result.error == SohanError.NOT_AUTHORIZED) {
 *                         sohan.client.requestPermission()
 *                     }
 *                 }
 *             }
 *         }
 *         lifecycle.addObserver(sohan)
 *     }
 * }
 * ```
 */
class SohanLifecycleObserver(
    private val lifecycleOwner: LifecycleOwner,
    private val onConnected: (SohanResult<Int>) -> Unit
) : DefaultLifecycleObserver {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    /** Direct access to the underlying client after [onStart] is called. */
    lateinit var client: SohanClient
        private set

    override fun onStart(owner: LifecycleOwner) {
        client = SohanClient(owner as android.content.Context)
        scope.launch {
            val result = client.connect()

            // If not authorized, request permission automatically
            if (result is SohanResult.Error &&
                result.error == SohanError.NOT_AUTHORIZED) {
                client.requestPermission()
            }

            onConnected(result)
        }
    }

    override fun onStop(owner: LifecycleOwner) {
        if (::client.isInitialized) client.disconnect()
    }

    override fun onDestroy(owner: LifecycleOwner) {
        scope.cancel()
    }
}
