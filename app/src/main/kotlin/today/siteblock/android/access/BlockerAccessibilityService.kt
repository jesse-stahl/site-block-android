package today.siteblock.android.access

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import today.siteblock.android.block.BlockActivity
import today.siteblock.android.repository
import today.siteblock.android.system.Notifier
import today.siteblock.core.Enforcement
import today.siteblock.core.GuardRules

/** Blocks listed apps by package name (installed or reinstalled alike) and guards SiteBlock's off-switches. */
class BlockerAccessibilityService : AccessibilityService() {

    private val scope = MainScope()
    private var lastGuardCheck = 0L
    private var lastToast = 0L
    private val handler = Handler(Looper.getMainLooper())
    private val trailingGuardCheck = Runnable { guardCheck() }

    override fun onServiceConnected() {
        super.onServiceConnected()
        repository.setAccessibilityConnected(true)
        Notifier.clear(this, Notifier.ID_ACCESSIBILITY)
        // When blocking turns on or the list grows, catch an already-open blocked app.
        scope.launch { repository.enforcement.collect { checkActiveWindow() } }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val pkg = event.packageName?.toString() ?: return
        if (pkg == packageName) return
        val enforcement = repository.enforcement.value
        if (!enforcement.blockingOn) return

        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED && pkg in enforcement.appPackages) {
            block(pkg)
            return
        }

        if (pkg !in GuardRules.guardedPackages) return
        // Always re-check once the screen settles: the first event can arrive before the page's
        // text is bound, and throttled content events would otherwise never be looked at.
        handler.removeCallbacks(trailingGuardCheck)
        handler.postDelayed(trailingGuardCheck, GUARD_THROTTLE_MS)
        val now = SystemClock.uptimeMillis()
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED && now - lastGuardCheck < GUARD_THROTTLE_MS) return
        lastGuardCheck = now
        guardCheck()
    }

    /** Inspect whatever window is active now (not the event's, which may be stale). */
    private fun guardCheck() {
        val enforcement = repository.enforcement.value
        if (!enforcement.blockingOn) return
        val root = rootInActiveWindow ?: return
        val pkg = root.packageName?.toString() ?: return
        if (pkg !in GuardRules.guardedPackages) return
        if (GuardRules.verdict(pkg, collectTexts(root), guardView(enforcement)) == GuardRules.Verdict.BOUNCE) bounce()
    }

    /**
     * Guard VPN / Private DNS screens only while the filter is actually running. If it should run
     * but isn't (e.g. VPN consent was lost), the user must be able to reach the consent dialog and
     * VPN settings to restore it; there is nothing running there to protect.
     */
    private fun guardView(enforcement: Enforcement): Enforcement =
        if (enforcement.filterActive && !repository.state.value.runtime.vpnRunning) enforcement.copy(filterActive = false)
        else enforcement

    override fun onInterrupt() = Unit

    override fun onUnbind(intent: Intent?): Boolean {
        repository.setAccessibilityConnected(false)
        if (repository.enforcement.value.blockingOn) {
            Notifier.showProblem(
                this,
                Notifier.ID_ACCESSIBILITY,
                "App blocking is off",
                "SiteBlock's Accessibility permission was turned off. Open SiteBlock to fix it.",
            )
        }
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        handler.removeCallbacks(trailingGuardCheck)
        scope.cancel()
        super.onDestroy()
    }

    private fun checkActiveWindow() {
        val enforcement = repository.enforcement.value
        if (!enforcement.blockingOn) return
        val pkg = rootInActiveWindow?.packageName?.toString() ?: return
        if (pkg != packageName && pkg in enforcement.appPackages) block(pkg)
    }

    private fun block(pkg: String) {
        val count = repository.recordAttempt(pkg)
        val intent = Intent(this, BlockActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            .putExtra(BlockActivity.EXTRA_LABEL, repository.labelFor(pkg))
            .putExtra(BlockActivity.EXTRA_COUNT, count)
        startActivity(intent)
    }

    private fun bounce() {
        performGlobalAction(GLOBAL_ACTION_HOME)
        val now = SystemClock.uptimeMillis()
        if (now - lastToast > TOAST_GAP_MS) {
            lastToast = now
            Toast.makeText(this, "SiteBlock locks this screen while blocking is on.", Toast.LENGTH_SHORT).show()
        }
    }

    /** Visible text and content descriptions, breadth-first, capped so huge screens stay cheap. */
    private fun collectTexts(root: AccessibilityNodeInfo): List<String> {
        val texts = ArrayList<String>()
        val queue = kotlin.collections.ArrayDeque<AccessibilityNodeInfo>()
        queue.addLast(root)
        var visited = 0
        while (queue.isNotEmpty() && visited < MAX_NODES) {
            val node = queue.removeFirst()
            visited++
            node.text?.toString()?.takeIf { it.isNotBlank() }?.let { texts.add(it) }
            node.contentDescription?.toString()?.takeIf { it.isNotBlank() }?.let { texts.add(it) }
            for (i in 0 until node.childCount) node.getChild(i)?.let { queue.addLast(it) }
        }
        return texts
    }

    companion object {
        private const val GUARD_THROTTLE_MS = 300L
        private const val TOAST_GAP_MS = 3_000L
        private const val MAX_NODES = 400
    }
}
