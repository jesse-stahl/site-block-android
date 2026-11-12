package today.siteblock.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WhitelistTest {
    private val min = 60_000L
    private val x = Catalog.byId("x")!!
    private val saved = mutableListOf<BlockList>()
    private fun gate(list: BlockList) = GateController(list, 1) { saved += it }
    private val onWithX get() = BlockList(blockingOn = true).withBuiltin(x)!!
    private val signal = PendingWeaken.Whitelist("org.thoughtcrime.securesms", "Signal")

    // ---- BlockList ----

    @Test fun whatsAppIsWhitelistedByDefault() {
        assertEquals(listOf("com.whatsapp", "com.whatsapp.w4b"), BlockList().whitelist.map { it.packageId })
        assertTrue(BlockList().isWhitelisted("com.whatsapp"))
    }

    @Test fun addValidatesAndRejectsDuplicatesAndSiteBlockItself() {
        val list = BlockList(whitelist = emptyList())
        val added = list.withWhitelisted("  org.thoughtcrime.securesms ", " Signal ")!!
        assertEquals(listOf(WhitelistedApp("org.thoughtcrime.securesms", "Signal")), added.whitelist)
        assertNull(added.withWhitelisted("org.thoughtcrime.securesms", "Signal"))
        assertNull(list.withWhitelisted("not a package", "Bad"))
        assertNull(list.withWhitelisted(PackageId.SELF, "SiteBlock"))
        assertEquals("com.example.app", list.withWhitelisted("com.example.app", "  ")!!.whitelist.single().label)
    }

    @Test fun removeOnlyWhatIsThere() {
        val list = BlockList()
        assertEquals(listOf("com.whatsapp.w4b"), list.withoutWhitelisted("com.whatsapp")!!.whitelist.map { it.packageId })
        assertNull(list.withoutWhitelisted("com.example.missing"))
    }

    @Test fun normalizeDropsDuplicatesAndInvalidEntries() {
        val messy = BlockList(
            whitelist = listOf(
                WhitelistedApp("com.whatsapp", "WhatsApp"),
                WhitelistedApp("com.whatsapp", "Again"),
                WhitelistedApp("bad id", "Bad"),
                WhitelistedApp(PackageId.SELF, "SiteBlock"),
            ),
        )
        assertEquals(listOf(WhitelistedApp("com.whatsapp", "WhatsApp")), messy.normalized().whitelist)
    }

    @Test fun codecRoundTripsAndOldFilesGetTheDefault() {
        val list = BlockList(whitelist = emptyList()).withWhitelisted("org.thoughtcrime.securesms", "Signal")!!
        assertEquals(list, BlockListCodec.decode(BlockListCodec.encode(list)))
        assertEquals(BlockList().whitelist, BlockListCodec.decode("""{"blockingOn":true}""").whitelist)
        assertEquals(emptyList<WhitelistedApp>(), BlockListCodec.decode("""{"whitelist":[]}""").whitelist)
    }

    // ---- Gate: adding weakens (gated, even while off); removing strengthens (instant) ----

    @Test fun addingIsGatedWhileOn() {
        val g = gate(onWithX)
        assertTrue(g.requestWhitelist("org.thoughtcrime.securesms", "Signal", 0))
        assertEquals(signal, g.pending)
        assertFalse(g.list.isWhitelisted("org.thoughtcrime.securesms"))
        assertFalse(g.completePending(signal, 4 * min))
        assertTrue(g.completePending(signal, 5 * min))
        assertTrue(g.list.isWhitelisted("org.thoughtcrime.securesms"))
        assertTrue(saved.last().isWhitelisted("org.thoughtcrime.securesms"))
        assertNull(g.pending)
    }

    @Test fun addingIsGatedEvenWhileOff() {
        val g = gate(onWithX.copy(blockingOn = false))
        assertTrue(g.requestWhitelist("org.thoughtcrime.securesms", "Signal", 0))
        assertFalse(g.list.isWhitelisted("org.thoughtcrime.securesms"))
        assertTrue(g.completePending(signal, 5 * min))
        assertTrue(g.list.isWhitelisted("org.thoughtcrime.securesms"))
    }

    @Test fun invalidOrDuplicateRequestsNeverStartTheGate() {
        val g = gate(onWithX)
        assertFalse(g.requestWhitelist("com.whatsapp", "WhatsApp", 0))
        assertFalse(g.requestWhitelist("bad id", "Bad", 0))
        assertFalse(g.requestWhitelist(PackageId.SELF, "SiteBlock", 0))
        assertNull(g.pending)
    }

    @Test fun pendingIdIsNormalizedSoConfirmMatches() {
        val g = gate(onWithX)
        assertTrue(g.requestWhitelist(" org.thoughtcrime.securesms ", " Signal ", 0))
        assertEquals(signal, g.pending)
    }

    @Test fun oneWeakenAtATime() {
        val g = gate(onWithX)
        assertTrue(g.requestTurnOff(0))
        assertFalse(g.requestWhitelist("org.thoughtcrime.securesms", "Signal", 0))
    }

    @Test fun removingIsInstantEvenWhileOn() {
        val g = gate(onWithX)
        assertTrue(g.removeFromWhitelist("com.whatsapp"))
        assertFalse(g.list.isWhitelisted("com.whatsapp"))
        assertFalse(saved.last().isWhitelisted("com.whatsapp"))
        assertFalse(g.removeFromWhitelist("com.whatsapp"))
    }

    // ---- Enforcement ----

    @Test fun runningFilterExemptsWhitelistedApps() {
        val e = EnforcementPolicy.compute(onWithX, 0, 1)
        assertEquals(setOf("com.whatsapp", "com.whatsapp.w4b"), e.vpnExemptPackages)
    }

    @Test fun noFilterNoExemptions() {
        assertEquals(emptySet<String>(), EnforcementPolicy.compute(onWithX.copy(blockingOn = false), 0, 1).vpnExemptPackages)
        val appOnly = BlockList(blockingOn = true).withApp("com.reddit.frontpage", "Reddit", ItemSource.PICKER)!!
        assertEquals(emptySet<String>(), EnforcementPolicy.compute(appOnly, 0, 1).vpnExemptPackages)
    }
}
