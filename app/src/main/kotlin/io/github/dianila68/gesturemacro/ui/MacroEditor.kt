package io.github.dianila68.gesturemacro.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import io.github.dianila68.gesturemacro.core.actions.ActionAssembly
import io.github.dianila68.gesturemacro.core.actions.ActionCatalog
import io.github.dianila68.gesturemacro.core.actions.ActionCategory
import io.github.dianila68.gesturemacro.core.actions.ActionSpec
import io.github.dianila68.gesturemacro.core.data.InstalledAppRepository
import io.github.dianila68.gesturemacro.core.serialization.AccessibilityAction
import io.github.dianila68.gesturemacro.core.serialization.Constraints
import io.github.dianila68.gesturemacro.core.serialization.GestureMacro
import io.github.dianila68.gesturemacro.core.serialization.IntentAction
import io.github.dianila68.gesturemacro.core.serialization.LocationAlertAction
import io.github.dianila68.gesturemacro.core.serialization.MacroAction
import io.github.dianila68.gesturemacro.core.serialization.MediaControlAction
import io.github.dianila68.gesturemacro.core.serialization.PlaySoundAction
import io.github.dianila68.gesturemacro.core.serialization.ScreenState
import io.github.dianila68.gesturemacro.core.serialization.SoundMode
import io.github.dianila68.gesturemacro.core.serialization.SystemToggleAction
import io.github.dianila68.gesturemacro.core.serialization.TimeWindow
import io.github.dianila68.gesturemacro.core.serialization.Trigger
import io.github.dianila68.gesturemacro.core.triggers.TriggerLibrary
import io.github.dianila68.gesturemacro.core.triggers.TriggerSpec
import java.util.UUID
import kotlinx.coroutines.launch

private enum class ActionType(val label: String) {
    SYSTEM_TOGGLE("System toggle"),
    MEDIA_CONTROL("Media control"),
    INTENT("Launch app / intent"),
    ACCESSIBILITY("Accessibility"),
    PLAY_SOUND("Play sound / TTS"),
    LOCATION_ALERT("Location alert"),
}

/** A single editable action row. Fields are interpreted per [type]; unused fields are ignored. */
private data class DraftAction(
    val type: ActionType,
    val target: String = "",
    val command: String = "",
    val delayMs: String = "0",
    // PLAY_SOUND fields
    val soundMode: SoundMode = SoundMode.BUNDLED,
    val bundledSound: String = "alert",
    val ttsText: String = "",
    val fileUri: String = "",
    // LOCATION_ALERT fields
    val contactName: String = "",
    val contactPhone: String = "",
    val contactMessage: String = "",
    val countdownSec: String = "15",
    /** Stable catalog id if this action was added via the picker; null for manual entries. */
    val catalogId: String? = null,
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

@OptIn(ExperimentalLayoutApi::class)
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ActionsSection(actions: SnapshotStateList<DraftAction>) {
    var showPicker by remember { mutableStateOf(false) }

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
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { showPicker = true }) { Text(text = "Add action") }
            }
        }
    }

    if (showPicker) {
        ActionPickerDialog(
            onPick = { draft ->
                actions.add(draft)
                showPicker = false
            },
            onDismiss = { showPicker = false },
        )
    }
}

