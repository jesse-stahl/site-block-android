package today.siteblock.core

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DnsMessageTest {
    @Test fun parsesAndLowercasesTheFirstQuestion() {
        val q = DnsMessage.parseQuestion(dnsQuery(0x1234, "API.x.com", DnsMessage.TYPE_AAAA))!!
        assertEquals("api.x.com", q.name)
        assertEquals(DnsMessage.TYPE_AAAA, q.type)
        assertEquals(1, q.qclass)
    }

    @Test fun ignoresResponses() {
        assertNull(DnsMessage.parseQuestion(dnsQuery(1, "x.com", flags = 0x8180)))
    }

    @Test fun rejectsTruncatedCompressedAndTinyMessages() {
        val q = dnsQuery(1, "x.com")
        assertNull(DnsMessage.parseQuestion(q.copyOf(15)))
        assertNull(DnsMessage.parseQuestion(q.copyOf().also { it[12] = 0xC0.toByte() }))
        assertNull(DnsMessage.parseQuestion(ByteArray(5)))
    }

    @Test fun blockedResponseIsNxdomainEchoingTheQuestion() {
        val q = dnsQuery(0xBEEF, "x.com")
        val r = DnsMessage.blockedResponse(q)!!
        assertEquals(q.size, r.size)
        assertEquals(0xBEEF, Bytes.u16(r, 0))
        assertEquals(0x8183, Bytes.u16(r, 2)) // QR | RD | RA | RCODE=3
        assertEquals(1, Bytes.u16(r, 4))
        assertEquals(0, Bytes.u16(r, 6))
        assertEquals(0, Bytes.u16(r, 8))
        assertEquals(0, Bytes.u16(r, 10))
        assertArrayEquals(q.copyOfRange(12, q.size), r.copyOfRange(12, r.size))
    }

    @Test fun blockedResponseDropsAdditionalRecords() {
        val q = dnsQuery(1, "x.com") + ByteArray(11)
        Bytes.put16(q, 10, 1) // pretend an EDNS OPT record follows
        val r = DnsMessage.blockedResponse(q)!!
        assertEquals(q.size - 11, r.size)
        assertEquals(0, Bytes.u16(r, 10))
    }

    @Test fun blockedResponseOfGarbageIsNull() {
        assertNull(DnsMessage.blockedResponse(ByteArray(3)))
    }

    @Test fun detectsAndSetsTheTruncatedFlag() {
        val reply = dnsQuery(5, "x.com", flags = 0x8180)
        assertEquals(false, DnsMessage.isTruncated(reply))
        val marked = DnsMessage.withTruncatedFlag(reply)
        assertEquals(true, DnsMessage.isTruncated(marked))
        assertEquals(0x8380, Bytes.u16(marked, 2))
        assertEquals(0x8180, Bytes.u16(reply, 2)) // original untouched
        assertEquals(false, DnsMessage.isTruncated(ByteArray(2)))
    }
}
