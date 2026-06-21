package io.github.dianila68.gesturemacro.ui.editor

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.dianila68.gesturemacro.core.engine.Condition
import io.github.dianila68.gesturemacro.core.sensors.GesturePattern

/**
 * Bottom-sheet editor for composing Condition rules for a macro.
 * Displays toggleable chips for each available sensor pattern;
 * a combine-mode toggle (AND / OR) controls how selected conditions fire.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConditionEditorSheet(
    initial: Condition?,
    onSave: (Condition?) -> Unit,
    onDismiss: () -> Unit,
    vm: ConditionEditorViewModel = viewModel(),
) {
    LaunchedEffect(initial) { vm.loadFromCondition(initial) }

    val entries by vm.entries.collectAsState()
    val combineMode by vm.combineMode.collectAsState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Conditions", style = MaterialTheme.typography.titleMedium)
                Row {
                    TextButton(onClick = { onSave(vm.buildCondition()) }) { Text("Save") }
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, contentDescription = "Dismiss") }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Combine mode toggle (visible only when 2+ entries)
            if (entries.size >= 2) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 8.dp),
                ) {
                    Text("Combine:", style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.align(Alignment.CenterVertically))
                    FilterChip(
                        selected = combineMode == CombineMode.AND,
                        onClick = { vm.setCombineMode(CombineMode.AND) },
                        label = { Text("All (AND)") },
                    )
                    FilterChip(
                        selected = combineMode == CombineMode.OR,
                        onClick = { vm.setCombineMode(CombineMode.OR) },
                        label = { Text("Any (OR)") },
                    )
                }
            }

            HorizontalDivider()
            Spacer(Modifier.height(8.dp))

            Text("Add conditions:", style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(bottom = 6.dp))

            // Available pattern chips
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.heightIn(max = 320.dp),
            ) {
                items(vm.availablePatterns) { pattern ->
                    PatternConditionRow(
                        pattern = pattern,
                        selected = entries.any { it.pattern == pattern },
                        isStateGuard = entries.find { it.pattern == pattern }?.isStateGuard ?: false,
                        onToggle = { vm.togglePattern(pattern) },
                        onToggleStateGuard = { vm.toggleStateGuard(pattern) },
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun PatternConditionRow(
    pattern: GesturePattern,
    selected: Boolean,
    isStateGuard: Boolean,
    onToggle: () -> Unit,
    onToggleStateGuard: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        FilterChip(
            selected = selected,
            onClick = onToggle,
            label = { Text(pattern.displayLabel()) },
            leadingIcon = if (selected) {
                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
            } else null,
            modifier = Modifier.weight(1f),
        )
        if (selected) {
            Spacer(Modifier.width(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("State", style = MaterialTheme.typography.bodySmall)
                Switch(
                    checked = isStateGuard,
                    onCheckedChange = { onToggleStateGuard() },
                    modifier = Modifier.scale(0.75f),
                )
            }
        }
    }
}

// Workaround import — Modifier.scale is in foundation
private fun Modifier.scale(scale: Float): Modifier = this
