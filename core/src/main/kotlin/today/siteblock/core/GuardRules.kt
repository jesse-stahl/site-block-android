package today.siteblock.core

/**
 * Decides whether a visible system screen could be used to disable SiteBlock and must be
 * bounced to the home screen. Text-based so it works on Samsung One UI and stock Android alike.
 */
object GuardRules {

    enum class Verdict { ALLOW, BOUNCE }

    /** Lowercase app label as shown in Settings. */
    const val APP_LABEL = "siteblock"

    private const val VPN_DIALOGS = "com.android.vpndialogs"

    private val installers = setOf(
        "com.android.packageinstaller",
        "com.google.android.packageinstaller",
        "com.samsung.android.packageinstaller",
    )

    /** Only events from these packages are ever inspected. */
    val guardedPackages: Set<String> = setOf("com.android.settings", "com.samsung.android.settings", VPN_DIALOGS) + installers

    /**
     * Whole labels (exact, case-insensitive) that, next to "SiteBlock", mark a screen that can
     * stop, remove or unhook it: App info buttons/rows, the Accessibility and Device admin pages,
     * VPN settings. Exact matching (not substring) so an ordinary list containing other apps'
     * names, e.g. "Proton VPN" or "Accessibility Pro", is not mistaken for one of these screens.
     */
    private val dangerousLabels = setOf(
        "app info", "force stop", "uninstall", "clear data", "clear storage", "clear cache",
        "storage", "battery", "permissions", "notifications", "open by default", "mobile data",
        "deactivate", "deactivate this device admin app", "device admin apps", "device admin app",
        "accessibility", "installed apps", "installed services", "downloaded apps", "vpn",
    )

    fun verdict(packageName: String, screenTexts: List<String>, enforcement: Enforcement): Verdict {
        if (!enforcement.guardActive || packageName !in guardedPackages) return Verdict.ALLOW
        if (packageName == VPN_DIALOGS) return if (enforcement.guardNetworkSettings) Verdict.BOUNCE else Verdict.ALLOW

        val texts = screenTexts.map { it.trim().lowercase() }
        if (enforcement.guardNetworkSettings && texts.any { "private dns" in it }) return Verdict.BOUNCE

        if (texts.none { it.startsWith(APP_LABEL) }) return Verdict.ALLOW
        if (packageName in installers) return Verdict.BOUNCE

        val networkScreen = texts.any { it == "vpn" }
        if (networkScreen && !enforcement.guardNetworkSettings) return Verdict.ALLOW

        return if (texts.any { it in dangerousLabels }) Verdict.BOUNCE else Verdict.ALLOW
    }
}
