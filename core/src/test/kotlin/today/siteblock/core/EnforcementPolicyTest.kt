package today.siteblock.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EnforcementPolicyTest {
    private val x = Catalog.byId("x")!!
    private val withX = BlockList(blockingOn = true).withBuiltin(x)!!

    @Test fun offEnforcesNothing() {
        assertEquals(Enforcement.OFF, EnforcementPolicy.compute(withX.copy(blockingOn = false), 0, 1))
        assertFalse(Enforcement.OFF.guardActive)
    }

    @Test fun onEnforcesAppsSitesAndDohProviders() {
        val e = EnforcementPolicy.compute(withX, 0, 1)
        assertTrue(e.blockingOn && e.guardActive && e.filterActive && e.guardNetworkSettings)
        assertFalse(e.filterPaused)
        assertEquals(setOf("com.twitter.android"), e.appPackages)
        assertTrue(e.filterDomains.containsAll(x.domains))
        assertTrue(e.filterDomains.containsAll(DohProviders.hostnames))
    }

    @Test fun pausedItemsAreNotEnforced() {
        val list = withX.withDomain("reddit.com")!!.copy(pausedIds = listOf("builtin:x"))
        val e = EnforcementPolicy.compute(list, 0, 1)
        assertTrue(e.appPackages.isEmpty())
        assertTrue("reddit.com" in e.filterDomains)
        assertFalse("x.com" in e.filterDomains)
    }

    @Test fun appOnlyListDoesNotRunTheFilter() {
        val list = BlockList(blockingOn = true).withApp("com.reddit.frontpage", "Reddit", ItemSource.PICKER)!!
        val e = EnforcementPolicy.compute(list, 0, 1)
        assertFalse(e.filterActive)
        assertTrue(e.filterDomains.isEmpty())
        assertEquals(setOf("com.reddit.frontpage"), e.appPackages)
    }

    @Test fun timedPauseHoldsUntilDeadlineInTheSameBoot() {
        val list = withX.copy(filterPause = FilterPause(untilMonotonic = 1_000, bootCount = 1))
        val during = EnforcementPolicy.compute(list, 999, 1)
        assertTrue(during.filterPaused)
        assertFalse(during.filterActive)
        assertTrue(during.filterDomains.isEmpty())
        assertEquals(setOf("com.twitter.android"), during.appPackages)
        assertTrue(EnforcementPolicy.compute(list, 1_000, 1).filterActive)
    }

    @Test fun rebootEndsATimedPause() {
        val list = withX.copy(filterPause = FilterPause(untilMonotonic = 1_000_000, bootCount = 1))
        assertTrue(EnforcementPolicy.compute(list, 5, 2).filterActive)
        assertFalse(EnforcementPolicy.isFilterPaused(list, 5, 2))
    }

    @Test fun indefinitePauseSurvivesReboots() {
        val list = withX.copy(filterPause = FilterPause(untilMonotonic = null, bootCount = 1))
        assertTrue(EnforcementPolicy.compute(list, Long.MAX_VALUE, 99).filterPaused)
    }
}
