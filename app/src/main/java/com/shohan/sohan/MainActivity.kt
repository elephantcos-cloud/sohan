package com.shohan.sohan

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.shohan.sohan.adb.ConnectionState
import com.shohan.sohan.ui.SohanRoot
import com.shohan.sohan.ui.theme.SohanTheme
import com.shohan.sohan.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val themeMode by viewModel.themeMode.collectAsState()
            SohanTheme(themeMode = themeMode) {
                SohanRoot(viewModel = viewModel)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Re-check permissions in case user just granted Usage Access
        viewModel.refreshPermissions()
        viewModel.refreshServiceState()

        // Re-try auto-connect only if disconnected/errored
        val state = viewModel.connectionState.value
        if (state !is ConnectionState.Connected &&
            state !is ConnectionState.Connecting) {
            viewModel.autoConnect()
        }
    }
}
