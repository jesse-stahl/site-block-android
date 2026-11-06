package today.siteblock.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AttemptLogTest {
    @Test fun countsPerPackagePerDay() {
        val log = AttemptLog().recorded("com.twitter.android", "2026-11-06")
            .recorded("com.twitter.android", "2026-11-06")
            .recorded("com.reddit.frontpage", "2026-11-06")
        assertEquals(2, log.countFor("com.twitter.android", "2026-11-06"))
        assertEquals(1, log.countFor("com.reddit.frontpage", "2026-11-06"))
        assertEquals(0, log.countFor("com.instagram.android", "2026-11-06"))
    }

    @Test fun aNewDayStartsFresh() {
        val log = AttemptLog().recorded("a.b", "2026-11-06").recorded("a.b", "2026-11-07")
        assertEquals("2026-11-07", log.day)
        assertEquals(1, log.countFor("a.b", "2026-11-07"))
        assertEquals(0, log.countFor("a.b", "2026-11-06"))
    }

    @Test fun codecRoundTripsAndToleratesGarbage() {
        val log = AttemptLog().recorded("a.b", "2026-11-06")
        assertEquals(log, AttemptLog.decode(AttemptLog.encode(log)))
        assertEquals(AttemptLog(), AttemptLog.decode("not json"))
    }

    @Test fun coalescerCountsOncePerWindow() {
        val c = AttemptCoalescer(windowMillis = 3_000)
        assertTrue(c.shouldCount("a.b", 0))
        assertFalse(c.shouldCount("a.b", 2_999))
        assertTrue(c.shouldCount("c.d", 2_999))
        assertTrue(c.shouldCount("a.b", 3_000))
    }
}
