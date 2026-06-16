package io.github.dianila68.gesturemacro.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.dianila68.gesturemacro.android.data.RecordedGestureEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * ticket-053: Displays saved recorded gestures with enable/disable and delete controls.
 */
@Composable
fun RecordedGesturesScreen(
    onRecord: () -> Unit,
    viewModel: RecordedGesturesViewModel = viewModel(),
) {
    val gestures by viewModel.gestures.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Recorded Gestures") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = onRecord) {
                Icon(Icons.Default.Add, contentDescription = "Record new gesture")
            }
        },
    ) { padding ->
        if (gestures.isEmpty()) {
            Column(
                modifier = Modifier
                    .padding(padding)
                    .padding(32.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("No recorded gestures yet.", style = MaterialTheme.typography.bodyMedium)
                Text("Tap + to record a new gesture.", style = MaterialTheme.typography.bodySmall)
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(gestures, key = { it.id }) { entity ->
                    GestureCard(
                        entity = entity,
                        onDelete = { viewModel.delete(entity.id) },
                        onToggle = { viewModel.toggleEnabled(entity) },
                    )
                }
            }
        }
    }
}

@Composable
private fun GestureCard(
    entity: RecordedGestureEntity,
    onDelete: () -> Unit,
    onToggle: () -> Unit,
) {
    val date = SimpleDateFormat("MMM d, yyyy HH:mm", Locale.getDefault())
        .format(Date(entity.createdAt))

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(entity.name, style = MaterialTheme.typography.bodyLarge)
                Text(
                    "Confidence: ${"%.0f".format(entity.confidence * 100)}% · ${entity.sampleCount} samples · $date",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Switch(checked = entity.enabled, onCheckedChange = { onToggle() })
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete")
            }
        }
    }
}
