package today.siteblock.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class DnsFilterTest {
    private val filter = DnsFilter(DomainMatcher(listOf("x.com")))

    @Test fun blockedNameGetsNxdomainBackToTheClient() {
        val decision = filter.decide(udpToDns(dnsQuery(7, "api.x.com")))
        assertTrue(decision is DnsDecision.Respond)
        val reply = Ipv4UdpPacket.parse((decision as DnsDecision.Respond).packet)!!
        assertEquals("10.111.222.2", reply.srcIp)
        assertEquals("10.111.222.1", reply.dstIp)
        assertEquals(53, reply.srcPort)
        assertEquals(40000, reply.dstPort)
        assertEquals(7, Bytes.u16(reply.payload, 0))
        assertEquals(DnsMessage.RCODE_NXDOMAIN, Bytes.u16(reply.payload, 2) and 0xF)
    }

    @Test fun allowedNameIsForwarded() {
        val query = dnsQuery(8, "wikipedia.org")
        val decision = filter.decide(udpToDns(query))
        assertTrue(decision is DnsDecision.Forward)
        val forwarded = (decision as DnsDecision.Forward).request
        assertEquals(53, forwarded.dstPort)
        assertTrue(query.contentEquals(forwarded.payload))
    }

    @Test fun nonDnsAndGarbageAreDropped() {
        assertSame(DnsDecision.Drop, filter.decide(udpToDns(dnsQuery(1, "x.com"), dstPort = 443)))
        assertSame(DnsDecision.Drop, filter.decide(byteArrayOf(1, 2, 3)))
        assertSame(DnsDecision.Drop, filter.decide(udpToDns(byteArrayOf(0, 1, 2))))
    }

    @Test fun respectsTheLengthArgument() {
        val packet = udpToDns(dnsQuery(9, "x.com"))
        val buffer = packet + ByteArray(64)
        assertTrue(filter.decide(buffer, packet.size) is DnsDecision.Respond)
    }
}
