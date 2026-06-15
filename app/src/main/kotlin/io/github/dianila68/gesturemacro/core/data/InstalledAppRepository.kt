package io.github.dianila68.gesturemacro.core.data

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * ticket-035: Resolves installed launchable apps to friendly display data.
 *
 * Uses `queryIntentActivities` against ACTION_MAIN + CATEGORY_LAUNCHER, backed by
 * the `<queries>` block in AndroidManifest (ticket-019). No QUERY_ALL_PACKAGES
 * permission is used or required — scoped visibility covers all launchable apps.
 *
 * Results are cached per-instance; call [refresh] on Activity resume to pick up
 * newly installed / uninstalled apps.
 */
class InstalledAppRepository(private val context: Context) {

    data class AppInfo(
        val packageName: String,
        val label: String,
        val icon: Drawable,
    )

    private var cached: List<AppInfo> = emptyList()

    /** Returns (possibly cached) list of launchable installed apps, sorted by label. */
    fun apps(): List<AppInfo> = cached

    /** Re-queries PackageManager on an IO thread and updates the cache. */
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

    /** Returns true if [packageName] is still launchable; false if uninstalled/visibility-blocked. */
    fun isLaunchable(packageName: String): Boolean =
        context.packageManager.getLaunchIntentForPackage(packageName) != null
}
