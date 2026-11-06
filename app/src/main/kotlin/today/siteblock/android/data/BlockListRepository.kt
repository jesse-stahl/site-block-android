package today.siteblock.android.data

import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.SystemClock
import android.provider.Settings
import java.io.File
import java.io.IOException
import java.time.LocalDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import today.siteblock.android.system.PrivateDnsCheck
import today.siteblock.android.system.ServiceNames
import today.siteblock.core.AttemptCoalescer
import today.siteblock.core.AttemptLog
import today.siteblock.core.BlockList
import today.siteblock.core.BlockListCodec
import today.siteblock.core.CatalogEntry
import today.siteblock.core.Enforcement
import today.siteblock.core.GateController
import today.siteblock.core.ItemSource
import today.siteblock.core.PauseDuration
import today.siteblock.core.PendingWeaken

/**
 * Single owner of SiteBlock's state. MAIN-THREAD ONLY for every public method. Persists the
 * block list to filesDir/blocklist.json on every change, ticks the friction gate, publishes
 * [state] for the UI and [enforcement] for the services, and starts/stops the VPN service when
 * the web filter should switch on or off.
 */
class BlockListRepository(
    private val context: Context,
    private val clock: () -> Long = { SystemClock.elapsedRealtime() },
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val listFile = JsonFile(File(context.filesDir, "blocklist.json"))
    private val attemptsFile = JsonFile(File(context.filesDir, "attempts.json"))
    private val bootCount = Settings.Global.getInt(context.contentResolver, Settings.Global.BOOT_COUNT, 0)
    private val wake = Channel<Unit>(Channel.CONFLATED)
    private val coalescer = AttemptCoalescer()

    private var problem: String? = null
    private var runtime = RuntimeStatus()
    private var lastFilterActive: Boolean? = null
    private var attempts: AttemptLog = loadAttempts()
    private val controller = GateController(loadList(), bootCount) { save(it) }

    private val _enforcement = MutableStateFlow(controller.enforcement(clock()))
    /** What the services must enforce right now. Safe to read from any thread. */
    val enforcement: StateFlow<Enforcement> = _enforcement.asStateFlow()

    private val _state = MutableStateFlow(buildState(clock()))
    val state: StateFlow<UiState> = _state.asStateFlow()

    /** Start ticking: every second while a gate is pending, otherwise every 15 s (or when poked). */
    fun start() {
        scope.launch {
            while (isActive) {
                publish()
                val waitMs = if (controller.pending != null) 1_000L else 15_000L
                withTimeoutOrNull(waitMs) { wake.receive() }
            }
        }
    }

    // ---- strengthen (instant) ----
    fun turnOn(): Boolean = act { controller.turnOn() }
    fun addDomain(raw: String): Boolean = act { controller.addDomain(raw) }
    fun addApp(packageId: String, label: String, source: ItemSource): Boolean = act { controller.addApp(packageId, label, source) }
    fun addBuiltin(entry: CatalogEntry): Boolean = act { controller.addBuiltin(entry) }
    fun resume(itemId: String): Boolean = act { controller.resume(itemId) }
    fun resumeFilter(): Boolean = act { controller.resumeFilter() }

    // ---- only while off ----
    fun pause(itemId: String): Boolean = act { controller.pause(itemId) }
    fun setWaitMinutes(minutes: Int): Boolean = act { controller.setWaitMinutes(minutes) }
    fun setConfirmMinutes(minutes: Int): Boolean = act { controller.setConfirmMinutes(minutes) }

    // ---- weaken (gated) ----
    fun requestTurnOff(): Boolean = act { controller.requestTurnOff(clock()) }
    fun requestRemove(itemId: String): Boolean = act { controller.requestRemove(itemId, clock()) }
    fun requestPauseFilter(duration: PauseDuration): Boolean = act { controller.requestPauseFilter(duration, clock()) }
    fun cancelPending(): Boolean = act { controller.cancelPending(); true }
    fun canConfirmNow(): Boolean = controller.canConfirm(clock())

    /** Call only after the PIN prompt succeeded. */
    fun completePending(expected: PendingWeaken): Boolean = act { controller.completePending(expected, clock()) }

    /** Re-push enforcement to the system (e.g. after the user grants a permission, or on app resume). */
    fun retryEnforcement() {
        lastFilterActive = null
        poke()
    }

    // ---- attempts ----

    /** Record a blocked open of [packageId] (coalesced) and return today's count for it. */
    fun recordAttempt(packageId: String): Int {
        val today = LocalDate.now().toString()
        if (coalescer.shouldCount(packageId, clock())) {
            attempts = attempts.recorded(packageId, today)
            try {
                attemptsFile.write(AttemptLog.encode(attempts))
            } catch (e: IOException) {
                // Stats are best-effort; blocking never depends on them.
            }
            poke()
        }
        return attempts.countFor(packageId, today)
    }

    /** The row label that blocks [packageId], or the ID itself. */
    fun labelFor(packageId: String): String =
        controller.list.items.firstOrNull { packageId in it.packages }?.label ?: packageId

    // ---- runtime status reported by services ----

    fun setVpnRunning(running: Boolean, error: String?) {
        runtime = runtime.copy(vpnRunning = running, vpnError = error)
        poke()
    }

    fun setAccessibilityConnected(connected: Boolean) {
        runtime = runtime.copy(accessibilityConnected = connected)
        poke()
    }

    // ---- internals ----

    private inline fun act(block: () -> Boolean): Boolean {
        val result = block()
        poke()
        return result
    }

    private fun poke() {
        publish()
        wake.trySend(Unit)
    }

    private fun publish() {
        val now = clock()
        controller.tick(now)
        val e = controller.enforcement(now)
        _enforcement.value = e
        _state.value = buildState(now)
        syncVpn(e)
    }

    private fun buildState(now: Long): UiState {
        val list = controller.list
        val e = controller.enforcement(now)
        val today = LocalDate.now().toString()
        return UiState(
            list = list,
            pending = controller.pending,
            remainingWaitMs = controller.remainingWait(now),
            remainingConfirmMs = controller.remainingConfirm(now),
            canConfirm = controller.canConfirm(now),
            canChangeSettings = controller.canChangeSettings,
            enforcement = e,
            filterPauseRemainingMs = list.filterPause?.untilMonotonic?.takeIf { e.filterPaused }?.let { it - now },
            runtime = runtime,
            privateDnsStrict = PrivateDnsCheck.isStrict(context),
            problem = problem,
            attemptsToday = if (attempts.day == today) attempts.counts else emptyMap(),
        )
    }

    /** Start or stop the VPN service when the filter's on/off intent changes (or on retry). */
    private fun syncVpn(e: Enforcement) {
        if (e.filterActive == lastFilterActive) return
        lastFilterActive = e.filterActive
        if (e.filterActive) {
            if (VpnService.prepare(context) != null) {
                runtime = runtime.copy(vpnRunning = false, vpnError = "VPN permission not granted. Open Setup to allow it.")
                _state.value = buildState(clock())
                return
            }
            sendToVpn(ServiceNames.ACTION_APPLY)
        } else if (runtime.vpnRunning) {
            sendToVpn(ServiceNames.ACTION_STOP)
        }
    }

    private fun sendToVpn(action: String) {
        try {
            context.startService(Intent(action).setClassName(context, ServiceNames.VPN_SERVICE))
        } catch (e: IllegalStateException) {
            // Background-start restriction. Opening the app retries via retryEnforcement().
            runtime = runtime.copy(vpnRunning = false, vpnError = "Couldn't start the web filter in the background. Open SiteBlock to retry.")
            _state.value = buildState(clock())
        } catch (e: SecurityException) {
            runtime = runtime.copy(vpnRunning = false, vpnError = "Android refused to start the web filter: ${e.message}")
            _state.value = buildState(clock())
        }
    }

    private fun save(list: BlockList) {
        problem = try {
            listFile.write(BlockListCodec.encode(list))
            null
        } catch (e: IOException) {
            "Couldn't save your block list: ${e.message}"
        }
    }

    private fun loadList(): BlockList {
        val text = try {
            listFile.read()
        } catch (e: IOException) {
            // Retry once; if it still fails, keep a copy so the next save can't silently destroy the list.
            try {
                listFile.read()
            } catch (again: IOException) {
                listFile.backupTo("blocklist.unreadable.json")
                problem = "Couldn't read your block list (${again.message}). A copy is kept as blocklist.unreadable.json."
                return BlockList()
            }
        } ?: return BlockList()
        return try {
            BlockListCodec.decode(text)
        } catch (e: IllegalArgumentException) {
            listFile.backupTo("blocklist.corrupt.json")
            problem = "Your saved block list couldn't be read and was reset. A copy is kept as blocklist.corrupt.json."
            BlockList()
        }
    }

    private fun loadAttempts(): AttemptLog =
        try {
            attemptsFile.read()?.let { AttemptLog.decode(it) } ?: AttemptLog()
        } catch (e: IOException) {
            AttemptLog()
        }
}
