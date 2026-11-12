package today.siteblock.core

import kotlinx.serialization.Serializable

/**
 * An app kept out of the web-filter VPN, for apps the tunnel breaks. WhatsApp calls, for
 * example, stay stuck on "Connecting" while the tunnel is up, though only DNS is routed into it.
 * A whitelisted app's own DNS isn't filtered; app blocking still applies to it.
 */
@Serializable
data class WhitelistedApp(val packageId: String, val label: String) {
    companion object {
        /** Seeded for new lists and for lists saved before the whitelist existed. */
        val DEFAULTS: List<WhitelistedApp> = listOf(
            WhitelistedApp("com.whatsapp", "WhatsApp"),
            WhitelistedApp("com.whatsapp.w4b", "WhatsApp Business"),
        )

        /** Valid package ID that isn't SiteBlock itself. */
        fun isAllowed(packageId: String): Boolean =
            PackageId.normalize(packageId) == packageId && packageId != PackageId.SELF
    }
}
