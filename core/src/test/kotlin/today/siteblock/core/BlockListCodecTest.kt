package today.siteblock.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class BlockListCodecTest {
    @Test fun roundTrips() {
        val list = BlockList(blockingOn = true, settings = FrictionSettings(10, 3))
            .withBuiltin(Catalog.byId("x")!!)!!
            .withApp("com.reddit.frontpage", "Reddit", ItemSource.PICKER)!!
            .withPaused("app:com.reddit.frontpage", true)!!
            .copy(filterPause = FilterPause(untilMonotonic = 123, bootCount = 4))
        assertEquals(list, BlockListCodec.decode(BlockListCodec.encode(list)))
    }

    @Test fun missingFieldsTakeDefaultsAndUnknownKeysAreIgnored() {
        assertEquals(BlockList(), BlockListCodec.decode("""{"future":"field"}"""))
    }

    @Test fun decodeNormalizes() {
        val text = """{"items":[],"pausedIds":["ghost"],"settings":{"waitMinutes":1,"confirmMinutes":1}}"""
        val list = BlockListCodec.decode(text)
        assertEquals(emptyList<String>(), list.pausedIds)
        assertEquals(FrictionSettings(5, 2), list.settings)
    }

    @Test fun malformedTextThrows() {
        assertThrows(IllegalArgumentException::class.java) { BlockListCodec.decode("{not json") }
    }
}
