package today.siteblock.core

/**
 * Encrypted-DNS (DoH/DoT) endpoints that would let a browser skip the DNS filter. While the
 * filter runs, these hostnames are blocked (so browsers fall back to system DNS) and these IPs
 * are routed into the tunnel (UDP/53 to them is filtered; everything else to them is dropped).
 * "use-application-dns.net" is Firefox's canary domain: NXDOMAIN there disables its auto-DoH.
 */
object DohProviders {
    val hostnames: List<String> = listOf(
        "use-application-dns.net",
        "dns.google",
        "dns.google.com",
        "cloudflare-dns.com",
        "one.one.one.one",
        "dns.quad9.net",
        "dns9.quad9.net",
        "dns10.quad9.net",
        "dns11.quad9.net",
        "doh.opendns.com",
        "doh.familyshield.opendns.com",
        "dns.adguard.com",
        "dns.adguard-dns.com",
        "dns.nextdns.io",
        "doh.cleanbrowsing.org",
        "doh.mullvad.net",
        "dns.mullvad.net",
        "dns.controld.com",
        "freedns.controld.com",
        "doh.dns.sb",
    )

    val ipv4Addresses: List<String> = listOf(
        "8.8.8.8", "8.8.4.4",
        "1.1.1.1", "1.0.0.1", "1.1.1.2", "1.0.0.2", "1.1.1.3", "1.0.0.3",
        "9.9.9.9", "149.112.112.112", "9.9.9.10", "149.112.112.10", "9.9.9.11", "149.112.112.11",
        "208.67.222.222", "208.67.220.220",
        "94.140.14.14", "94.140.15.15",
        "185.228.168.168", "185.228.169.168",
        "45.90.28.0", "45.90.30.0",
        "194.242.2.2",
        "76.76.2.0", "76.76.10.0",
        "185.222.222.222", "45.11.45.11",
    )
}
