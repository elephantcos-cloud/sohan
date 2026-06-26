package com.shohan.sohan.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.connectionStore: DataStore<Preferences>
    by preferencesDataStore(name = "sohan_connection")

/**
 * Persists the last successfully used host/port so Sohan can attempt
 * auto-reconnect on the next launch even if SystemProperties fails.
 */
class ConnectionPreference(private val context: Context) {

    private val KEY_HOST = stringPreferencesKey("last_host")
    private val KEY_PORT = intPreferencesKey("last_port")

    val savedHost: Flow<String> = context.connectionStore.data
        .map { it[KEY_HOST] ?: "127.0.0.1" }

    val savedPort: Flow<Int> = context.connectionStore.data
        .map { it[KEY_PORT] ?: 0 }

    suspend fun save(host: String, port: Int) {
        context.connectionStore.edit {
            it[KEY_HOST] = host
            it[KEY_PORT] = port
        }
    }

    suspend fun clear() {
        context.connectionStore.edit {
            it.remove(KEY_HOST)
            it.remove(KEY_PORT)
        }
    }
}
