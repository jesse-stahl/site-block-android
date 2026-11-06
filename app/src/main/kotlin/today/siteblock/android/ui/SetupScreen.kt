package today.siteblock.android.ui

import android.Manifest
import android.app.admin.DevicePolicyManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.net.VpnService
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import today.siteblock.android.system.SetupChecks
import today.siteblock.android.system.SetupStatus

@Composable
fun SetupScreen(status: SetupStatus, blockingOn: Boolean, onRefresh: () -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    val vpnConsent = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { onRefresh() }
    val adminConsent = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { onRefresh() }
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { onRefresh() }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onBack) { Text("← Back") }
                Text("Setup", style = MaterialTheme.typography.titleLarge)
            }
        }
        if (blockingOn) {
            item { Text("Some of these settings screens are locked while blocking is ON. Turn blocking off to change them.") }
        }
        item {
            SetupRow("1. Screen lock PIN", status.screenLock, "SiteBlock asks for your phone's PIN before anything gets weaker.", "Open lock screen settings") {
                open(context, Intent(Settings.ACTION_SECURITY_SETTINGS))
            }
        }
        item {
            SetupRow(
                "2. App blocking (Accessibility)",
                status.accessibility,
                "Settings → Accessibility → Installed apps → SiteBlock → On. If the switch is greyed out, open SiteBlock's App info → ⋮ → Allow restricted settings, then try again.",
                "Open Accessibility settings",
            ) { open(context, Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
        }
        item {
            SetupRow("3. Web filter (VPN permission)", status.vpnConsent, "Lets SiteBlock run its on-phone web filter. Nothing leaves your phone.", "Allow VPN") {
                val consent = VpnService.prepare(context)
                if (consent != null) vpnConsent.launch(consent) else onRefresh()
            }
        }
        item {
            SetupRow("4. Uninstall protection (Device admin)", status.deviceAdmin, "Stops SiteBlock from being uninstalled while it's active.", "Activate") {
                adminConsent.launch(
                    Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
                        .putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, SetupChecks.adminComponent(context))
                        .putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Stops SiteBlock from being uninstalled while blocking is on."),
                )
            }
        }
        item {
            SetupRow(
                "5. Keep SiteBlock awake (battery)",
                status.batteryUnrestricted,
                "Samsung puts apps to sleep. Allow this, then also go to Settings → Battery → Background usage limits → Never auto sleeping apps → add SiteBlock.",
                "Allow",
            ) {
                open(context, Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:${context.packageName}")))
            }
        }
        item {
            SetupRow("6. Notifications", status.notifications, "So SiteBlock can warn you if blocking stops working.", "Allow") {
                if (Build.VERSION.SDK_INT >= 33) {
                    notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    open(context, Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName))
                }
            }
        }
        item {
            SetupRow(
                "7. Private DNS: Off or Automatic",
                status.privateDnsOk,
                "A specific Private DNS provider skips the web filter. Settings → Connections → More connection settings → Private DNS → Off or Automatic.",
                "Open network settings",
            ) { open(context, Intent(Settings.ACTION_WIRELESS_SETTINGS)) }
        }
        item {
            SetupRow(
                "8. Always-on VPN (recommended)",
                null,
                "Restarts the web filter after a reboot. Settings → Connections → More connection settings → VPN → ⚙ next to SiteBlock → turn on Always-on VPN. Leave 'Block connections without VPN' OFF.",
                "Open VPN settings",
            ) { open(context, Intent(Settings.ACTION_VPN_SETTINGS)) }
        }
    }
}

/** [done] null = can't be checked automatically (instructions only). */
@Composable
private fun SetupRow(title: String, done: Boolean?, body: String, button: String, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            val mark = when (done) {
                true -> "✓ "
                false -> "✗ "
                null -> ""
            }
            Text(mark + title, style = MaterialTheme.typography.titleMedium)
            Text(body, style = MaterialTheme.typography.bodyMedium)
            if (done != true) OutlinedButton(onClick = onClick) { Text(button) }
        }
    }
}

private fun open(context: Context, intent: Intent) {
    try {
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        context.startActivity(Intent(Settings.ACTION_SETTINGS))
    }
}
