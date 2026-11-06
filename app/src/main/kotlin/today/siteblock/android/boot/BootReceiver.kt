package today.siteblock.android.boot

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import today.siteblock.android.repository

/** Starting the process creates the repository; this also re-pushes enforcement (starts the web filter). */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED -> context.repository.retryEnforcement()
        }
    }
}
