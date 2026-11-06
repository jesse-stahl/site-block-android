package today.siteblock.core

/**
 * Answers "is this host blocked?" for a set of blocked base domains. A host is blocked if it
 * equals a blocked domain or is any subdomain of one ("api.x.com" is blocked by "x.com";
 * "notx.com" is not). Lookup walks the host's suffixes, so it is O(number of labels).
 * Immutable and safe to share across threads.
 */
class DomainMatcher(domains: Collection<String>) {

    private val blocked: Set<String> = domains.map { it.trim().lowercase().removeSuffix(".") }.toHashSet()

    val isEmpty: Boolean get() = blocked.isEmpty()

    fun matches(host: String): Boolean {
        var candidate = host.trim().lowercase().removeSuffix(".")
        while (candidate.isNotEmpty()) {
            if (candidate in blocked) return true
            val dot = candidate.indexOf('.')
            if (dot < 0) return false
            candidate = candidate.substring(dot + 1)
        }
        return false
    }
}
