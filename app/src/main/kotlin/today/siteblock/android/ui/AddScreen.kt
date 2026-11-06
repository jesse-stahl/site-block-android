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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import today.siteblock.android.system.InstalledApp
import today.siteblock.android.system.InstalledApps
import today.siteblock.core.BlockItem
import today.siteblock.core.BlockList
import today.siteblock.core.Catalog
import today.siteblock.core.CatalogEntry
import today.siteblock.core.ItemSource
import today.siteblock.core.PackageId

@Composable
fun AddScreen(
    list: BlockList,
    onAddBuiltin: (CatalogEntry) -> Boolean,
    onAddApp: (String, String, ItemSource) -> Boolean,
    onAddSite: (String) -> Boolean,
    onBack: () -> Unit,
) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("← Back") }
            Text("Add to block list", style = MaterialTheme.typography.titleLarge)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Common", "Installed", "By ID / site").forEachIndexed { index, title ->
                if (index == tab) Button(onClick = { tab = index }) { Text(title) }
                else OutlinedButton(onClick = { tab = index }) { Text(title) }
            }
        }
        when (tab) {
            0 -> CommonTab(list, onAddBuiltin)
            1 -> InstalledTab(list, onAddApp)
            else -> ManualTab(onAddApp, onAddSite)
        }
    }
}

@Composable
private fun CommonTab(list: BlockList, onAddBuiltin: (CatalogEntry) -> Boolean) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(Catalog.entries, key = { it.id }) { entry ->
            val added = list.item(BlockItem.builtinId(entry.id)) != null
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(entry.label, style = MaterialTheme.typography.titleMedium)
                    Text("App + " + entry.domains.joinToString(", "), style = MaterialTheme.typography.bodySmall)
                }
                Button(onClick = { onAddBuiltin(entry) }, enabled = !added) { Text(if (added) "Added" else "Block") }
            }
            HorizontalDivider()
        }
    }
}

@Composable
private fun InstalledTab(list: BlockList, onAddApp: (String, String, ItemSource) -> Boolean) {
    val context = LocalContext.current
    var apps by remember { mutableStateOf<List<InstalledApp>?>(null) }
    var query by rememberSaveable { mutableStateOf("") }
    LaunchedEffect(Unit) { apps = withContext(Dispatchers.IO) { InstalledApps.launchable(context) } }

    OutlinedTextField(
        value = query,
        onValueChange = { query = it },
        label = { Text("Search apps") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
    val all = apps
    if (all == null) {
        Text("Loading apps…")
        return
    }
    val shown = all.filter {
        PackageId.isBlockable(it.packageName) &&
            (query.isBlank() || it.label.contains(query, ignoreCase = true) || it.packageName.contains(query, ignoreCase = true))
    }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(shown, key = { it.packageName }) { app ->
            val added = list.coversPackage(app.packageName)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(app.label, style = MaterialTheme.typography.titleMedium)
                    Text(app.packageName, style = MaterialTheme.typography.bodySmall)
                }
                Button(onClick = { onAddApp(app.packageName, app.label, ItemSource.PICKER) }, enabled = !added) {
                    Text(if (added) "Added" else "Block")
                }
            }
        }
    }
}

@Composable
private fun ManualTab(onAddApp: (String, String, ItemSource) -> Boolean, onAddSite: (String) -> Boolean) {
    val context = LocalContext.current
    var packageId by rememberSaveable { mutableStateOf("") }
    var site by rememberSaveable { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }

    Text(
        "Block any app by its ID, even if it isn't installed. It stays blocked if you install it later. " +
            "The ID is the end of its Play Store link: play.google.com/store/apps/details?id=THIS_PART",
        style = MaterialTheme.typography.bodyMedium,
    )
    OutlinedTextField(
        value = packageId,
        onValueChange = { packageId = it },
        label = { Text("App ID, e.g. com.twitter.android") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
    Button(onClick = {
        val id = packageId.trim()
        val label = InstalledApps.labelFor(context, id) ?: id
        message = if (onAddApp(id, label, ItemSource.MANUAL_APP)) {
            packageId = ""
            "Blocked $label."
        } else {
            "Couldn't add that app. Check the ID. It may already be on your list, or be a system app SiteBlock won't block."
        }
    }) { Text("Block app") }

    HorizontalDivider()

    OutlinedTextField(
        value = site,
        onValueChange = { site = it },
        label = { Text("Website, e.g. x.com") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
    Button(onClick = {
        message = if (onAddSite(site)) {
            site = ""
            "Website blocked, including all its subdomains."
        } else {
            "Couldn't add that website. Check the spelling. It may already be on your list."
        }
    }) { Text("Block website") }

    message?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
}
