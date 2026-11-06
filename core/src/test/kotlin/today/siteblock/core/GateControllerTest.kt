package today.siteblock.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GateControllerTest {
    private val min = 60_000L
    private val x = Catalog.byId("x")!!
    private val saved = mutableListOf<BlockList>()
    private fun gate(list: BlockList = BlockList(), boot: Int = 1) = GateController(list, boot) { saved += it }
    private val onWithX get() = BlockList(blockingOn = true).withBuiltin(x)!!

    @Test fun strengtheningIsInstantAndSaved() {
        val g = gate()
        assertTrue(g.turnOn())
        assertTrue(g.addBuiltin(x))
        assertTrue(g.addDomain("reddit.com"))
        assertTrue(g.addApp("com.example.game", "Game", ItemSource.MANUAL_APP))
        assertFalse(g.turnOn())
        assertTrue(g.list.blockingOn)
        assertEquals(3, g.list.items.size)
        assertEquals(g.list, saved.last())
        assertNull(g.pending)
    }

    @Test fun turnOffWaitsThenNeedsConfirmInsideTheWindow() {
        val g = gate(onWithX)
        assertTrue(g.requestTurnOff(0))
        assertEquals(PendingWeaken.TurnOff, g.pending)
        assertFalse(g.canConfirm(0))
        assertEquals(5 * min, g.remainingWait(0))
        assertFalse(g.completePending(PendingWeaken.TurnOff, 4 * min))
        assertTrue(g.list.blockingOn)
        assertEquals(PendingWeaken.TurnOff, g.pending)
        assertTrue(g.canConfirm(5 * min))
        assertEquals(2 * min, g.remainingConfirm(5 * min))
        assertTrue(g.completePending(PendingWeaken.TurnOff, 6 * min))
        assertFalse(g.list.blockingOn)
        assertNull(g.pending)
        assertFalse(saved.last().blockingOn)
    }

    @Test fun missingTheWindowDropsThePendingAction() {
        val g = gate(onWithX)
        g.requestTurnOff(0)
        g.tick(7 * min + 1)
        assertNull(g.pending)
        assertFalse(g.completePending(PendingWeaken.TurnOff, 7 * min + 2))
        assertTrue(g.list.blockingOn)
    }

    @Test fun lateConfirmFailsAndClears() {
        val g = gate(onWithX)
        g.requestTurnOff(0)
        assertFalse(g.completePending(PendingWeaken.TurnOff, 8 * min))
        assertNull(g.pending)
        assertTrue(g.list.blockingOn)
    }

    @Test fun onlyOneWeakenAtATimeAndExpectedMustMatch() {
        val g = gate(onWithX)
        assertTrue(g.requestTurnOff(0))
        assertFalse(g.requestRemove("builtin:x", 0))
        assertFalse(g.requestPauseFilter(PauseDuration.MIN_15, 0))
        assertFalse(g.completePending(PendingWeaken.Remove("builtin:x"), 6 * min))
        assertTrue(g.list.blockingOn)
    }

    @Test fun cannotRequestTurnOffWhenAlreadyOff() {
        assertFalse(gate().requestTurnOff(0))
    }

    @Test fun removeIsGatedEvenWhileOff() {
        val g = gate(onWithX.copy(blockingOn = false))
        assertFalse(g.requestRemove("missing", 0))
        assertTrue(g.requestRemove("builtin:x", 0))
        assertEquals(1, g.list.items.size)
        assertTrue(g.completePending(PendingWeaken.Remove("builtin:x"), 5 * min))
        assertTrue(g.list.items.isEmpty())
    }

    @Test fun pauseOnlyWhileOffButResumeAnytime() {
        val g = gate(onWithX)
        assertFalse(g.pause("builtin:x"))
        val off = gate(onWithX.copy(blockingOn = false))
        assertTrue(off.pause("builtin:x"))
        assertTrue(off.turnOn())
        assertTrue(off.resume("builtin:x"))
        assertFalse(off.list.isPaused("builtin:x"))
    }

    @Test fun timersOnlyWhileOffAndIdleAndNewLengthsApply() {
        val on = gate(onWithX)
        assertFalse(on.canChangeSettings)
        assertFalse(on.setWaitMinutes(10))

        val g = gate(onWithX.copy(blockingOn = false))
        assertTrue(g.canChangeSettings)
        assertTrue(g.setWaitMinutes(10))
        assertFalse(g.setWaitMinutes(10))
        assertFalse(g.setConfirmMinutes(1)) // clamps to 2 = unchanged
        assertEquals(FrictionSettings(10, 2), g.list.settings)
        g.requestRemove("builtin:x", 0)
        assertFalse(g.canChangeSettings)
        assertFalse(g.canConfirm(5 * min))
        assertTrue(g.canConfirm(10 * min))
    }

    @Test fun timedFilterPauseAppliesAndExpires() {
        val g = gate(onWithX)
        assertTrue(g.requestPauseFilter(PauseDuration.MIN_15, 0))
        assertTrue(g.completePending(PendingWeaken.PauseFilter(PauseDuration.MIN_15), 5 * min))
        assertEquals(FilterPause(untilMonotonic = 20 * min, bootCount = 1), g.list.filterPause)
        assertFalse(g.enforcement(6 * min).filterActive)
        assertEquals(setOf("com.twitter.android"), g.enforcement(6 * min).appPackages)
        g.tick(20 * min)
        assertNull(g.list.filterPause)
        assertNull(saved.last().filterPause)
        assertTrue(g.enforcement(20 * min).filterActive)
    }

    @Test fun indefiniteFilterPauseLastsUntilResumed() {
        val g = gate(onWithX)
        g.requestPauseFilter(PauseDuration.UNTIL_RESUMED, 0)
        g.completePending(PendingWeaken.PauseFilter(PauseDuration.UNTIL_RESUMED), 5 * min)
        g.tick(1_000 * min)
        assertTrue(g.enforcement(1_000 * min).filterPaused)
        assertTrue(g.resumeFilter())
        assertFalse(g.resumeFilter())
        assertTrue(g.enforcement(1_000 * min).filterActive)
    }

    @Test fun filterPauseNeedsARunningFilter() {
        val appOnly = BlockList(blockingOn = true).withApp("com.reddit.frontpage", "Reddit", ItemSource.PICKER)!!
        assertFalse(gate(appOnly).requestPauseFilter(PauseDuration.HOUR_1, 0))
        assertFalse(gate(onWithX.copy(blockingOn = false)).requestPauseFilter(PauseDuration.HOUR_1, 0))
    }

    @Test fun turningOffClearsAFilterPause() {
        val g = gate(onWithX.copy(filterPause = FilterPause(null, 1)))
        g.requestTurnOff(0)
        g.completePending(PendingWeaken.TurnOff, 5 * min)
        assertNull(g.list.filterPause)
    }

    @Test fun aRebootEndsATimedPause() {
        val g = gate(onWithX.copy(filterPause = FilterPause(untilMonotonic = 999 * min, bootCount = 1)), boot = 2)
        assertTrue(g.enforcement(0).filterActive)
        g.tick(0)
        assertNull(g.list.filterPause)
    }

    @Test fun cancelPendingResetsTheGate() {
        val g = gate(onWithX)
        g.requestTurnOff(0)
        g.cancelPending()
        assertNull(g.pending)
        assertNull(g.remainingWait(0))
        assertTrue(g.requestTurnOff(10 * min))
        assertEquals(5 * min, g.remainingWait(10 * min))
    }
}
