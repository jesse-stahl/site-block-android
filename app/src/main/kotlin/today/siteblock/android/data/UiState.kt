package today.siteblock.android.data

import today.siteblock.core.BlockList
import today.siteblock.core.Enforcement
import today.siteblock.core.PendingWeaken

/** Live status reported by the services themselves (not derivable from the block list). */
data class RuntimeStatus(
    val vpnRunning: Boolean = false,
    val vpnError: String? = null,
    val accessibilityConnected: Boolean = false,
)

/** Everything the UI renders. Rebuilt on every change and every tick. */
data class UiState(
    val list: BlockList,
    val pending: PendingWeaken?,
    val remainingWaitMs: Long?,
    val remainingConfirmMs: Long?,
    val canConfirm: Boolean,
    val canChangeSettings: Boolean,
    val enforcement: Enforcement,
    /** Time left on a timed filter pause; null if not paused or paused until resumed. */
    val filterPauseRemainingMs: Long?,
    val runtime: RuntimeStatus,
    val privateDnsStrict: Boolean,
    /** A load/save problem to show the user, if any. */
    val problem: String?,
    /** Today's blocked-open counts by package. */
    val attemptsToday: Map<String, Int>,
) {
    /** Why blocking is ON but not fully enforced. Empty when fully enforced or when blocking is off. Never lie about status. */
    val enforcementGaps: List<String>
        get() {
            if (!enforcement.blockingOn) return emptyList()
            val gaps = mutableListOf<String>()
            if (!runtime.accessibilityConnected) {
                gaps += "App blocking and settings protection are off. Turn on SiteBlock in Settings → Accessibility → Installed apps."
            }
            if (enforcement.filterActive && !runtime.vpnRunning) {
                gaps += runtime.vpnError ?: "The web filter isn't running."
            }
            if (enforcement.filterActive && privateDnsStrict) {
                gaps += "Private DNS is set to a specific provider, which skips the web filter. Set it to Off or Automatic."
            }
            return gaps
        }
}
