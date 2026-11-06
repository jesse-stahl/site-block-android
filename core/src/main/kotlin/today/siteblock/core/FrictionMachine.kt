package today.siteblock.core

/** State of a gated "weaken" action (turn blocking off, remove an item, pause the web filter). */
sealed interface FrictionState {
    /** No weaken attempt in progress. */
    data object Idle : FrictionState
    /** The wait is running; nothing can be confirmed until [deadline]. */
    data class Waiting(val deadline: Long) : FrictionState
    /** The confirm window is open until [windowEnds] (inclusive). */
    data class Confirming(val windowEnds: Long) : FrictionState
    /** The action was confirmed inside the window. */
    data object Confirmed : FrictionState
}

/**
 * Pure state machine for SiteBlock's asymmetric friction. Flow: Idle -> request() -> Waiting
 * (waitMillis) -> Confirming (confirmMillis) -> confirm() -> Confirmed. Missing the window drops
 * back to Idle. All `now` values are caller-supplied monotonic milliseconds, so the machine is
 * deterministic and the device clock can't be used to skip the wait. Strengthening actions never
 * touch this machine.
 */
class FrictionMachine(
    val waitMillis: Long = 5 * 60_000L,
    val confirmMillis: Long = 2 * 60_000L,
) {
    var state: FrictionState = FrictionState.Idle
        private set

    /** Start the wait. Only from Idle, so repeated taps never reset or extend it. */
    fun request(now: Long) {
        if (state == FrictionState.Idle) state = FrictionState.Waiting(now + waitMillis)
    }

    /** Apply any due time-based transitions. Call before reading [state] for display. */
    fun tick(now: Long) {
        state = resolved(now)
    }

    /** Succeeds only inside the confirm window; otherwise updates state and returns false. */
    fun confirm(now: Long): Boolean {
        state = resolved(now)
        if (state is FrictionState.Confirming) {
            state = FrictionState.Confirmed
            return true
        }
        return false
    }

    fun reset() {
        state = FrictionState.Idle
    }

    fun remainingWait(now: Long): Long? =
        (resolved(now) as? FrictionState.Waiting)?.let { maxOf(0L, it.deadline - now) }

    fun remainingConfirm(now: Long): Long? =
        (resolved(now) as? FrictionState.Confirming)?.let { maxOf(0L, it.windowEnds - now) }

    fun canConfirm(now: Long): Boolean = resolved(now) is FrictionState.Confirming

    private fun resolved(now: Long): FrictionState = when (val s = state) {
        FrictionState.Idle, FrictionState.Confirmed -> s
        is FrictionState.Waiting ->
            if (now >= s.deadline) {
                val windowEnds = s.deadline + confirmMillis
                if (now > windowEnds) FrictionState.Idle else FrictionState.Confirming(windowEnds)
            } else {
                s
            }
        is FrictionState.Confirming -> if (now > s.windowEnds) FrictionState.Idle else s
    }
}
