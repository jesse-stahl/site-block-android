package today.siteblock.core

/**
 * An IPv4 packet carrying one UDP datagram, as read from / written to the VPN tunnel.
 * Unfragmented only; IP options are tolerated on parse, never written.
 */
class Ipv4UdpPacket(
    val srcAddr: ByteArray,
    val dstAddr: ByteArray,
    val srcPort: Int,
    val dstPort: Int,
    val payload: ByteArray,
) {
    init {
        require(srcAddr.size == 4 && dstAddr.size == 4) { "IPv4 addresses must be 4 bytes" }
    }

    val srcIp: String get() = ipString(srcAddr)
    val dstIp: String get() = ipString(dstAddr)

    /** The answer to this packet: addresses and ports swapped, carrying [replyPayload]. */
    fun reply(replyPayload: ByteArray) = Ipv4UdpPacket(dstAddr, srcAddr, dstPort, srcPort, replyPayload)

    /** Serialize: 20-byte IPv4 header (DF, TTL 64, valid checksum) + 8-byte UDP header (checksum 0). */
    fun toBytes(): ByteArray {
        val total = IPV4_HEADER + UDP_HEADER + payload.size
        val b = ByteArray(total)
        b[0] = 0x45
        Bytes.put16(b, 2, total)
        Bytes.put16(b, 6, 0x4000)
        b[8] = 64
        b[9] = PROTOCOL_UDP.toByte()
        srcAddr.copyInto(b, 12)
        dstAddr.copyInto(b, 16)
        Bytes.put16(b, 10, headerChecksum(b, 0, IPV4_HEADER))
        Bytes.put16(b, 20, srcPort)
        Bytes.put16(b, 22, dstPort)
        Bytes.put16(b, 24, UDP_HEADER + payload.size)
        payload.copyInto(b, IPV4_HEADER + UDP_HEADER)
        return b
    }

    companion object {
        const val PROTOCOL_UDP = 17
        private const val IPV4_HEADER = 20
        private const val UDP_HEADER = 8

        /** Parse the first [length] bytes of [buf]; null if not a well-formed unfragmented IPv4/UDP packet. */
        fun parse(buf: ByteArray, length: Int = buf.size): Ipv4UdpPacket? {
            if (length > buf.size || length < IPV4_HEADER + UDP_HEADER) return null
            if (((buf[0].toInt() ushr 4) and 0xF) != 4) return null
            val ihl = (buf[0].toInt() and 0xF) * 4
            if (ihl < IPV4_HEADER || length < ihl + UDP_HEADER) return null
            val totalLength = Bytes.u16(buf, 2)
            if (totalLength < ihl + UDP_HEADER || totalLength > length) return null
            if ((Bytes.u16(buf, 6) and 0x3FFF) != 0) return null
            if ((buf[9].toInt() and 0xFF) != PROTOCOL_UDP) return null
            val udpLength = Bytes.u16(buf, ihl + 4)
            if (udpLength < UDP_HEADER || ihl + udpLength > totalLength) return null
            return Ipv4UdpPacket(
                srcAddr = buf.copyOfRange(12, 16),
                dstAddr = buf.copyOfRange(16, 20),
                srcPort = Bytes.u16(buf, ihl),
                dstPort = Bytes.u16(buf, ihl + 2),
                payload = buf.copyOfRange(ihl + UDP_HEADER, ihl + udpLength),
            )
        }

        /** RFC 1071 one's-complement checksum over [len] bytes at [off]. 0 means "valid" when run over a header that includes its checksum. */
        fun headerChecksum(b: ByteArray, off: Int, len: Int): Int {
            var sum = 0L
            var i = off
            while (i + 1 < off + len) {
                sum += Bytes.u16(b, i)
                i += 2
            }
            if (i < off + len) sum += (b[i].toInt() and 0xFF) shl 8
            while ((sum ushr 16) != 0L) sum = (sum and 0xFFFF) + (sum ushr 16)
            return (sum.inv() and 0xFFFF).toInt()
        }

        fun ipString(addr: ByteArray): String = addr.joinToString(".") { (it.toInt() and 0xFF).toString() }

        fun ipBytes(ip: String): ByteArray {
            val parts = ip.split('.')
            require(parts.size == 4) { "not an IPv4 address: $ip" }
            return ByteArray(4) { i -> parts[i].toInt().also { require(it in 0..255) }.toByte() }
        }
    }
}
