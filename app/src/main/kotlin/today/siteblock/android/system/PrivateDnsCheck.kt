package today.siteblock.android.system

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

/**
 * True when Private DNS is set to a specific provider ("strict" mode) on a real network. Strict
 * mode sends DNS over TLS to that provider and skips SiteBlock's filter, so the UI flags it.
 */
object PrivateDnsCheck {
    @Suppress("DEPRECATION") // allNetworks: still the simplest way to see non-VPN networks from a VPN app
    fun isStrict(context: Context): Boolean {
        val cm = context.getSystemService(ConnectivityManager::class.java) ?: return false
        return cm.allNetworks.any { network ->
            val caps = cm.getNetworkCapabilities(network)
            val props = cm.getLinkProperties(network)
            caps != null && props != null &&
                !caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN) &&
                props.isPrivateDnsActive && props.privateDnsServerName != null
        }
    }
}
