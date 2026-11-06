package today.siteblock.android.system

/** Fully-qualified component names, so modules can reach each other without compile-time coupling. */
object ServiceNames {
    const val VPN_SERVICE = "today.siteblock.android.vpn.FilterVpnService"
    const val ACCESSIBILITY_SERVICE = "today.siteblock.android.access.BlockerAccessibilityService"
    const val ADMIN_RECEIVER = "today.siteblock.android.admin.SiteBlockAdminReceiver"
    const val MAIN_ACTIVITY = "today.siteblock.android.ui.MainActivity"

    /** Start (or refresh) the web filter. */
    const val ACTION_APPLY = "today.siteblock.android.action.APPLY"
    /** Stop the web filter and release the VPN. */
    const val ACTION_STOP = "today.siteblock.android.action.STOP"
}
