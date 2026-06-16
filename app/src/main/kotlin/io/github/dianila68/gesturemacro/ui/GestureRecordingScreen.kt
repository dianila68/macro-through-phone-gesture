package io.github.dianila68.gesturemacro.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.dianila68.gesturemacro.core.recording.RecordingConfig
import io.github.dianila68.gesturemacro.core.recording.RecordingState

/**
 * ticket-050: Multi-step gesture recording wizard.
 * Steps: Start → Countdown → Recording reps → Inter-sample pause → Analysing → Name & Save / Error
 */
@Composable
fun GestureRecordingScreen(
    onSaved: (id: String) -> Unit,
    onCancel: () -> Unit,
    viewModel: GestureRecordingViewModel = viewModel(),
) {
    val state by viewModel.recordingState.collectAsState()
    val savedId by viewModel.savedId.collectAsState()
    val coverage by viewModel.coverageHistory.collectAsState()

    LaunchedEffect(savedId) {
        savedId?.let { onSaved(it) }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Record Gesture") }) },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            AnimatedContent(targetState = state, label = "recording_state") { s ->
                when (s) {
                    is RecordingState.Idle -> IdleStep(
                        onStart = { viewModel.startRecording() },
                        onCancel = onCancel,
                    )
                    is RecordingState.Countdown -> CountdownStep(
                        remainingSec = (s.remainingMs / 1000 + 1).toInt().coerceAtLeast(1),
                        onCancel = { viewModel.cancel(); onCancel() },
                    )
                    is RecordingState.Recording -> RecordingStep(
                        sampleIndex = s.sampleIndex,
                        totalSamples = RecordingConfig().requiredSamples,
                        elapsedMs = s.elapsedMs,
                        maxWindowMs = RecordingConfig().maxWindowMs,
                        coverageScores = coverage.map { it.qualityScore },
                        onCancel = { viewModel.cancel(); onCancel() },
                    )
                    is RecordingState.InterSamplePause -> PauseStep(
                        nextIndex = s.nextSampleIndex,
                        totalSamples = RecordingConfig().requiredSamples,
                        remainingMs = s.remainingMs,
                        onCancel = { viewModel.cancel(); onCancel() },
                    )
                    is RecordingState.Analysing -> AnalysingStep()
                    is RecordingState.Ready -> SaveStep(
                        confidence = s.envelope.confidence,
                        sampleCount = s.envelope.sampleCount,
                        onSave = { name -> viewModel.save(name) },
                        onDiscard = { viewModel.cancel(); onCancel() },
                    )
                    is RecordingState.InsufficientData -> ErrorStep(
                        message = "Not enough usable repetitions. Try performing the gesture more clearly.",
                        onRetry = { viewModel.startRecording() },
                        onCancel = onCancel,
                    )
                    is RecordingState.TimedOut -> ErrorStep(
                        message = "Recording timed out.",
                        onRetry = { viewModel.startRecording() },
                        onCancel = onCancel,
                    )
                    is RecordingState.Cancelled -> {
                        // navigate away
                        LaunchedEffect(Unit) { onCancel() }
                        Box(Modifier.fillMaxSize())
                    }
                }
            }
        }
    }
}

@Composable
private fun IdleStep(onStart: () -> Unit, onCancel: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Record a Custom Gesture", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Perform the same gesture ${RecordingConfig().requiredSamples} times. " +
                "Keep the motion consistent — the app learns the shape of your gesture.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Button(onClick = onStart, modifier = Modifier.fillMaxWidth()) { Text("Start Recording") }
        TextButton(onClick = onCancel) { Text("Cancel") }
    }
}

@Composable
private fun CountdownStep(remainingSec: Int, onCancel: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Get ready…", style = MaterialTheme.typography.headlineSmall)
        Text(
            "$remainingSec",
            style = MaterialTheme.typography.displayLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        TextButton(onClick = onCancel) { Text("Cancel") }
    }
}

@Composable
private fun RecordingStep(
    sampleIndex: Int,
    totalSamples: Int,
    elapsedMs: Long,
    maxWindowMs: Long,
    coverageScores: List<Float>,
    onCancel: () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Perform the gesture now", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Rep ${sampleIndex + 1} of $totalSamples",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        LinearProgressIndicator(
            progress = { (elapsedMs.toFloat() / maxWindowMs).coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth(),
        )
        if (coverageScores.isNotEmpty()) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                coverageScores.forEachIndexed { i, score ->
                    QualityDot(score = score)
                }
            }
        }
        TextButton(onClick = onCancel) { Text("Cancel") }
    }
}

@Composable
private fun QualityDot(score: Float) {
    val color = when {
        score >= 0.7f -> MaterialTheme.colorScheme.primary
        score >= 0.4f -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.error
    }
    Box(
        modifier = Modifier
            .size(12.dp)
            .padding(1.dp),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(
            progress = { score },
            modifier = Modifier.size(12.dp),
            color = color,
            strokeWidth = 3.dp,
        )
    }
}

@Composable
private fun PauseStep(nextIndex: Int, totalSamples: Int, remainingMs: Long, onCancel: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Rest…", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Rep ${nextIndex + 1} of $totalSamples coming up in ${remainingMs / 1000 + 1}s",
            style = MaterialTheme.typography.bodyMedium,
        )
        TextButton(onClick = onCancel) { Text("Cancel") }
    }
}

@Composable
private fun AnalysingStep() {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        CircularProgressIndicator()
        Text("Building gesture model…", style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun SaveStep(
    confidence: Float,
    sampleCount: Int,
    onSave: (name: String) -> Unit,
    onDiscard: () -> Unit,
) {
    var name by remember { mutableStateOf("") }
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp))
        Text("Gesture recorded!", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Confidence: ${"%.0f".format(confidence * 100)}% from $sampleCount samples",
            style = MaterialTheme.typography.bodyMedium,
        )
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Gesture name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Button(
            onClick = { onSave(name.ifBlank { "My gesture" }) },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Save") }
        TextButton(onClick = onDiscard) { Text("Discard") }
    }
}

@Composable
private fun ErrorStep(message: String, onRetry: () -> Unit, onCancel: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Recording failed", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.error)
        Text(message, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
        Button(onClick = onRetry, modifier = Modifier.fillMaxWidth()) { Text("Try again") }
        TextButton(onClick = onCancel) { Text("Cancel") }
    }
}
