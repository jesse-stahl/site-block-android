package today.siteblock.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BlockListTest {
    private val x = Catalog.byId("x")!!

    @Test fun addDomainNormalizesAndRejectsDuplicatesAndJunk() {
        val list = BlockList().withDomain("https://www.Reddit.com/r/all")!!
        assertEquals(listOf(BlockItem("site:reddit.com", "reddit.com", domains = listOf("reddit.com"), source = ItemSource.MANUAL_SITE)), list.items)
        assertNull(list.withDomain("reddit.com"))
        assertNull(list.withDomain("not a domain"))
    }

    @Test fun addAppValidatesAndRejectsDuplicatesAndNeverBlock() {
        val list = BlockList().withApp(" com.twitter.android ", "X", ItemSource.PICKER)!!
        assertEquals(BlockItem("app:com.twitter.android", "X", packages = listOf("com.twitter.android"), source = ItemSource.PICKER), list.items.single())
        assertNull(list.withApp("com.twitter.android", "X again", ItemSource.MANUAL_APP))
        assertNull(list.withApp("twitter", "bad", ItemSource.MANUAL_APP))
        assertNull(list.withApp("com.android.settings", "Settings", ItemSource.MANUAL_APP))
        assertNull(list.withApp(PackageId.SELF, "SiteBlock", ItemSource.MANUAL_APP))
    }

    @Test fun blankLabelFallsBackToTheId() {
        val list = BlockList().withApp("com.example.game", "  ", ItemSource.MANUAL_APP)!!
        assertEquals("com.example.game", list.items.single().label)
    }

    @Test fun builtinIsOneRowWithAllItsAppsAndSites() {
        val list = BlockList().withBuiltin(x)!!
        val item = list.items.single()
        assertEquals("builtin:x", item.id)
        assertEquals(x.packages, item.packages)
        assertEquals(x.domains, item.domains)
        assertNull(list.withBuiltin(x))
        assertTrue(list.coversPackage("com.twitter.android"))
        assertTrue(list.coversDomain("x.com"))
    }

    @Test fun removeDropsTheItemAndItsPause() {
        val list = BlockList().withBuiltin(x)!!.withPaused("builtin:x", true)!!
        val removed = list.without("builtin:x")!!
        assertTrue(removed.items.isEmpty())
        assertTrue(removed.pausedIds.isEmpty())
        assertNull(removed.without("builtin:x"))
    }

    @Test fun pauseAndResumeAreNoOpAwareAndActiveItemsSkipPaused() {
        val list = BlockList().withBuiltin(x)!!.withDomain("reddit.com")!!
        assertNull(list.withPaused("builtin:x", false))
        val paused = list.withPaused("builtin:x", true)!!
        assertTrue(paused.isPaused("builtin:x"))
        assertNull(paused.withPaused("builtin:x", true))
        assertNull(paused.withPaused("nope", true))
        assertEquals(listOf("site:reddit.com"), paused.activeItems().map { it.id })
        assertFalse(paused.withPaused("builtin:x", false)!!.isPaused("builtin:x"))
    }

    @Test fun normalizedDropsDanglingPausesDuplicateItemsAndClampsSettings() {
        val item = BlockItem("site:x.com", "x.com", domains = listOf("x.com"), source = ItemSource.MANUAL_SITE)
        val messy = BlockList(items = listOf(item, item), pausedIds = listOf("gone", "site:x.com", "site:x.com"), settings = FrictionSettings(0, 0))
        val clean = messy.normalized()
        assertEquals(listOf(item), clean.items)
        assertEquals(listOf("site:x.com"), clean.pausedIds)
        assertEquals(FrictionSettings(5, 2), clean.settings)
        assertNotNull(clean.item("site:x.com"))
    }
}
