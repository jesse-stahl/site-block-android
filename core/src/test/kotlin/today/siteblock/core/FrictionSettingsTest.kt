package today.siteblock.core

import org.junit.Assert.assertEquals
import org.junit.Test

class FrictionSettingsTest {
    @Test fun defaultsAreFiveAndTwoMinutes() {
        val s = FrictionSettings()
        assertEquals(5, s.waitMinutes)
        assertEquals(2, s.confirmMinutes)
        assertEquals(300_000L, s.waitMillis)
        assertEquals(120_000L, s.confirmMillis)
    }

    @Test fun ofClampsToAllowedRanges() {
        assertEquals(FrictionSettings(5, 2), FrictionSettings.of(1, 0))
        assertEquals(FrictionSettings(120, 30), FrictionSettings.of(500, 99))
        assertEquals(FrictionSettings(15, 3), FrictionSettings.of(15, 3))
    }

    @Test fun clampedFixesOutOfRangeValues() {
        assertEquals(FrictionSettings(5, 2), FrictionSettings(waitMinutes = 0, confirmMinutes = -1).clamped())
    }
}
