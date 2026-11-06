package today.siteblock.core

data class DnsQuestion(val name: String, val type: Int, val qclass: Int)

/** Minimal DNS wire-format support: read a query's first question; build an NXDOMAIN reply. */
object DnsMessage {
    const val HEADER = 12
    const val TYPE_A = 1
    const val TYPE_AAAA = 28
    const val RCODE_NXDOMAIN = 3

    /** The first question of a QUERY (QR=0), name lowercased without trailing dot; null if malformed. */
    fun parseQuestion(msg: ByteArray): DnsQuestion? {
        if (msg.size < HEADER) return null
        if ((Bytes.u16(msg, 2) and 0x8000) != 0) return null
        if (Bytes.u16(msg, 4) < 1) return null
        val (name, after) = readFirstName(msg) ?: return null
        if (after + 4 > msg.size) return null
        return DnsQuestion(name, Bytes.u16(msg, after), Bytes.u16(msg, after + 2))
    }

    /**
     * An NXDOMAIN response to [query]: same ID, QR=1, opcode and RD copied, RA=1, RCODE=3,
     * exactly one question echoed, no answer/authority/additional records.
     */
    fun blockedResponse(query: ByteArray): ByteArray? {
        if (parseQuestion(query) == null) return null
        val (_, after) = readFirstName(query) ?: return null
        val out = query.copyOfRange(0, after + 4)
        val flags = 0x8000 or (Bytes.u16(query, 2) and 0x7900) or 0x0080 or RCODE_NXDOMAIN
        Bytes.put16(out, 2, flags)
        Bytes.put16(out, 4, 1)
        Bytes.put16(out, 6, 0)
        Bytes.put16(out, 8, 0)
        Bytes.put16(out, 10, 0)
        return out
    }

    /** (name, offset just past the name's terminating zero). Compression pointers are rejected. */
    private fun readFirstName(msg: ByteArray): Pair<String, Int>? {
        var i = HEADER
        val sb = StringBuilder()
        var labels = 0
        while (true) {
            if (i >= msg.size) return null
            val len = msg[i].toInt() and 0xFF
            if (len == 0) return sb.toString().lowercase() to (i + 1)
            if ((len and 0xC0) != 0) return null
            labels++
            if (labels > 127 || i + 1 + len > msg.size) return null
            if (sb.isNotEmpty()) sb.append('.')
            sb.append(String(msg, i + 1, len, Charsets.US_ASCII))
            i += 1 + len
        }
    }
}
