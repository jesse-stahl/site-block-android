package today.siteblock.core

import org.junit.Assert.assertEquals
import org.junit.Test
import today.siteblock.core.GuardRules.Verdict.ALLOW
import today.siteblock.core.GuardRules.Verdict.BOUNCE

class GuardRulesTest {
    private val settings = "com.android.settings"
    private val filterOn = Enforcement(true, setOf("com.twitter.android"), listOf("x.com"), filterActive = true, filterPaused = false)
    private val filterPaused = filterOn.copy(filterDomains = emptyList(), filterActive = false, filterPaused = true)

    private fun v(pkg: String, e: Enforcement, vararg texts: String) = GuardRules.verdict(pkg, texts.toList(), e)

    @Test fun nothingIsGuardedWhileBlockingIsOff() {
        assertEquals(ALLOW, v(settings, Enforcement.OFF, "SiteBlock", "Force stop", "Uninstall"))
        assertEquals(ALLOW, v("com.android.vpndialogs", Enforcement.OFF, "Disconnect"))
    }

    @Test fun unguardedPackagesAreAllowed() {
        assertEquals(ALLOW, v("org.telegram.messenger", filterOn, "SiteBlock", "Uninstall"))
    }

    @Test fun ownAppInfoPageBounces() {
        assertEquals(BOUNCE, v(settings, filterOn, "App info", "SiteBlock", "Open", "Uninstall", "Force stop"))
    }

    @Test fun deviceAdminPageBounces() {
        assertEquals(BOUNCE, v(settings, filterOn, "Device admin apps", "SiteBlock"))
    }

    @Test fun accessibilityInstalledAppsBouncesEvenDuringAFilterPause() {
        assertEquals(BOUNCE, v(settings, filterPaused, "Installed apps", "SiteBlock", "Off"))
    }

    @Test fun ordinaryAppsListIsAllowed() {
        assertEquals(ALLOW, v(settings, filterOn, "Apps", "Search", "SiteBlock", "Telegram", "X"))
    }

    @Test fun ownAccessibilityDetailPageBouncesOnPixelAndSamsung() {
        val description = "SiteBlock uses this to block the apps on your block list and to lock the settings screens that could switch it off while blocking is on."
        assertEquals(BOUNCE, v(settings, filterPaused, "SiteBlock", "Use SiteBlock", "SiteBlock shortcut", description))
        assertEquals(BOUNCE, v(settings, filterPaused, "SiteBlock", "On", description))
    }

    @Test fun samsungAccessibilityPackageIsGuarded() {
        assertEquals(BOUNCE, v("com.samsung.accessibility", filterPaused, "Installed apps", "SiteBlock", "Off"))
        assertEquals(false, PackageId.isBlockable("com.samsung.accessibility"))
    }

    @Test fun appsListWithDangerWordsInOtherAppNamesIsAllowed() {
        assertEquals(ALLOW, v(settings, filterOn, "Apps", "SiteBlock", "Proton VPN", "AccuBattery", "Files by Google: Clean storage"))
        assertEquals(ALLOW, v(settings, filterOn, "Apps", "SiteBlock", "Accessibility Pro"))
    }

    @Test fun pixelAccessibilityPageBounces() {
        assertEquals(BOUNCE, v(settings, filterOn, "Accessibility", "Downloaded apps", "SiteBlock", "Off"))
    }

    @Test fun settingsScreensWithoutSiteBlockAreAllowed() {
        assertEquals(ALLOW, v(settings, filterOn, "Display", "Brightness", "Dark mode"))
    }

    @Test fun privateDnsBouncesOnlyWhileTheFilterRuns() {
        assertEquals(BOUNCE, v(settings, filterOn, "Private DNS", "Off", "Automatic", "Private DNS provider hostname"))
        assertEquals(ALLOW, v(settings, filterPaused, "Private DNS", "Off", "Automatic"))
    }

    @Test fun vpnSettingsBounceOnlyWhileTheFilterRuns() {
        assertEquals(BOUNCE, v(settings, filterOn, "VPN", "SiteBlock", "Connected"))
        assertEquals(ALLOW, v(settings, filterPaused, "VPN", "SiteBlock"))
    }

    @Test fun vpnDialogsBounceOnlyWhileTheFilterRuns() {
        assertEquals(BOUNCE, v("com.android.vpndialogs", filterOn, "Connection request"))
        assertEquals(ALLOW, v("com.android.vpndialogs", filterPaused, "Connection request"))
    }

    @Test fun uninstallDialogBounces() {
        assertEquals(BOUNCE, v("com.google.android.packageinstaller", filterPaused, "SiteBlock", "Do you want to uninstall this app?"))
    }

    @Test fun labelMatchIgnoresCase() {
        assertEquals(BOUNCE, v(settings, filterOn, "SITEBLOCK", "Force stop"))
    }
}
