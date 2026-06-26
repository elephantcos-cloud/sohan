package com.shohan.sohan.data

import android.app.usage.StorageStatsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.storage.StorageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AppListRepository(private val context: Context) {

    /**
     * Loads every installed app's cache size using [StorageStatsManager].
     * Requires Usage Access permission (PACKAGE_USAGE_STATS).
     * Returns list sorted by cache size, largest first, zeros excluded.
     */
    suspend fun loadApps(): List<AppInfo> = withContext(Dispatchers.IO) {
        val pm      = context.packageManager
        val ssm     = context.getSystemService(StorageStatsManager::class.java)
        val sm      = context.getSystemService(StorageManager::class.java)
        val results = mutableListOf<AppInfo>()

        try {
            val uuid = sm.getUuidForPath(context.dataDir)
            pm.getInstalledApplications(0).forEach { app ->
                try {
                    val stats = ssm.queryStatsForUid(uuid, app.uid)
                    results += AppInfo(
                        packageName = app.packageName,
                        appName     = pm.getApplicationLabel(app).toString(),
                        cacheBytes  = stats.cacheBytes,
                        isSystemApp = (app.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                    )
                } catch (_: Exception) {}
            }
        } catch (_: Exception) {}

        results
            .filter { it.cacheBytes > 0 }
            .sortedByDescending { it.cacheBytes }
    }
}
