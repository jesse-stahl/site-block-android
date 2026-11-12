package today.siteblock.android.ui

import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import today.siteblock.android.repository
import today.siteblock.android.system.DeviceCredential
import today.siteblock.android.system.SetupChecks
import today.siteblock.android.system.SetupStatus

private enum class Screen { HOME, ADD, TIMERS, SETUP, WHITELIST }

/** FragmentActivity (not ComponentActivity) because BiometricPrompt needs one. */
class MainActivity : FragmentActivity() {

    private var setup by mutableStateOf<SetupStatus?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(Modifier.fillMaxSize()) {
                    // targetSdk 35+ draws edge-to-edge: keep content clear of the status bar, nav bar and keyboard.
                    Box(Modifier.fillMaxSize().safeDrawingPadding()) { AppRoot() }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        setup = SetupChecks.read(this)
        repository.retryEnforcement() // in the foreground, so starting the VPN service is always allowed
    }

    @Composable
    private fun AppRoot() {
        val state by repository.state.collectAsStateWithLifecycle()
        var screen by rememberSaveable { mutableStateOf(Screen.HOME) }
        BackHandler(enabled = screen != Screen.HOME) { screen = Screen.HOME }
        val back = { screen = Screen.HOME }

        when (screen) {
            Screen.HOME -> HomeScreen(
                state = state,
                setupComplete = setup?.complete == true,
                actions = HomeActions(
                    turnOn = { repository.turnOn() },
                    requestTurnOff = { repository.requestTurnOff() },
                    requestRemove = { repository.requestRemove(it) },
                    pause = { repository.pause(it) },
                    resume = { repository.resume(it) },
                    requestPauseFilter = { repository.requestPauseFilter(it) },
                    resumeFilter = { repository.resumeFilter() },
                    cancelPending = { repository.cancelPending() },
                    confirmPending = { confirmPending() },
                    retry = { repository.retryEnforcement() },
                    openAdd = { screen = Screen.ADD },
                    openTimers = { screen = Screen.TIMERS },
                    openSetup = { screen = Screen.SETUP },
                    openWhitelist = { screen = Screen.WHITELIST },
                ),
            )
            Screen.ADD -> AddScreen(
                list = state.list,
                onAddBuiltin = { repository.addBuiltin(it) },
                onAddApp = { id, label, source -> repository.addApp(id, label, source) },
                onAddSite = { repository.addDomain(it) },
                onBack = back,
            )
            Screen.TIMERS -> TimersScreen(
                state = state,
                onWait = { repository.setWaitMinutes(it) },
                onConfirm = { repository.setConfirmMinutes(it) },
                onBack = back,
            )
            Screen.WHITELIST -> WhitelistScreen(
                state = state,
                // The countdown and PIN confirm live on the home screen's pending card.
                onRequestAdd = { id, label -> repository.requestWhitelist(id, label).also { if (it) back() } },
                onRemove = { repository.removeFromWhitelist(it) },
                onBack = back,
            )
            Screen.SETUP -> SetupScreen(
                status = setup ?: SetupChecks.read(this),
                blockingOn = state.enforcement.blockingOn,
                onRefresh = { setup = SetupChecks.read(this) },
                onBack = back,
            )
        }
    }

    /** Window open -> PIN prompt -> completePending (which re-checks the window after the prompt). */
    private fun confirmPending() {
        val current = repository.state.value
        val expected = current.pending ?: return
        if (!repository.canConfirmNow()) return
        if (!DeviceCredential.isAvailable(this)) {
            toast("Set a screen lock PIN first (Setup, step 1).")
            return
        }
        DeviceCredential.prompt(this, Format.describe(expected, current.list)) { ok ->
            if (ok && !repository.completePending(expected)) toast("Time's up. Start over.")
        }
    }

    private fun toast(text: String) = Toast.makeText(this, text, Toast.LENGTH_LONG).show()
}
