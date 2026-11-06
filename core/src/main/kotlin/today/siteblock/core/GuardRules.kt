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

    /** Words that, next to "SiteBlock", mark a screen that can stop, remove or unhook it. */
    private val dangerousScreenWords = listOf(
        "force stop", "uninstall", "clear data", "clear storage", "clear cache",
        "deactivate", "device admin", "admin app", "installed apps", "installed services",
        "accessibility", "vpn", "permissions", "battery", "storage", "open by default",
    )

    fun verdict(packageName: String, screenTexts: List<String>, enforcement: Enforcement): Verdict {
        if (!enforcement.guardActive || packageName !in guardedPackages) return Verdict.ALLOW
        if (packageName == VPN_DIALOGS) return if (enforcement.guardNetworkSettings) Verdict.BOUNCE else Verdict.ALLOW

        val texts = screenTexts.map { it.trim().lowercase() }
        if (enforcement.guardNetworkSettings && texts.any { "private dns" in it }) return Verdict.BOUNCE

        if (texts.none { it.startsWith(APP_LABEL) }) return Verdict.ALLOW
        if (packageName in installers) return Verdict.BOUNCE

        val networkScreen = texts.any { "vpn" in it }
        if (networkScreen && !enforcement.guardNetworkSettings) return Verdict.ALLOW

        return if (texts.any { t -> dangerousScreenWords.any { it in t } }) Verdict.BOUNCE else Verdict.ALLOW
    }
}
