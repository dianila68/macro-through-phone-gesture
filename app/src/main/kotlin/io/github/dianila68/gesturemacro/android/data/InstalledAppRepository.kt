package io.github.dianila68.gesturemacro.android.data

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * ticket-037: Quarantined to `.android.data`.
 * Resolves installed launchable apps to friendly display data.
 * Uses `queryIntentActivities` against ACTION_MAIN + CATEGORY_LAUNCHER; no QUERY_ALL_PACKAGES.
 */
class InstalledAppRepository(private val context: Context) {

    data class AppInfo(
        val packageName: String,
        val label: String,
        val icon: Drawable,
    )

    private var cached: List<AppInfo> = emptyList()

    fun apps(): List<AppInfo> = cached

    suspend fun refresh() {
        cached = withContext(Dispatchers.IO) { queryLaunchable() }
    }

    private fun queryLaunchable(): List<AppInfo> {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        @Suppress("DEPRECATION")
        val resolveInfos = pm.queryIntentActivities(intent, PackageManager.GET_META_DATA)
        return resolveInfos
            .distinctBy { it.activityInfo.packageName }
            .map { ri ->
                AppInfo(
                    packageName = ri.activityInfo.packageName,
                    label = ri.loadLabel(pm).toString(),
                    icon = ri.loadIcon(pm),
                )
            }
            .sortedBy { it.label.lowercase() }
    }

    fun isLaunchable(packageName: String): Boolean =
        context.packageManager.getLaunchIntentForPackage(packageName) != null
}
