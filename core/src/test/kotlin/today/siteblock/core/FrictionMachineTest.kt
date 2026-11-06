package today.siteblock.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FrictionMachineTest {
    private val min = 60_000L
    private fun machine() = FrictionMachine(waitMillis = 5 * min, confirmMillis = 2 * min)

    @Test fun startsIdle() {
        assertEquals(FrictionState.Idle, machine().state)
    }

    @Test fun requestStartsWaiting() {
        val m = machine()
        m.request(0)
        assertEquals(FrictionState.Waiting(5 * min), m.state)
    }

    @Test fun repeatedRequestDoesNotExtendTheWait() {
        val m = machine()
        m.request(0)
        m.request(3 * min)
        assertEquals(FrictionState.Waiting(5 * min), m.state)
    }

    @Test fun cannotConfirmDuringWait() {
        val m = machine()
        m.request(0)
        assertFalse(m.canConfirm(4 * min))
        assertFalse(m.confirm(4 * min))
        assertEquals(FrictionState.Waiting(5 * min), m.state)
    }

    @Test fun windowOpensWhenWaitEnds() {
        val m = machine()
        m.request(0)
        m.tick(5 * min)
        assertEquals(FrictionState.Confirming(7 * min), m.state)
        assertTrue(m.canConfirm(5 * min))
    }

    @Test fun confirmInsideWindowSucceeds() {
        val m = machine()
        m.request(0)
        assertTrue(m.confirm(6 * min))
        assertEquals(FrictionState.Confirmed, m.state)
    }

    @Test fun windowEdgeIsInclusive() {
        val m = machine()
        m.request(0)
        assertTrue(m.confirm(7 * min))
    }

    @Test fun missingTheWindowFallsBackToIdle() {
        val m = machine()
        m.request(0)
        assertFalse(m.confirm(7 * min + 1))
        assertEquals(FrictionState.Idle, m.state)
    }

    @Test fun jumpingPastWaitAndWindowGoesIdle() {
        val m = machine()
        m.request(0)
        m.tick(60 * min)
        assertEquals(FrictionState.Idle, m.state)
    }

    @Test fun remainingTimes() {
        val m = machine()
        m.request(0)
        assertEquals(5 * min, m.remainingWait(0))
        assertNull(m.remainingConfirm(0))
        assertNull(m.remainingWait(5 * min))
        assertEquals(min, m.remainingConfirm(6 * min))
    }

    @Test fun resetReturnsToIdle() {
        val m = machine()
        m.request(0)
        m.reset()
        assertEquals(FrictionState.Idle, m.state)
    }
}
