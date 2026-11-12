package today.siteblock.core

/** A weaken action waiting for the friction gate. */
sealed interface PendingWeaken {
    data object TurnOff : PendingWeaken
    data class Remove(val itemId: String) : PendingWeaken
    data class PauseFilter(val duration: PauseDuration) : PendingWeaken
    data class Whitelist(val packageId: String, val label: String) : PendingWeaken
}

/**
 * All of SiteBlock's action rules in one place (the Android port of the Mac AppModel's logic).
 * Strengthen = instant. Weaken = request -> wait -> confirm window -> PIN -> completePending.
 * Not thread-safe: confine to one thread (the app uses the main thread). [bootCount] identifies
 * the current boot so timed filter pauses end on reboot. Every list change is passed to
 * [onChange] for persistence.
 */
class GateController(
    initial: BlockList,
    private val bootCount: Int,
    private val onChange: (BlockList) -> Unit,
) {
    var list: BlockList = initial.normalized()
        private set

    var pending: PendingWeaken? = null
        private set

    private var machine = machineFor(list.settings)

    /** Timers can change only while blocking is off and no weaken is mid-countdown. */
    val canChangeSettings: Boolean get() = !list.blockingOn && pending == null

    // ---- strengthen: instant ----

    fun turnOn(): Boolean = commitIf(if (list.blockingOn) null else list.copy(blockingOn = true))

    fun addDomain(raw: String): Boolean = commitIf(list.withDomain(raw))

    fun addApp(packageId: String, label: String, source: ItemSource): Boolean =
        commitIf(list.withApp(packageId, label, source))

    fun addBuiltin(entry: CatalogEntry): Boolean = commitIf(list.withBuiltin(entry))

    fun resume(itemId: String): Boolean = commitIf(list.withPaused(itemId, false))

    fun resumeFilter(): Boolean = commitIf(if (list.filterPause == null) null else list.copy(filterPause = null))

    fun removeFromWhitelist(packageId: String): Boolean = commitIf(list.withoutWhitelisted(packageId))

    // ---- allowed only while off (blocking off already paid the gate) ----

    fun pause(itemId: String): Boolean = commitIf(if (list.blockingOn) null else list.withPaused(itemId, true))

    fun setWaitMinutes(minutes: Int): Boolean =
        updateSettings(FrictionSettings.of(minutes, list.settings.confirmMinutes))

    fun setConfirmMinutes(minutes: Int): Boolean =
        updateSettings(FrictionSettings.of(list.settings.waitMinutes, minutes))

    // ---- weaken: gated ----

    fun requestTurnOff(now: Long): Boolean = begin(if (list.blockingOn) PendingWeaken.TurnOff else null, now)

    fun requestRemove(itemId: String, now: Long): Boolean =
        begin(if (list.item(itemId) != null) PendingWeaken.Remove(itemId) else null, now)

    fun requestPauseFilter(duration: PauseDuration, now: Long): Boolean =
        begin(if (enforcement(now).filterActive) PendingWeaken.PauseFilter(duration) else null, now)

    /** Gated even while blocking is off, like Remove: the hole would outlast turning blocking back on. */
    fun requestWhitelist(packageId: String, label: String, now: Long): Boolean {
        val added = list.withWhitelisted(packageId, label)?.whitelist?.last()
        return begin(added?.let { PendingWeaken.Whitelist(it.packageId, it.label) }, now)
    }

    fun cancelPending() {
        pending = null
        machine.reset()
    }

    /** Apply due transitions: drop a pending action whose window closed; clear an expired/stale filter pause. */
    fun tick(now: Long) {
        machine.tick(now)
        if (pending != null && machine.state == FrictionState.Idle) pending = null
        if (list.filterPause != null && !EnforcementPolicy.isFilterPaused(list, now, bootCount)) {
            commit(list.copy(filterPause = null))
        }
    }

    fun canConfirm(now: Long): Boolean = pending != null && machine.canConfirm(now)

    fun remainingWait(now: Long): Long? = if (pending == null) null else machine.remainingWait(now)

    fun remainingConfirm(now: Long): Long? = if (pending == null) null else machine.remainingConfirm(now)

    /**
     * Call ONLY after the device-credential prompt succeeded. Applies [expected] if it is still the
     * pending action and the confirm window is still open at [now] (the prompt took time).
     */
    fun completePending(expected: PendingWeaken, now: Long): Boolean {
        if (pending != expected) return false
        if (!machine.confirm(now)) {
            if (machine.state == FrictionState.Idle) pending = null
            return false
        }
        pending = null
        machine.reset()
        when (expected) {
            PendingWeaken.TurnOff -> commit(list.copy(blockingOn = false, filterPause = null))
            is PendingWeaken.Remove -> list.without(expected.itemId)?.let { commit(it) }
            is PendingWeaken.PauseFilter -> commit(
                list.copy(filterPause = FilterPause(expected.duration.millis?.let { now + it }, bootCount)),
            )
            is PendingWeaken.Whitelist -> list.withWhitelisted(expected.packageId, expected.label)?.let { commit(it) }
        }
        return true
    }

    fun enforcement(now: Long): Enforcement = EnforcementPolicy.compute(list, now, bootCount)

    private fun begin(action: PendingWeaken?, now: Long): Boolean {
        if (action == null || pending != null) return false
        pending = action
        machine.reset()
        machine.request(now)
        return true
    }

    private fun updateSettings(next: FrictionSettings): Boolean {
        if (!canChangeSettings || next == list.settings) return false
        commit(list.copy(settings = next))
        machine = machineFor(next)
        return true
    }

    private fun commitIf(next: BlockList?): Boolean {
        if (next == null) return false
        commit(next)
        return true
    }

    private fun commit(next: BlockList) {
        list = next
        onChange(next)
    }

    private fun machineFor(s: FrictionSettings) = FrictionMachine(s.waitMillis, s.confirmMillis)
}
