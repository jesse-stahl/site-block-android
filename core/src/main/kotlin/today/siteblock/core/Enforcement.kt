package today.siteblock.core

/** What the services must enforce right now. Computed from the BlockList; never persisted. */
data class Enforcement(
    val blockingOn: Boolean,
    val appPackages: Set<String>,
    /** Domains the DNS filter blocks (user sites + DoH providers); empty when the filter is off. */
    val filterDomains: List<String>,
    val filterActive: Boolean,
    val filterPaused: Boolean,
) {
    /** The settings guard runs whenever blocking is on. */
    val guardActive: Boolean get() = blockingOn

    /** VPN / Private DNS screens are guarded only while the web filter is actually running. */
    val guardNetworkSettings: Boolean get() = filterActive

    companion object {
        val OFF = Enforcement(false, emptySet(), emptyList(), filterActive = false, filterPaused = false)
    }
}

object EnforcementPolicy {

    fun isFilterPaused(list: BlockList, now: Long, bootCount: Int): Boolean {
        val pause = list.filterPause ?: return false
        val until = pause.untilMonotonic ?: return true
        return pause.bootCount == bootCount && now < until
    }

    /**
     * [now] is monotonic ms; [bootCount] identifies the current boot. The filter runs only if
     * blocking is on, it isn't paused, and at least one active row has a website.
     */
    fun compute(list: BlockList, now: Long, bootCount: Int): Enforcement {
        if (!list.blockingOn) return Enforcement.OFF
        val active = list.activeItems()
        val userDomains = active.flatMap { it.domains }.distinct()
        val paused = isFilterPaused(list, now, bootCount)
        val filterActive = !paused && userDomains.isNotEmpty()
        return Enforcement(
            blockingOn = true,
            appPackages = active.flatMap { it.packages }.toSet(),
            filterDomains = if (filterActive) (userDomains + DohProviders.hostnames).distinct() else emptyList(),
            filterActive = filterActive,
            filterPaused = paused,
        )
    }
}
