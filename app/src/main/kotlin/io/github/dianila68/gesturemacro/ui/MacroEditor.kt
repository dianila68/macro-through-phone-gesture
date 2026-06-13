package io.github.dianila68.gesturemacro.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.github.dianila68.gesturemacro.core.serialization.AccessibilityAction
import io.github.dianila68.gesturemacro.core.serialization.Constraints
import io.github.dianila68.gesturemacro.core.serialization.GestureMacro
import io.github.dianila68.gesturemacro.core.serialization.IntentAction
import io.github.dianila68.gesturemacro.core.serialization.MacroAction
import io.github.dianila68.gesturemacro.core.serialization.MediaControlAction
import io.github.dianila68.gesturemacro.core.serialization.ScreenState
import io.github.dianila68.gesturemacro.core.serialization.SystemToggleAction
import io.github.dianila68.gesturemacro.core.serialization.TimeWindow
import io.github.dianila68.gesturemacro.core.serialization.Trigger
import io.github.dianila68.gesturemacro.core.triggers.TriggerLibrary
import io.github.dianila68.gesturemacro.core.triggers.TriggerSpec
import java.util.UUID

private enum class ActionType(val label: String) {
    SYSTEM_TOGGLE("System toggle"),
    MEDIA_CONTROL("Media control"),
    INTENT("Launch app / intent"),
    ACCESSIBILITY("Accessibility"),
}

/** A single editable action row. [command]/[target] are interpreted per [type]. */
private data class DraftAction(
    val type: ActionType,
    val target: String = "",
    val command: String = "",
    val delayMs: String = "0",
)

/**
 * Full FR-6 editor: trigger + sensitivity + cooldown, screen/time constraints,
 * and a multi-action list builder. Builds a [GestureMacro] through the model's
 * own init invariants so an invalid macro can never be saved; validation errors
 * surface inline. Used for both new macros (initial = null) and edits.
 */
@Composable
fun MacroEditorScreen(initial: GestureMacro?, onSave: (GestureMacro) -> Unit, onCancel: () -> Unit) {
    val triggers = TriggerLibrary.available
    var name by remember { mutableStateOf(initial?.name.orEmpty()) }
    var spec by remember {
        mutableStateOf(
            initial?.trigger?.pattern?.let { TriggerLibrary.forPattern(it) }
                ?.takeIf { it.available }
                ?: triggers.first(),
        )
    }
    var sensitivity by remember { mutableStateOf(initial?.trigger?.sensitivity ?: TriggerSpec.DEFAULT_SENSITIVITY) }
    var cooldownMs by remember { mutableStateOf((initial?.trigger?.cooldownMs ?: spec.defaultCooldownMs).toString()) }
    var screenState by remember { mutableStateOf(initial?.constraints?.screenState ?: ScreenState.ANY) }
    var timeRestricted by remember { mutableStateOf(initial?.constraints?.timeWindow != null) }
    var windowStart by remember { mutableStateOf(initial?.constraints?.timeWindow?.start.orEmpty()) }
    var windowEnd by remember { mutableStateOf(initial?.constraints?.timeWindow?.end.orEmpty()) }
    val actions: SnapshotStateList<DraftAction> = remember {
        initial?.actions?.map { it.toDraft() }.orEmpty().toMutableStateList()
    }
    var error by remember { mutableStateOf<String?>(null) }

    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = if (initial == null) "New macro" else "Edit macro",
            style = MaterialTheme.typography.titleMedium,
        )

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text(text = "Name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        TriggerSection(
            triggers = triggers,
            selected = spec,
            onSelect = { spec = it },
            sensitivity = sensitivity,
            onSensitivity = { sensitivity = it },
            cooldownMs = cooldownMs,
            onCooldown = { cooldownMs = it },
        )

        ConstraintsSection(
            screenState = screenState,
            onScreenState = { screenState = it },
            timeRestricted = timeRestricted,
            onTimeRestricted = { timeRestricted = it },
            windowStart = windowStart,
            onWindowStart = { windowStart = it },
            windowEnd = windowEnd,
            onWindowEnd = { windowEnd = it },
        )

        ActionsSection(actions = actions)

        error?.let {
            Text(text = it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                runCatching {
                    buildMacro(
                        id = initial?.id ?: UUID.randomUUID().toString(),
                        enabled = initial?.enabled ?: true,
                        name = name,
                        spec = spec,
                        sensitivity = sensitivity,
                        cooldownMs = cooldownMs,
                        screenState = screenState,
                        timeWindow = if (timeRestricted) TimeWindow(windowStart.trim(), windowEnd.trim()) else null,
                        drafts = actions,
                    )
                }.onSuccess {
                    error = null
                    onSave(it)
                }.onFailure { error = it.message }
            }) {
                Text(text = "Save")
            }
            TextButton(onClick = onCancel) { Text(text = "Cancel") }
        }
    }
}

