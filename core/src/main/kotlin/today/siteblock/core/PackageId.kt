package today.siteblock.core

/** Validation of Android application IDs (package names), plus the never-block list. */
object PackageId {

    /** SiteBlock's own application ID. */
    const val SELF = "today.siteblock.android"

    private val pattern = Regex("^[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z][A-Za-z0-9_]*)+$")

    /**
     * Apps that must never be blocked: blocking them would lock the user out of the phone or of
     * SiteBlock's own unlock path (settings, launchers, dialers, installers, permission UI).
     */
    val neverBlock: Set<String> = setOf(
        SELF,
        "com.android.settings",
        "com.android.systemui",
        "com.android.phone",
        "com.android.server.telecom",
        "com.android.emergency",
        "com.android.vpndialogs",
        "com.android.packageinstaller",
        "com.google.android.packageinstaller",
        "com.google.android.permissioncontroller",
        "com.android.permissioncontroller",
        "com.google.android.dialer",
        "com.samsung.android.dialer",
        "com.samsung.android.incallui",
        "com.sec.android.app.launcher",
        "com.google.android.apps.nexuslauncher",
        "com.android.launcher3",
    )

    /** Trim and validate; returns the ID unchanged in case (Android IDs are case-sensitive) or null. */
    fun normalize(raw: String): String? {
        val id = raw.trim()
        return if (id.length <= 255 && pattern.matches(id)) id else null
    }

    fun isBlockable(id: String): Boolean = id !in neverBlock
}
