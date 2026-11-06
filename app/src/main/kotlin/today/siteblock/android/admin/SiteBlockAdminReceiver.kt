package today.siteblock.android.admin

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import today.siteblock.android.R

/** Being an active device admin is what stops SiteBlock from being uninstalled directly. */
class SiteBlockAdminReceiver : DeviceAdminReceiver() {
    override fun onDisableRequested(context: Context, intent: Intent): CharSequence =
        context.getString(R.string.admin_disable_warning)
}
