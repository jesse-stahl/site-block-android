package today.siteblock.core

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class Ipv4UdpPacketTest {
    private val payload = byteArrayOf(1, 2, 3, 4, 5)

    @Test fun roundTripsThroughBytes() {
        val parsed = Ipv4UdpPacket.parse(udpToDns(payload))
        assertNotNull(parsed)
        parsed!!
        assertEquals("10.111.222.1", parsed.srcIp)
        assertEquals("10.111.222.2", parsed.dstIp)
        assertEquals(40000, parsed.srcPort)
        assertEquals(53, parsed.dstPort)
        assertArrayEquals(payload, parsed.payload)
    }

    @Test fun builtHeaderHasValidChecksumAndLengths() {
        val bytes = udpToDns(payload)
        assertEquals(28 + payload.size, bytes.size)
        assertEquals(0, Ipv4UdpPacket.headerChecksum(bytes, 0, 20))
        assertEquals(bytes.size, Bytes.u16(bytes, 2))
        assertEquals(8 + payload.size, Bytes.u16(bytes, 24))
        assertEquals(17, bytes[9].toInt())
    }

    @Test fun replySwapsAddressesAndPorts() {
        val request = Ipv4UdpPacket.parse(udpToDns(payload))!!
        val reply = Ipv4UdpPacket.parse(request.reply(byteArrayOf(9, 9)).toBytes())!!
        assertEquals("10.111.222.2", reply.srcIp)
        assertEquals("10.111.222.1", reply.dstIp)
        assertEquals(53, reply.srcPort)
        assertEquals(40000, reply.dstPort)
        assertArrayEquals(byteArrayOf(9, 9), reply.payload)
    }

    @Test fun parseHonoursTheLengthArgument() {
        val bytes = udpToDns(payload)
        val buffer = bytes + ByteArray(100)
        assertArrayEquals(payload, Ipv4UdpPacket.parse(buffer, bytes.size)!!.payload)
    }

    @Test fun rejectsShortNonUdpFragmentedAndIpv6() {
        val good = udpToDns(payload)
        assertNull(Ipv4UdpPacket.parse(good.copyOf(10)))
        assertNull(Ipv4UdpPacket.parse(good.copyOf().also { it[9] = 6 }))      // TCP
        assertNull(Ipv4UdpPacket.parse(good.copyOf().also { it[6] = 0x20 }))   // more-fragments flag
        assertNull(Ipv4UdpPacket.parse(good.copyOf().also { it[0] = 0x60 }))   // IPv6
        assertNull(Ipv4UdpPacket.parse(good, good.size + 1))                   // length past buffer
    }

    @Test fun ipHelpers() {
        assertEquals("8.8.4.4", Ipv4UdpPacket.ipString(Ipv4UdpPacket.ipBytes("8.8.4.4")))
        assertEquals("208.67.222.222", Ipv4UdpPacket.ipString(Ipv4UdpPacket.ipBytes("208.67.222.222")))
    }
}
