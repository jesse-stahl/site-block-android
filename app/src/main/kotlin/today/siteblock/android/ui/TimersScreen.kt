package today.siteblock.android.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import today.siteblock.android.data.UiState
import today.siteblock.core.FrictionSettings

@Composable
fun TimersScreen(state: UiState, onWait: (Int) -> Unit, onConfirm: (Int) -> Unit, onBack: () -> Unit) {
    val s = state.list.settings
    val enabled = state.canChangeSettings
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("← Back") }
            Text("Timers", style = MaterialTheme.typography.titleLarge)
        }
        Text("Anything that weakens blocking makes you wait, then confirm within a window, then enter your PIN.")
        if (!enabled) Text("You can change these only while blocking is OFF and nothing is pending.", color = MaterialTheme.colorScheme.error)
        Stepper(
            title = "Wait before you can confirm",
            value = "${s.waitMinutes} min",
            canDecrease = enabled && s.waitMinutes > FrictionSettings.MIN_WAIT,
            canIncrease = enabled && s.waitMinutes < FrictionSettings.MAX_WAIT,
            onDecrease = { onWait(s.waitMinutes - 5) },
            onIncrease = { onWait(s.waitMinutes + 5) },
        )
        Stepper(
            title = "Time to confirm",
            value = "${s.confirmMinutes} min",
            canDecrease = enabled && s.confirmMinutes > FrictionSettings.MIN_CONFIRM,
            canIncrease = enabled && s.confirmMinutes < FrictionSettings.MAX_CONFIRM,
            onDecrease = { onConfirm(s.confirmMinutes - 1) },
            onIncrease = { onConfirm(s.confirmMinutes + 1) },
        )
    }
}

@Composable
private fun Stepper(
    title: String,
    value: String,
    canDecrease: Boolean,
    canIncrease: Boolean,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onDecrease, enabled = canDecrease) { Text("−") }
            Text(value, style = MaterialTheme.typography.titleLarge)
            OutlinedButton(onClick = onIncrease, enabled = canIncrease) { Text("+") }
        }
    }
}