@Composable
private fun TriggerSection(
    triggers: List<TriggerSpec>,
    selected: TriggerSpec,
    onSelect: (TriggerSpec) -> Unit,
    sensitivity: Float,
    onSensitivity: (Float) -> Unit,
    cooldownMs: String,
    onCooldown: (String) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = "Trigger", style = MaterialTheme.typography.titleSmall)
            triggers.forEach { t ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = selected.pattern == t.pattern, onClick = { onSelect(t) })
                    Column {
                        Text(text = t.displayName, style = MaterialTheme.typography.bodyMedium)
                        Text(text = t.description, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            Text(
                text = "Sensitivity: ${(sensitivity * 100).toInt()}%",
                style = MaterialTheme.typography.labelMedium,
            )
            Slider(value = sensitivity, onValueChange = onSensitivity, valueRange = 0f..1f)
            Text(text = selected.sensitivityHint, style = MaterialTheme.typography.bodySmall)
            OutlinedTextField(
                value = cooldownMs,
                onValueChange = onCooldown,
                label = { Text(text = "Cooldown (ms)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun ConstraintsSection(
    screenState: ScreenState,
    onScreenState: (ScreenState) -> Unit,
    timeRestricted: Boolean,
    onTimeRestricted: (Boolean) -> Unit,
    windowStart: String,
    onWindowStart: (String) -> Unit,
    windowEnd: String,
    onWindowEnd: (String) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = "Constraints", style = MaterialTheme.typography.titleSmall)
            Text(text = "Screen", style = MaterialTheme.typography.labelMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ScreenState.entries.forEach { s ->
                    FilterChip(
                        selected = screenState == s,
                        onClick = { onScreenState(s) },
                        label = { Text(text = s.name.lowercase()) },
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(checked = timeRestricted, onCheckedChange = onTimeRestricted)
                Text(text = "Only within a time window", style = MaterialTheme.typography.bodyMedium)
            }
            if (timeRestricted) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = windowStart,
                        onValueChange = onWindowStart,
                        label = { Text(text = "Start HH:MM") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = windowEnd,
                        onValueChange = onWindowEnd,
                        label = { Text(text = "End HH:MM") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun ActionsSection(actions: SnapshotStateList<DraftAction>) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = "Actions (run in order)", style = MaterialTheme.typography.titleSmall)
            if (actions.isEmpty()) {
                Text(text = "No actions yet — add at least one.", style = MaterialTheme.typography.bodySmall)
            }
            actions.forEachIndexed { index, draft ->
                ActionEditor(
                    draft = draft,
                    index = index,
                    count = actions.size,
                    onChange = { actions[index] = it },
                    onRemove = { actions.removeAt(index) },
                    onMoveUp = { if (index > 0) actions.add(index - 1, actions.removeAt(index)) },
                    onMoveDown = { if (index < actions.size - 1) actions.add(index + 1, actions.removeAt(index)) },
                )
            }
            if (actions.any { it.type == ActionType.ACCESSIBILITY }) {
                Text(
                    text = "Accessibility actions can drive other apps. Macros you create here are " +
                        "trusted; macros imported from a file stay disabled until you turn them on.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Text(text = "Add action", style = MaterialTheme.typography.labelMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ActionType.entries.forEach { type ->
                    OutlinedButton(onClick = { actions.add(defaultDraft(type)) }) {
                        Text(text = type.label)
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionEditor(
    draft: DraftAction,
    index: Int,
    count: Int,
    onChange: (DraftAction) -> Unit,
    onRemove: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        HorizontalDivider()
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = "${index + 1}. ${draft.type.label}", style = MaterialTheme.typography.bodyMedium)
            Row {
                if (index > 0) TextButton(onClick = onMoveUp) { Text(text = "↑") }
                if (index < count - 1) TextButton(onClick = onMoveDown) { Text(text = "↓") }
                TextButton(onClick = onRemove) { Text(text = "Remove") }
            }
        }
        when (draft.type) {
            ActionType.SYSTEM_TOGGLE -> ActionField(
                label = "Target (e.g. flashlight)",
                value = draft.target,
                onValueChange = { onChange(draft.copy(target = it)) },
            )

            ActionType.MEDIA_CONTROL -> {
                ActionField(
                    label = "Command (play_pause, next, previous)",
                    value = draft.command,
                    onValueChange = { onChange(draft.copy(command = it)) },
                )
                ActionField(
                    label = "Target package (optional)",
                    value = draft.target,
                    onValueChange = { onChange(draft.copy(target = it)) },
                )
            }

            ActionType.INTENT -> {
                ActionField(
                    label = "Package name (e.g. com.spotify.music)",
                    value = draft.target,
                    onValueChange = { onChange(draft.copy(target = it)) },
                )
                ActionField(
                    label = "Command (e.g. launch)",
                    value = draft.command,
                    onValueChange = { onChange(draft.copy(command = it)) },
                )
            }

            ActionType.ACCESSIBILITY -> {
                ActionField(
                    label = "Target package",
                    value = draft.target,
                    onValueChange = { onChange(draft.copy(target = it)) },
                )
                ActionField(
                    label = "Command (e.g. back, notifications)",
                    value = draft.command,
                    onValueChange = { onChange(draft.copy(command = it)) },
                )
            }
        }
        ActionField(
            label = "Delay after (ms)",
            value = draft.delayMs,
            onValueChange = { onChange(draft.copy(delayMs = it)) },
            keyboardType = KeyboardType.Number,
        )
    }
}

@Composable
private fun ActionField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(text = label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier = Modifier.fillMaxWidth(),
    )
}

private fun defaultDraft(type: ActionType): DraftAction = when (type) {
    ActionType.SYSTEM_TOGGLE -> DraftAction(type, target = "flashlight")
    ActionType.MEDIA_CONTROL -> DraftAction(type, command = "play_pause")
    ActionType.INTENT -> DraftAction(type, command = "launch")
    ActionType.ACCESSIBILITY -> DraftAction(type, command = "back")
}

private fun MacroAction.toDraft(): DraftAction = when (this) {
    is SystemToggleAction -> DraftAction(ActionType.SYSTEM_TOGGLE, target = target, delayMs = delayAfterMs.toString())
    is MediaControlAction -> DraftAction(
        ActionType.MEDIA_CONTROL,
        target = target.orEmpty(),
        command = command,
        delayMs = delayAfterMs.toString(),
    )

    is IntentAction -> DraftAction(
        ActionType.INTENT,
        target = target,
        command = command,
        delayMs = delayAfterMs.toString(),
    )

    is AccessibilityAction -> DraftAction(
        ActionType.ACCESSIBILITY,
        target = target,
        command = command,
        delayMs = delayAfterMs.toString(),
    )
}

private fun DraftAction.toAction(): MacroAction {
    val delay = delayMs.trim().ifEmpty { "0" }.toLongOrNull()
        ?: throw IllegalArgumentException("Delay must be a whole number of milliseconds")
    require(delay >= 0) { "Delay must be 0 or greater" }
    return when (type) {
        ActionType.SYSTEM_TOGGLE -> {
            require(target.isNotBlank()) { "System toggle needs a target (e.g. flashlight)" }
            SystemToggleAction(target = target.trim(), delayAfterMs = delay)
        }

        ActionType.MEDIA_CONTROL -> {
            require(command.isNotBlank()) { "Media control needs a command (e.g. play_pause)" }
            MediaControlAction(
                command = command.trim(),
                target = target.trim().ifBlank { null },
                delayAfterMs = delay,
            )
        }

        ActionType.INTENT -> {
            require(target.isNotBlank()) { "Launch/intent needs a package name" }
            require(command.isNotBlank()) { "Launch/intent needs a command (e.g. launch)" }
            IntentAction(target = target.trim(), command = command.trim(), delayAfterMs = delay)
        }

        ActionType.ACCESSIBILITY -> {
            require(target.isNotBlank()) { "Accessibility needs a target package" }
            require(command.isNotBlank()) { "Accessibility needs a command (e.g. back)" }
            AccessibilityAction(target = target.trim(), command = command.trim(), delayAfterMs = delay)
        }
    }
}

@Suppress("LongParameterList")
private fun buildMacro(
    id: String,
    enabled: Boolean,
    name: String,
    spec: TriggerSpec,
    sensitivity: Float,
    cooldownMs: String,
    screenState: ScreenState,
    timeWindow: TimeWindow?,
    drafts: List<DraftAction>,
): GestureMacro {
    require(drafts.isNotEmpty()) { "Add at least one action" }
    val cooldown = cooldownMs.trim().ifEmpty { "0" }.toLongOrNull()
        ?: throw IllegalArgumentException("Cooldown must be a whole number of milliseconds")
    return GestureMacro(
        version = 1,
        id = id,
        name = name.trim(),
        enabled = enabled,
        trigger = Trigger(
            sensor = spec.sensor,
            pattern = spec.pattern,
            sensitivity = sensitivity,
            cooldownMs = cooldown,
        ),
        constraints = Constraints(screenState = screenState, timeWindow = timeWindow),
        actions = drafts.map { it.toAction() },
    )
}
