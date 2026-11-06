package today.siteblock.android.system

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager

data class InstalledApp(val packageName: String, val label: String)

/** Launchable apps (visible via the manifest's <queries> LAUNCHER intent). Call off the main thread. */
object InstalledApps {
    @Suppress("DEPRECATION") // flag-int overloads: the Flags variants are API 33+
    fun launchable(context: Context): List<InstalledApp> {
        val pm = context.packageManager
        val launcher = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return pm.queryIntentActivities(launcher, 0)
            .map { InstalledApp(it.activityInfo.packageName, it.loadLabel(pm).toString()) }
            .distinctBy { it.packageName }
            .filter { it.packageName != context.packageName }
            .sortedBy { it.label.lowercase() }
    }

    /** The app's label if it's installed and visible, else null. */
    @Suppress("DEPRECATION")
    fun labelFor(context: Context, packageName: String): String? = try {
        val pm = context.packageManager
        pm.getApplicationInfo(packageName, 0).loadLabel(pm).toString()
    } catch (e: PackageManager.NameNotFoundException) {
        null
    }
}
