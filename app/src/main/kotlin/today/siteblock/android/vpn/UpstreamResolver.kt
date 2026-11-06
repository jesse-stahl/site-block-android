package today.siteblock.android.vpn

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.SystemClock
import java.net.Inet4Address
import java.net.InetAddress

/** A real DNS server and the network it belongs to (null network = use the default route). */
class Upstream(val address: InetAddress, val network: Network?)

/**
 * Finds the DNS server of the current real (non-VPN) network, preferring validated, unmetered
 * networks (Wi-Fi) and IPv4 servers. Using the network's own resolver keeps carrier DNS64 and
 * captive portals working. Cached for 5 s. Thread-safe.
 */
class UpstreamResolver(
    context: Context,
    private val clock: () -> Long = { SystemClock.elapsedRealtime() },
) {
    private val cm = context.getSystemService(ConnectivityManager::class.java)

    @Volatile private var cached: Upstream? = null
    @Volatile private var cachedAt = 0L

    fun upstream(): Upstream {
        val now = clock()
        cached?.let { if (now - cachedAt < CACHE_MS) return it }
        val found = lookup() ?: Upstream(FALLBACK, null)
        cached = found
        cachedAt = now
        return found
    }

    /** Forget the cache (e.g. after a network change made the last server unreachable). */
    fun invalidate() {
        cached = null
    }

    @Suppress("DEPRECATION") // allNetworks: needed to see real networks from inside a VPN app
    private fun lookup(): Upstream? {
        val manager = cm ?: return null
        data class Candidate(val network: Network, val score: Int, val servers: List<InetAddress>)
        val candidates = manager.allNetworks.mapNotNull { network ->
            val caps = manager.getNetworkCapabilities(network) ?: return@mapNotNull null
            if (caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) return@mapNotNull null
            if (!caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) return@mapNotNull null
            val servers = manager.getLinkProperties(network)?.dnsServers.orEmpty()
            if (servers.isEmpty()) return@mapNotNull null
            var score = 0
            if (caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)) score += 2
            if (caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)) score += 1
            Candidate(network, score, servers)
        }
        val best = candidates.maxByOrNull { it.score } ?: return null
        val server = best.servers.firstOrNull { it is Inet4Address } ?: best.servers.first()
        return Upstream(server, best.network)
    }

    companion object {
        private const val CACHE_MS = 5_000L
        val FALLBACK: InetAddress = InetAddress.getByAddress(byteArrayOf(1, 1, 1, 1))
    }
}
