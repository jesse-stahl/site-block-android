package today.siteblock.android.system

import android.app.KeyguardManager
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.net.VpnService
import android.os.PowerManager
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat

/** Which one-time setup steps are done. Always-on VPN can't be read by apps, so it's instructions only. */
data class SetupStatus(
    val screenLock: Boolean,
    val accessibility: Boolean,
    val vpnConsent: Boolean,
    val deviceAdmin: Boolean,
    val batteryUnrestricted: Boolean,
    val notifications: Boolean,
    val privateDnsOk: Boolean,
) {
    val complete: Boolean
        get() = screenLock && accessibility && vpnConsent && deviceAdmin && batteryUnrestricted && notifications && privateDnsOk
}

object SetupChecks {
    fun read(context: Context): SetupStatus {
        val keyguard = context.getSystemService(KeyguardManager::class.java)
        val dpm = context.getSystemService(DevicePolicyManager::class.java)
        val power = context.getSystemService(PowerManager::class.java)
        return SetupStatus(
            screenLock = keyguard?.isDeviceSecure == true,
            accessibility = accessibilityEnabled(context),
            vpnConsent = VpnService.prepare(context) == null,
            deviceAdmin = dpm?.isAdminActive(adminComponent(context)) == true,
            batteryUnrestricted = power?.isIgnoringBatteryOptimizations(context.packageName) == true,
            notifications = NotificationManagerCompat.from(context).areNotificationsEnabled(),
            privateDnsOk = !PrivateDnsCheck.isStrict(context),
        )
    }

    fun adminComponent(context: Context) = ComponentName(context.packageName, ServiceNames.ADMIN_RECEIVER)

    private fun accessibilityEnabled(context: Context): Boolean {
        val enabled = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
            ?: return false
        val mine = ComponentName(context.packageName, ServiceNames.ACCESSIBILITY_SERVICE)
        return enabled.split(':').any { ComponentName.unflattenFromString(it) == mine }
    }
}
