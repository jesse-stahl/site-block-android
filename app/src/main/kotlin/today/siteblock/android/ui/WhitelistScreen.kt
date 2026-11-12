package today.siteblock.android.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import today.siteblock.android.data.UiState
import today.siteblock.android.system.InstalledApp
import today.siteblock.android.system.InstalledApps

/**
 * Apps that skip the web filter, for apps the VPN breaks (e.g. WhatsApp calls). Removing is
 * instant; adding is a weaken action, so it goes through the same wait + PIN as turning blocking off.
 */
@Composable
fun WhitelistScreen(
    state: UiState,
    onRequestAdd: (String, String) -> Boolean,
    onRemove: (String) -> Boolean,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    var apps by remember { mutableStateOf<List<InstalledApp>?>(null) }
    var query by rememberSaveable { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) { apps = withContext(Dispatchers.IO) { InstalledApps.launchable(context) } }
    val busy = state.pending != null

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onBack) { Text("← Back") }
                Text("Whitelist", style = MaterialTheme.typography.titleLarge)
            }
        }
        item {
            Text(
                "Whitelisted apps skip the web filter, so use this for apps it breaks, like calls that " +
                    "never connect. Their own web traffic isn't filtered, but app blocking still applies. " +
                    "Adding an app takes the same wait and PIN as turning blocking off. Removing one is instant.",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        if (busy) {
            item {
                Text(
                    "Another change is waiting on the home screen. Finish or cancel it first.",
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
        message?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }

        item { Text("Whitelisted", style = MaterialTheme.typography.titleMedium) }
        if (state.list.whitelist.isEmpty()) item { Text("No apps. Everything goes through the web filter.") }
        items(state.list.whitelist, key = { "w:" + it.packageId }) { app ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(app.label, style = MaterialTheme.typography.bodyLarge)
                    Text(app.packageId, style = MaterialTheme.typography.bodySmall)
                }
                TextButton(onClick = { onRemove(app.packageId) }) { Text("Remove") }
            }
        }

        item { HorizontalDivider() }
        item { Text("Add an app", style = MaterialTheme.typography.titleMedium) }
        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Search apps") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        val all = apps
        if (all == null) {
            item { Text("Loading apps…") }
        } else {
            val shown = all.filter {
                !state.list.isWhitelisted(it.packageName) &&
                    (query.isBlank() || it.label.contains(query, ignoreCase = true) || it.packageName.contains(query, ignoreCase = true))
            }
            items(shown, key = { "a:" + it.packageName }) { app ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(app.label, style = MaterialTheme.typography.bodyLarge)
                        Text(app.packageName, style = MaterialTheme.typography.bodySmall)
                    }
                    Button(
                        enabled = !busy,
                        onClick = {
                            if (!onRequestAdd(app.packageName, app.label)) message = "Couldn't whitelist ${app.label}."
                        },
                    ) { Text("Whitelist…") }
                }
            }
        }
    }
}
