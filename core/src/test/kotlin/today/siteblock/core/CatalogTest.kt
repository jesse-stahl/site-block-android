package today.siteblock.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CatalogTest {
    @Test fun starterListHasTheAgreedApps() {
        assertEquals(
            listOf("x", "instagram", "threads", "facebook", "tiktok", "reddit", "youtube", "snapchat"),
            Catalog.entries.map { it.id },
        )
    }

    @Test fun xCoversTheAppAndAllItsAliases() {
        val x = Catalog.byId("x")!!
        assertEquals(listOf("com.twitter.android"), x.packages)
        assertEquals(listOf("x.com", "twitter.com", "t.co", "twimg.com"), x.domains)
    }

    @Test fun everyEntryIsValid() {
        for (entry in Catalog.entries) {
            assertTrue(entry.id, entry.packages.isNotEmpty() && entry.domains.isNotEmpty())
            for (p in entry.packages) assertEquals(p, PackageId.normalize(p))
            for (p in entry.packages) assertTrue(p, PackageId.isBlockable(p))
            for (d in entry.domains) assertEquals(d, Domain.normalize(d))
        }
    }

    @Test fun dohListIsValid() {
        for (h in DohProviders.hostnames) assertEquals(h, Domain.normalize(h))
        for (ip in DohProviders.ipv4Addresses) assertEquals(ip, Ipv4UdpPacketIpCheck.roundTrip(ip))
        assertTrue("use-application-dns.net" in DohProviders.hostnames)
        assertTrue("1.1.1.1" in DohProviders.ipv4Addresses)
        for (ip in DohProviders.ipv6Addresses) {
            assertTrue(ip, java.net.InetAddress.getByName(ip) is java.net.Inet6Address)
        }
        assertTrue("2606:4700:4700::1111" in DohProviders.ipv6Addresses)
        assertTrue("2001:4860:4860::8888" in DohProviders.ipv6Addresses)
    }
}

/** Local IPv4 check so this test does not depend on Task 4's packet class. */
private object Ipv4UdpPacketIpCheck {
    fun roundTrip(ip: String): String {
        val parts = ip.split('.').map { it.toInt() }
        require(parts.size == 4 && parts.all { it in 0..255 })
        return parts.joinToString(".")
    }
}
