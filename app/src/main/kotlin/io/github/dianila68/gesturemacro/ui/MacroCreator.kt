package io.github.dianila68.gesturemacro.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.dianila68.gesturemacro.core.data.MacroStore
import io.github.dianila68.gesturemacro.core.serialization.GestureMacro
import io.github.dianila68.gesturemacro.core.serialization.IntentAction
import io.github.dianila68.gesturemacro.core.serialization.MacroAction
import io.github.dianila68.gesturemacro.core.serialization.MediaControlAction
import io.github.dianila68.gesturemacro.core.serialization.PatternKind
import io.github.dianila68.gesturemacro.core.serialization.SensorKind
import io.github.dianila68.gesturemacro.core.serialization.SystemToggleAction
import io.github.dianila68.gesturemacro.core.serialization.Trigger
import io.github.dianila68.gesturemacro.core.triggers.TriggerLibrary
import java.util.UUID

private enum class QuickAction(val label: String) {
    FLASHLIGHT("Toggle flashlight"),
    PLAY_PAUSE("Media: play/pause"),
    NEXT_TRACK("Media: next track"),
    LAUNCH_APP("Launch app (package)"),
}

private val quickPatterns = listOf(
    PatternKind.SHAKE,
    PatternKind.FLIP_FACE_DOWN,
    PatternKind.FLIP_FACE_UP,
    PatternKind.PROXIMITY_WAVE,
)

/**
 * Quick-add slice of FR-6: name + trigger pattern + one preset action.
 * The full editor (constraints, action lists, validation UX) is ticket-010.
 */
@Composable
fun MacroCreatorSection() {
    var expanded by remember { mutableStateOf(false) }
    if (!expanded) {
        TextButton(onClick = { expanded = true }) { Text(text = "New macro") }
        return
    }
    var name by remember { mutableStateOf("") }
    var pattern by remember { mutableStateOf(PatternKind.SHAKE) }
    var action by remember { mutableStateOf(QuickAction.FLASHLIGHT) }
    var packageName by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = "New macro", style = MaterialTheme.typography.titleSmall)
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(text = "Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(text = "Trigger", style = MaterialTheme.typography.labelMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                quickPatterns.forEach { p ->
                    FilterChip(
                        selected = pattern == p,
                        onClick = { pattern = p },
                        label = { Text(text = p.name.lowercase().replace('_', ' ')) },
                    )
                }
            }
            Text(text = "Action", style = MaterialTheme.typography.labelMedium)
            QuickAction.entries.forEach { option ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = action == option, onClick = { action = option })
                    Text(text = option.label, style = MaterialTheme.typography.bodyMedium)
                }
            }
            if (action == QuickAction.LAUNCH_APP) {
                OutlinedTextField(
                    value = packageName,
                    onValueChange = { packageName = it },
                    label = { Text(text = "Package name, e.g. com.spotify.music") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            error?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    val result = runCatching { buildMacro(name, pattern, action, packageName) }
                    result
                        .onSuccess {
                            MacroStore.upsert(it)
                            expanded = false
                        }
                        .onFailure { error = it.message }
                }) {
                    Text(text = "Save")
                }
                TextButton(onClick = { expanded = false }) { Text(text = "Cancel") }
            }
        }
    }
}

private fun buildMacro(name: String, pattern: PatternKind, action: QuickAction, packageName: String): GestureMacro {
    val macroAction: MacroAction = when (action) {
        QuickAction.FLASHLIGHT -> SystemToggleAction(target = "flashlight")
        QuickAction.PLAY_PAUSE -> MediaControlAction(command = "play_pause")
        QuickAction.NEXT_TRACK -> MediaControlAction(command = "next")
        QuickAction.LAUNCH_APP -> {
            require(packageName.isNotBlank()) { "Package name is required for app launch" }
            IntentAction(target = packageName.trim(), command = "launch")
        }
    }
    val sensorKind = TriggerLibrary.forPattern(pattern)?.sensor ?: SensorKind.ACCELEROMETER
    return GestureMacro(
        version = 1,
        id = UUID.randomUUID().toString(),
        name = name.trim(),
        enabled = true,
        trigger = Trigger(sensor = sensorKind, pattern = pattern),
        actions = listOf(macroAction),
    )
}
