package today.siteblock.core

/** What to do with one packet read from the VPN tunnel. */
sealed interface DnsDecision {
    /** Write [packet] straight back into the tunnel (a blocked-name NXDOMAIN reply). */
    class Respond(val packet: ByteArray) : DnsDecision
    /** Send [request]'s payload to a real resolver; wrap the answer with request.reply(...). */
    class Forward(val request: Ipv4UdpPacket) : DnsDecision
    /** Not DNS (e.g. TCP/443 to a routed DoH IP) or malformed: discard. */
    data object Drop : DnsDecision
}

/** Stateless and thread-safe; swap in a new instance when the block list changes. */
class DnsFilter(private val matcher: DomainMatcher) {

    fun decide(buf: ByteArray, length: Int = buf.size): DnsDecision {
        val packet = Ipv4UdpPacket.parse(buf, length) ?: return DnsDecision.Drop
        if (packet.dstPort != 53) return DnsDecision.Drop
        val question = DnsMessage.parseQuestion(packet.payload) ?: return DnsDecision.Drop
        if (!matcher.matches(question.name)) return DnsDecision.Forward(packet)
        val response = DnsMessage.blockedResponse(packet.payload) ?: return DnsDecision.Drop
        return DnsDecision.Respond(packet.reply(response).toBytes())
    }
}
