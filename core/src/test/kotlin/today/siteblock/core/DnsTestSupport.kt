package today.siteblock.core

import java.io.ByteArrayOutputStream

/** Builds a minimal DNS query: header + one question. flags 0x0100 = standard query, RD set. */
internal fun dnsQuery(id: Int, name: String, type: Int = DnsMessage.TYPE_A, flags: Int = 0x0100): ByteArray {
    val out = ByteArrayOutputStream()
    fun w16(v: Int) {
        out.write((v ushr 8) and 0xFF)
        out.write(v and 0xFF)
    }
    w16(id); w16(flags); w16(1); w16(0); w16(0); w16(0)
    for (label in name.split('.')) {
        out.write(label.length)
        out.write(label.toByteArray(Charsets.US_ASCII))
    }
    out.write(0)
    w16(type); w16(1)
    return out.toByteArray()
}

internal val CLIENT_IP: ByteArray = Ipv4UdpPacket.ipBytes("10.111.222.1")
internal val FAKE_DNS_IP: ByteArray = Ipv4UdpPacket.ipBytes("10.111.222.2")

/** A full IPv4/UDP packet from the tunnel client to the fake DNS server carrying [payload]. */
internal fun udpToDns(payload: ByteArray, srcPort: Int = 40000, dstPort: Int = 53): ByteArray =
    Ipv4UdpPacket(CLIENT_IP, FAKE_DNS_IP, srcPort, dstPort, payload).toBytes()
