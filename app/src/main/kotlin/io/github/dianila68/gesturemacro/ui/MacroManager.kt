package io.github.dianila68.gesturemacro.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import io.github.dianila68.gesturemacro.core.data.MacroStore
import io.github.dianila68.gesturemacro.core.serialization.GestureMacro
import io.github.dianila68.gesturemacro.core.serialization.MacroCodec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * FR-6 (partial: list/toggle/delete; full editor is ticket-010) and FR-7
 * (import/export through SAF — the app needs no storage permission).
 */
@Composable
fun MacroManagerSection() {
    val context = LocalContext.current
    val macros by MacroStore.macros.collectAsState()
    val io = rememberCoroutineScope()
    var importError by remember { mutableStateOf<String?>(null) }
    var exportCandidate by remember { mutableStateOf<GestureMacro?>(null) }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        io.launch(Dispatchers.IO) {
            val text = runCatching {
                context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            }.getOrNull()
            if (text == null) {
                importError = "Could not read the selected file"
                return@launch
            }
            MacroCodec.decodeAuto(text)
                .onSuccess {
                    MacroStore.upsert(it)
                    importError = null
                }
                .onFailure { importError = it.message }
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        val macro = exportCandidate
        exportCandidate = null
        if (uri == null || macro == null) return@rememberLauncherForActivityResult
        io.launch(Dispatchers.IO) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use {
                    it.write(MacroCodec.encode(macro))
                }
            }
        }
    }

    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = "Macros", style = MaterialTheme.typography.titleMedium)
        macros.forEach { macro ->
            MacroRow(
                macro = macro,
                onToggle = { MacroStore.setEnabled(macro.id, it) },
                onDelete = { MacroStore.remove(macro.id) },
                onExport = {
                    exportCandidate = macro
                    exportLauncher.launch("${macro.name.replace(' ', '_')}.json")
                },
            )
        }
        importError?.let {
            Text(
                text = "Import failed: $it",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
        OutlinedButton(onClick = {
            importLauncher.launch(
                arrayOf("application/json", "application/x-yaml", "text/*", "application/octet-stream"),
            )
        }) {
            Text(text = "Import macro (JSON/YAML)")
        }
        MacroCreatorSection()
    }
}

@Composable
private fun MacroRow(macro: GestureMacro, onToggle: (Boolean) -> Unit, onDelete: () -> Unit, onExport: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = macro.name, style = MaterialTheme.typography.titleSmall)
                Switch(checked = macro.enabled, onCheckedChange = onToggle)
            }
            Text(
                text = "${macro.trigger.pattern} → ${macro.actions.size} action(s)",
                style = MaterialTheme.typography.bodySmall,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onExport) { Text(text = "Export") }
                TextButton(onClick = onDelete) { Text(text = "Delete") }
            }
        }
    }
}