/** Catalog-driven action picker grouped by [ActionCategory]. Falls back to manual entry via Advanced. */
@Composable
private fun ActionPickerDialog(
    onPick: (DraftAction) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var searchQuery by remember { mutableStateOf("") }
    var pendingSpec by remember { mutableStateOf<ActionSpec?>(null) }
    var packageInput by remember { mutableStateOf("") }
    var showAdvanced by remember { mutableStateOf(false) }
    var advancedType by remember { mutableStateOf(ActionType.MEDIA_CONTROL) }
    val appRepo = remember { InstalledAppRepository(context) }
    var apps by remember { mutableStateOf(appRepo.apps()) }
    var showAppPicker by remember { mutableStateOf(false) }

    // Load installed apps the first time the picker opens
    LaunchedEffect(Unit) {
        appRepo.refresh()
        apps = appRepo.apps()
    }

    if (showAppPicker && pendingSpec != null) {
        AppPickerDialog(
            apps = apps,
            onPick = { app ->
                val draft = DraftAction(
                    type = ActionType.INTENT,
                    target = app.packageName,
                    command = "launch",
                    catalogId = pendingSpec!!.id,
                )
                onPick(draft)
                showAppPicker = false
                pendingSpec = null
            },
            onManual = {
                // Fall back to manual package entry
                showAppPicker = false
            },
            onDismiss = {
                showAppPicker = false
                pendingSpec = null
            },
        )
    }

    if (!showAppPicker && pendingSpec != null) {
        // Fallback: manual package-name entry (when app list is empty or user chose manual)
        AlertDialog(
            onDismissRequest = { pendingSpec = null },
            title = { Text(text = "App package name") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (apps.isNotEmpty()) {
                        Button(
                            onClick = { showAppPicker = true },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text(text = "Choose from installed apps") }
                        Text(text = "— or type a package name —", style = MaterialTheme.typography.labelSmall)
                    }
                    OutlinedTextField(
                        value = packageInput,
                        onValueChange = { packageInput = it },
                        label = { Text(text = "e.g. com.spotify.music") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val pkg = packageInput.trim()
                        if (pkg.isNotBlank()) {
                            val draft = DraftAction(
                                type = ActionType.INTENT,
                                target = pkg,
                                command = "launch",
                                catalogId = pendingSpec!!.id,
                            )
                            onPick(draft)
                            pendingSpec = null
                            packageInput = ""
                        }
                    },
                ) { Text(text = "Add") }
            },
            dismissButton = { TextButton(onClick = { pendingSpec = null }) { Text(text = "Cancel") } },
        )
        return
    }

    if (showAppPicker) return // AppPickerDialog is showing; don't render the main dialog

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Add action") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text(text = "Search actions") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (showAdvanced) {
                    // Advanced / manual entry — same typed path as before
                    Text(text = "Manual entry", style = MaterialTheme.typography.labelMedium)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        ActionType.entries.forEach { t ->
                            FilterChip(
                                selected = advancedType == t,
                                onClick = { advancedType = t },
                                label = { Text(text = t.label) },
                            )
                        }
                    }
                    Button(
                        onClick = { onPick(defaultDraft(advancedType)) },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(text = "Add ${advancedType.label} (manual)") }
                    HorizontalDivider()
                }
                val grouped = ActionCatalog.byCategory().entries.mapNotNull { (cat, specs) ->
                    val filtered = specs.filter { spec ->
                        searchQuery.isBlank() ||
                            spec.displayName.contains(searchQuery, ignoreCase = true) ||
                            cat.label.contains(searchQuery, ignoreCase = true)
                    }
                    if (filtered.isEmpty()) null else cat to filtered
                }
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    grouped.forEach { (cat, specs) ->
                        item {
                            Text(
                                text = cat.label,
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.padding(top = 8.dp),
                            )
                        }
                        items(specs) { spec ->
                            TextButton(
                                onClick = {
                                    if (spec.requiresPackage) {
                                        pendingSpec = spec
                                        // Show the installed-app list if available, else manual entry
                                        showAppPicker = apps.isNotEmpty()
                                    } else {
                                        onPick(specToDraft(spec))
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Text(text = spec.displayName, style = MaterialTheme.typography.bodyMedium)
                                    Text(text = spec.description, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = { showAdvanced = !showAdvanced }) {
                    Text(text = if (showAdvanced) "Hide advanced" else "Advanced")
                }
                TextButton(onClick = onDismiss) { Text(text = "Cancel") }
            }
        },
    )
}

/** Converts a catalog [ActionSpec] to a [DraftAction] for the editor. */
private fun specToDraft(spec: ActionSpec): DraftAction {
    val action = ActionAssembly.assemble(spec)
    return action.toDraft().copy(catalogId = spec.id)
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

            ActionType.PLAY_SOUND -> {
                Text(text = "Sound mode", style = MaterialTheme.typography.labelMedium)
                @OptIn(ExperimentalLayoutApi::class)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SoundMode.entries.forEach { mode ->
                        FilterChip(
                            selected = draft.soundMode == mode,
                            onClick = { onChange(draft.copy(soundMode = mode)) },
                            label = { Text(text = mode.name.lowercase()) },
                        )
                    }
                }
                when (draft.soundMode) {
                    SoundMode.BUNDLED -> ActionField(
                        label = "Bundled sound id (alert, chime, no)",
                        value = draft.bundledSound,
                        onValueChange = { onChange(draft.copy(bundledSound = it)) },
                    )
                    SoundMode.TTS -> ActionField(
                        label = "Text to speak",
                        value = draft.ttsText,
                        onValueChange = { onChange(draft.copy(ttsText = it)) },
                    )
                    SoundMode.FILE -> ActionField(
                        label = "File URI (content://…)",
                        value = draft.fileUri,
                        onValueChange = { onChange(draft.copy(fileUri = it)) },
                    )
                }
            }

            ActionType.LOCATION_ALERT -> {
                ActionField(
                    label = "Contact name",
                    value = draft.contactName,
                    onValueChange = { onChange(draft.copy(contactName = it)) },
                )
                ActionField(
                    label = "Phone number (e.g. +15555550100)",
                    value = draft.contactPhone,
                    onValueChange = { onChange(draft.copy(contactPhone = it)) },
                )
                ActionField(
                    label = "Extra message (optional)",
                    value = draft.contactMessage,
                    onValueChange = { onChange(draft.copy(contactMessage = it)) },
                )
                ActionField(
                    label = "Countdown (seconds)",
                    value = draft.countdownSec,
                    onValueChange = { onChange(draft.copy(countdownSec = it)) },
                    keyboardType = KeyboardType.Number,
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
    ActionType.PLAY_SOUND -> DraftAction(type, soundMode = SoundMode.BUNDLED, bundledSound = "alert")
    ActionType.LOCATION_ALERT -> DraftAction(type, countdownSec = "15")
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
    is PlaySoundAction -> DraftAction(
        type = ActionType.PLAY_SOUND,
        soundMode = mode,
        bundledSound = bundledSound.orEmpty(),
        ttsText = ttsText.orEmpty(),
        fileUri = fileUri.orEmpty(),
        delayMs = delayAfterMs.toString(),
    )
    is LocationAlertAction -> DraftAction(
        type = ActionType.LOCATION_ALERT,
        contactName = contactName,
        contactPhone = contactPhone,
        contactMessage = message,
        countdownSec = countdownSec.toString(),
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
        ActionType.PLAY_SOUND -> when (soundMode) {
            SoundMode.BUNDLED -> {
                require(bundledSound.isNotBlank()) { "Play sound: bundled sound id required" }
                PlaySoundAction(mode = SoundMode.BUNDLED, bundledSound = bundledSound.trim(), delayAfterMs = delay)
            }
            SoundMode.TTS -> {
                require(ttsText.isNotBlank()) { "Play sound: text to speak required" }
                PlaySoundAction(mode = SoundMode.TTS, ttsText = ttsText.trim(), delayAfterMs = delay)
            }
            SoundMode.FILE -> {
                require(fileUri.isNotBlank()) { "Play sound: file URI required" }
                PlaySoundAction(mode = SoundMode.FILE, fileUri = fileUri.trim(), delayAfterMs = delay)
            }
        }
        ActionType.LOCATION_ALERT -> {
            require(contactName.isNotBlank()) { "Location alert: contact name required" }
            require(contactPhone.isNotBlank()) { "Location alert: phone number required" }
            val countdown = countdownSec.trim().ifEmpty { "15" }.toIntOrNull()
                ?: throw IllegalArgumentException("Countdown must be a whole number of seconds")
            require(countdown >= 0) { "Countdown must be 0 or greater" }
            LocationAlertAction(
                contactName = contactName.trim(),
                contactPhone = contactPhone.trim(),
                message = contactMessage.trim(),
                countdownSec = countdown,
                delayAfterMs = delay,
            )
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

/**
 * ticket-035: Searchable picker listing installed launchable apps by label + icon.
 * [onManual] is called if the user wants to type a package name manually instead.
 */
@Composable
private fun AppPickerDialog(
    apps: List<InstalledAppRepository.AppInfo>,
    onPick: (InstalledAppRepository.AppInfo) -> Unit,
    onManual: () -> Unit,
    onDismiss: () -> Unit,
) {
    var search by remember { mutableStateOf("") }
    val filtered = remember(search, apps) {
        if (search.isBlank()) apps
        else apps.filter { it.label.contains(search, ignoreCase = true) || it.packageName.contains(search, ignoreCase = true) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Choose app") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    label = { Text(text = "Search apps") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (filtered.isEmpty()) {
                    Text(text = "No apps match.", style = MaterialTheme.typography.bodySmall)
                } else {
                    LazyColumn(modifier = Modifier.fillMaxWidth()) {
                        items(filtered) { app ->
                            TextButton(
                                onClick = { onPick(app) },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Image(
                                        painter = rememberDrawablePainter(app.icon),
                                        contentDescription = null,
                                        modifier = Modifier.size(32.dp),
                                    )
                                    Column {
                                        Text(text = app.label, style = MaterialTheme.typography.bodyMedium)
                                        Text(text = app.packageName, style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = onManual) { Text(text = "Type package") }
                TextButton(onClick = onDismiss) { Text(text = "Cancel") }
            }
        },
    )
}
