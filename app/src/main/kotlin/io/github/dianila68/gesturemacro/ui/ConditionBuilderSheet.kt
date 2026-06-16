package io.github.dianila68.gesturemacro.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.dianila68.gesturemacro.core.sensors.GesturePattern

/**
 * ticket-050: Bottom sheet for building a Condition from gesture pattern chips.
 * Shows: combine mode toggle (ALL / ANY), chip row, pattern picker, confirm.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConditionBuilderSheet(
    onConfirm: (io.github.dianila68.gesturemacro.core.engine.Condition?) -> Unit,
    onDismiss: () -> Unit,
    vm: ConditionBuilderViewModel = viewModel(),
) {
    val chips by vm.chips.collectAsState()
    val combineMode by vm.combineMode.collectAsState()
    var showPatternPicker by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Condition", style = MaterialTheme.typography.titleMedium)

            // Combine mode toggle
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = combineMode is ConditionBuilderViewModel.CombineMode.All,
                    onClick = { vm.setCombineMode(ConditionBuilderViewModel.CombineMode.All) },
                    label = { Text("ALL must match") },
                )
                FilterChip(
                    selected = combineMode is ConditionBuilderViewModel.CombineMode.Any,
                    onClick = { vm.setCombineMode(ConditionBuilderViewModel.CombineMode.Any) },
                    label = { Text("ANY must match") },
                )
            }

            // Chip row
            if (chips.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    itemsIndexed(chips) { index, chip ->
                        InputChip(
                            selected = chip.negate,
                            onClick = { vm.toggleNegate(index) },
                            label = {
                                Text(
                                    (if (chip.negate) "NOT " else "") +
                                    chip.pattern.name.lowercase().replace('_', ' ')
                                )
                            },
                            trailingIcon = {
                                IconButton(onClick = { vm.removeChip(index) }, modifier = Modifier.size(18.dp)) {
                                    Icon(Icons.Default.Close, contentDescription = "Remove")
                                }
                            },
                        )
                    }
                }
            }

            // Add pattern button
            if (chips.size < ConditionBuilderViewModel.MAX_CHIPS) {
                OutlinedButton(
                    onClick = { showPatternPicker = true },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Add pattern")
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            ) {
                TextButton(onClick = { vm.clear(); onConfirm(null) }) { Text("Clear") }
                Button(onClick = { onConfirm(vm.buildCondition()) }) { Text("Apply") }
            }
        }
    }

    if (showPatternPicker) {
        PatternPickerDialog(
            onSelect = { pattern ->
                vm.addChip(ConditionBuilderViewModel.ConditionChip(pattern))
                showPatternPicker = false
            },
            onDismiss = { showPatternPicker = false },
        )
    }
}

@Composable
private fun PatternPickerDialog(
    onSelect: (GesturePattern) -> Unit,
    onDismiss: () -> Unit,
) {
    val patterns = GesturePattern.entries
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select pattern") },
        text = {
            Column {
                patterns.forEach { pattern ->
                    TextButton(
                        onClick = { onSelect(pattern) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(pattern.name.lowercase().replace('_', ' '))
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
