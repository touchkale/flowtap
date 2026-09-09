package com.flowtap.app.ui.modes

import android.content.Context
import android.content.Intent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Shared app catalog: loaded off the main thread, deduplicated by package,
 * deterministically sorted. Previously each editor entry re-queried
 * PackageManager on the UI thread and mapped launcher *activities* without
 * deduping, which could produce duplicate Compose keys (M3).
 */
object AppCatalog {

    /** Pure: dedupe by package, keep first label, sort by label then package. */
    fun deduplicate(apps: List<InstalledApp>): List<InstalledApp> {
        val byPkg = LinkedHashMap<String, InstalledApp>()
        for (app in apps) {
            byPkg.putIfAbsent(app.pkg, app)
        }
        return byPkg.values.sortedWith(compareBy({ it.label.lowercase() }, { it.pkg }))
    }

    fun filter(apps: List<InstalledApp>, query: String): List<InstalledApp> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return apps
        return apps.filter {
            it.label.lowercase().contains(q) || it.pkg.lowercase().contains(q)
        }
    }

    suspend fun load(context: Context): List<InstalledApp> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val raw = runCatching { pm.queryIntentActivities(intent, 0) }.getOrDefault(emptyList())
        val mapped = raw.mapNotNull { ri ->
            val pkg = runCatching { ri.activityInfo.packageName }.getOrNull() ?: return@mapNotNull null
            if (pkg == context.packageName) return@mapNotNull null
            val label = runCatching { ri.loadLabel(pm).toString() }.getOrDefault(pkg)
            InstalledApp(pkg, label)
        }
        deduplicate(mapped)
    }
}
