package today.siteblock.android.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import today.siteblock.android.data.UiState
import today.siteblock.core.BlockItem
import today.siteblock.core.PauseDuration
import today.siteblock.core.PendingWeaken

class HomeActions(
    val turnOn: () -> Unit,
    val requestTurnOff: () -> Unit,
    val requestRemove: (String) -> Unit,
    val pause: (String) -> Unit,
    val resume: (String) -> Unit,
    val requestPauseFilter: (PauseDuration) -> Unit,
    val resumeFilter: () -> Unit,
    val cancelPending: () -> Unit,
    val confirmPending: () -> Unit,
    val retry: () -> Unit,
    val openAdd: () -> Unit,
    val openTimers: () -> Unit,
    val openSetup: () -> Unit,
)

@Composable
fun HomeScreen(state: UiState, setupComplete: Boolean, actions: HomeActions) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { Text("SiteBlock", style = MaterialTheme.typography.headlineMedium) }
        if (!setupComplete) {
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Setup isn't finished, so blocking may not work yet.")
                        Button(onClick = actions.openSetup) { Text("Finish setup") }
                    }
                }
            }
        }
        state.problem?.let { problem ->
            item { Text(problem, color = MaterialTheme.colorScheme.error) }
        }
        item { StatusCard(state, actions) }
        state.pending?.let { pending -> item { PendingCard(state, pending, actions) } }
        if (state.enforcement.filterActive || state.enforcement.filterPaused) {
            item { FilterCard(state, actions) }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = actions.openAdd) { Text("Add") }
                OutlinedButton(onClick = actions.openTimers) { Text("Timers") }
                OutlinedButton(onClick = actions.openSetup) { Text("Setup") }
            }
        }
        if (state.list.items.isEmpty()) {
            item { Text("Nothing on your block list yet. Tap Add.") }
        }
        items(state.list.items, key = { it.id }) { item -> ItemRow(item, state, actions) }
    }
}

@Composable
private fun StatusCard(state: UiState, actions: HomeActions) {
    val on = state.enforcement.blockingOn
    val gaps = state.enforcementGaps
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                when {
                    !on -> "Blocking is OFF"
                    gaps.isEmpty() -> "Blocking is ON"
                    else -> "Blocking is ON, but not fully working"
                },
                style = MaterialTheme.typography.titleLarge,
            )
            gaps.forEach { Text("• $it", color = MaterialTheme.colorScheme.error) }
            if (gaps.isNotEmpty()) OutlinedButton(onClick = actions.retry) { Text("Retry") }
            if (!on) {
                Button(onClick = actions.turnOn) { Text("Turn blocking ON") }
            } else {
                OutlinedButton(onClick = actions.requestTurnOff, enabled = state.pending == null) { Text("Turn blocking OFF…") }
            }
        }
    }
}

@Composable
private fun PendingCard(state: UiState, pending: PendingWeaken, actions: HomeActions) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(Format.describe(pending, state.list), style = MaterialTheme.typography.titleMedium)
            val wait = state.remainingWaitMs
            val confirm = state.remainingConfirmMs
            when {
                wait != null -> Text("Wait ${Format.mmss(wait)} before you can confirm.")
                confirm != null -> Text("Confirm within ${Format.mmss(confirm)}, or you start over.")
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = actions.confirmPending, enabled = state.canConfirm) { Text("Confirm with PIN") }
                TextButton(onClick = actions.cancelPending) { Text("Cancel") }
            }
        }
    }
}

@Composable
private fun FilterCard(state: UiState, actions: HomeActions) {
    var menuOpen by remember { mutableStateOf(false) }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (state.enforcement.filterPaused) {
                val left = state.filterPauseRemainingMs
                Text(if (left == null) "Web filter paused until you turn it back on." else "Web filter paused. Back on in ${Format.duration(left)}.")
                Button(onClick = actions.resumeFilter) { Text("Resume web filter now") }
            } else {
                // Only claim it works when it really runs; the status card above lists why it doesn't.
                Text(
                    if (state.runtime.vpnRunning && !state.privateDnsStrict) "Web filter is on. Blocked sites won't load in any app."
                    else "Web filter should be on but isn't working yet. See the status above.",
                )
                Box {
                    OutlinedButton(onClick = { menuOpen = true }, enabled = state.pending == null) { Text("Pause web filter…") }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        PauseDuration.entries.forEach { duration ->
                            DropdownMenuItem(
                                text = { Text(duration.label) },
                                onClick = {
                                    menuOpen = false
                                    actions.requestPauseFilter(duration)
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ItemRow(item: BlockItem, state: UiState, actions: HomeActions) {
    val paused = state.list.isPaused(item.id)
    val attempts = item.packages.sumOf { state.attemptsToday[it] ?: 0 }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(item.label + if (paused) " (paused)" else "", style = MaterialTheme.typography.titleMedium)
            Text(Format.summary(item), style = MaterialTheme.typography.bodySmall)
            if (attempts > 0) Text("Blocked $attempts× today", style = MaterialTheme.typography.bodySmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (paused) {
                    TextButton(onClick = { actions.resume(item.id) }) { Text("Resume") }
                } else if (!state.enforcement.blockingOn) {
                    TextButton(onClick = { actions.pause(item.id) }) { Text("Pause") }
                }
                TextButton(onClick = { actions.requestRemove(item.id) }, enabled = state.pending == null) { Text("Remove…") }
            }
        }
    }
}
