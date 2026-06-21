package io.github.dianila68.gesturemacro.ui.recording

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.dianila68.gesturemacro.android.data.RecordedGestureStore
import io.github.dianila68.gesturemacro.android.data.StoredGesture
import kotlinx.coroutines.launch

private const val CONFIDENCE_HIGH = 0.75f
private const val CONFIDENCE_MEDIUM = 0.5f
private const val MAX_NAME_LENGTH = 40

/**
 * Section shown in MacroManager that lists saved recorded gestures with
 * rename, re-record, and delete (with cascade-disable warning) actions.
 */
@Composable
fun GestureLibrarySection(
    store: RecordedGestureStore,
    onReRecord: (StoredGesture) -> Unit,
) {
    val gestures by store.observeAll().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Recorded Gestures", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

        if (gestures.isEmpty()) {
            Text(
                "No recorded gestures yet. Record one from the trigger picker.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp),
            )
        } else {
            gestures.forEach { gesture ->
                GestureRow(
                    gesture = gesture,
                    onRename = { newName -> scope.launch { store.rename(gesture.id, newName) } },
                    onDelete = { scope.launch { store.delete(gesture.id) } },
                    onReRecord = { onReRecord(gesture) },
                    store = store,
                )
            }
        }
    }
}

@Composable
private fun GestureRow(
    gesture: StoredGesture,
    onRename: (String) -> Unit,
    onDelete: () -> Unit,
    onReRecord: () -> Unit,
    store: RecordedGestureStore,
) {
    val scope = rememberCoroutineScope()
    var showDeleteDialog by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf(false) }
    var nameField by remember(gesture.id) { mutableStateOf(gesture.name) }
    var usedByCount by remember { mutableStateOf(0) }

    val confidenceLabel = when {
        gesture.envelope.confidence >= CONFIDENCE_HIGH -> "High"
        gesture.envelope.confidence >= CONFIDENCE_MEDIUM -> "Medium"
        else -> "Low"
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (renaming) {
                    OutlinedTextField(
                        value = nameField,
                        onValueChange = { if (it.length <= MAX_NAME_LENGTH) nameField = it },
                        label = { Text("Name") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        isError = nameField.isBlank(),
                        supportingText = if (nameField.isBlank()) {
                            { Text("Name cannot be empty") }
                        } else {
                            null
                        },
                    )
                } else {
                    Text(
                        gesture.name,
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            Text(
                "Confidence: $confidenceLabel · ${gesture.envelope.sampleCount} samples",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (!gesture.sealValid) {
                Text(
                    "⚠ Integrity check failed — gesture disabled",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            GestureRowActions(
                renaming = renaming,
                nameField = nameField,
                onSaveRename = {
                    if (nameField.isNotBlank()) {
                        onRename(nameField)
                        renaming = false
                    }
                },
                onCancelRename = { renaming = false; nameField = gesture.name },
                onStartRename = { renaming = true },
                onReRecord = onReRecord,
                onDeleteRequest = {
                    scope.launch { usedByCount = store.macrosUsing(gesture.id) }
                    showDeleteDialog = true
                },
            )
        }
    }

    if (showDeleteDialog) {
        GestureDeleteDialog(
            gestureName = gesture.name,
            usedByCount = usedByCount,
            onConfirm = { onDelete(); showDeleteDialog = false },
            onDismiss = { showDeleteDialog = false },
        )
    }
}

@Composable
private fun GestureRowActions(
    renaming: Boolean,
    nameField: String,
    onSaveRename: () -> Unit,
    onCancelRename: () -> Unit,
    onStartRename: () -> Unit,
    onReRecord: () -> Unit,
    onDeleteRequest: () -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (renaming) {
            TextButton(onClick = onSaveRename, enabled = nameField.isNotBlank()) { Text("Save") }
            TextButton(onClick = onCancelRename) { Text("Cancel") }
        } else {
            TextButton(onClick = onStartRename) { Text("Rename") }
            TextButton(onClick = onReRecord) { Text("Re-record") }
            TextButton(onClick = onDeleteRequest) { Text("Delete") }
        }
    }
}

@Composable
private fun GestureDeleteDialog(
    gestureName: String,
    usedByCount: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val warning = if (usedByCount > 0) " $usedByCount macro(s) using this gesture will be disabled." else ""
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete gesture?") },
        text = { Text("\"$gestureName\" will be permanently deleted.$warning") },
        confirmButton = { TextButton(onClick = onConfirm) { Text("Delete") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
