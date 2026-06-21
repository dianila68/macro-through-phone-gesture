package io.github.dianila68.gesturemacro.ui.recording

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.dianila68.gesturemacro.core.recording.GestureEnvelope
import io.github.dianila68.gesturemacro.core.recording.RecordingState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val COUNTDOWN_FONT_SP = 96
private const val WAVEFORM_HEIGHT_DP = 140
private const val WINDOW_DURATION_DISPLAY_MS = 3000f
private const val WAVEFORM_TOP_MARGIN = 0.9f
private const val BAND_HEIGHT_RATIO = 0.85f
private const val BAND_STD_MULTIPLIER = 1.5f
private const val BAND_COERCE_MIN = 0.01f
private const val COLOR_MATCH_GREEN = 0xFF1B5E20L
private const val COLOR_MATCH_AMBER = 0xFFF57F17L
private const val COLOR_FEEDBACK_ALPHA = 0.15f
private const val TEST_DURATION_MS = 10_000L
private const val CONFIDENCE_HIGH = 0.75f
private const val CONFIDENCE_MEDIUM = 0.5f

/**
 * Entry point for the gesture recording sub-editor.
 * Calls [onSave] with the resulting [GestureEnvelope] on success, or [onCancel] if dismissed.
 *
 * Manual test: tap "Record a gesture" from the trigger picker → confirm 6 screens appear
 * in sequence → perform gesture 5 times → review screen shows shaded band chart.
 */
@Composable
fun RecordingSubEditor(
    onSave: (GestureEnvelope) -> Unit,
    onCancel: () -> Unit,
    vm: RecordingViewModel = viewModel(),
) {
    val state by vm.recordingState.collectAsState()

    when (val s = state) {
        is RecordingState.Idle -> RecordingBriefingScreen(vm, onCancel)
        is RecordingState.Countdown -> RecordingCountdownScreen(s.remainingMs)
        is RecordingState.Recording -> ActiveRecordingScreen(vm, s)
        is RecordingState.InterSamplePause -> InterSamplePauseScreen(s)
        is RecordingState.Analysing -> AnalysingScreen()
        is RecordingState.Ready -> RecordingReviewScreen(
            vm = vm,
            envelope = s.envelope,
            onSave = { onSave(s.envelope) },
            onRecordAgain = { vm.reset() },
        )
        is RecordingState.InsufficientData -> InsufficientDataScreen(
            onTryAgain = { vm.reset() },
            onCancel = onCancel,
        )
        is RecordingState.Cancelled -> {
            LaunchedEffect(Unit) { onCancel() }
        }
        is RecordingState.TimedOut -> InsufficientDataScreen(
            onTryAgain = { vm.reset() },
            onCancel = onCancel,
        )
    }
}

@Composable
private fun RecordingBriefingScreen(vm: RecordingViewModel, onCancel: () -> Unit) {
    val samples by vm.requiredSamples.collectAsState()
    val useGyro by vm.useGyro.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Text("Record a gesture", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(
            "You'll perform the same movement $samples times. " +
                "Hold the phone however you plan to hold it when using this gesture.",
            style = MaterialTheme.typography.bodyMedium,
        )
        Text("Repetitions", style = MaterialTheme.typography.labelLarge)
        Slider(
            value = samples.toFloat(),
            onValueChange = { vm.setRequiredSamples(it.toInt()) },
            valueRange = 3f..8f,
            steps = 4,
        )
        Text("$samples repetitions", style = MaterialTheme.typography.bodySmall)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Include gyroscope", modifier = Modifier.weight(1f))
            Switch(checked = useGyro, onCheckedChange = vm::setUseGyro)
        }
        Spacer(Modifier.weight(1f))
        Button(onClick = vm::startRecording, modifier = Modifier.fillMaxWidth()) { Text("Start recording") }
        OutlinedButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) { Text("Cancel") }
    }
}

@Composable
private fun RecordingCountdownScreen(remainingMs: Long) {
    val seconds = (remainingMs / 1000) + 1
    Box(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = if (seconds > 0) "$seconds" else "Go!",
                fontSize = COUNTDOWN_FONT_SP.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Text(
                "Get ready…",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}

@Composable
private fun ActiveRecordingScreen(vm: RecordingViewModel, state: RecordingState.Recording) {
    val waveform by vm.waveformPoints.collectAsState()
    val accentColor = MaterialTheme.colorScheme.primary

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            "Repetition ${state.sampleIndex + 1}",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(WAVEFORM_HEIGHT_DP.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) { drawWaveform(waveform, accentColor) }
        LinearProgressIndicator(
            progress = { (state.elapsedMs / WINDOW_DURATION_DISPLAY_MS).coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            "${state.elapsedMs / 1000}s / 3s",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.align(Alignment.End),
        )
        Text(
            "Perform your gesture now. Keep moving until the progress bar fills.",
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

private fun DrawScope.drawWaveform(points: List<Float>, color: Color) {
    if (points.size < 2) return
    val max = points.maxOrNull()?.coerceAtLeast(1f) ?: 1f
    val step = size.width / (points.size - 1)
    val path = Path()
    points.forEachIndexed { i, v ->
        val x = i * step
        val y = size.height - (v / max) * size.height * WAVEFORM_TOP_MARGIN
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    drawPath(path, color, style = Stroke(width = 3f))
}

@Composable
private fun InterSamplePauseScreen(state: RecordingState.InterSamplePause) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CircularProgressIndicator()
            Text("Rest…", style = MaterialTheme.typography.headlineSmall)
            Text(
                "Next in ${(state.remainingMs / 1000) + 1}s (repetition ${state.nextSampleIndex + 1})",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun AnalysingScreen() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(modifier = Modifier.size(56.dp))
            Spacer(Modifier.height(16.dp))
            Text("Building gesture profile…", style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
fun RecordingReviewScreen(
    vm: RecordingViewModel,
    envelope: GestureEnvelope,
    onSave: () -> Unit,
    onRecordAgain: () -> Unit,
) {
    val sensitivity by vm.sensitivity.collectAsState()
    val validationResult by vm.validationResult.collectAsState()
    var isValidating by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = primaryColor.copy(alpha = 0.3f)
    val confidenceLabel = when {
        envelope.confidence >= CONFIDENCE_HIGH -> "High"
        envelope.confidence >= CONFIDENCE_MEDIUM -> "Medium"
        else -> "Low"
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Gesture recorded", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("${envelope.sampleCount} repetitions · Confidence: $confidenceLabel", style = MaterialTheme.typography.bodyMedium)
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(WAVEFORM_HEIGHT_DP.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) { drawEnvelopeBand(envelope, primaryColor, secondaryColor) }
        Text("Sensitivity", style = MaterialTheme.typography.labelLarge)
        Slider(value = sensitivity, onValueChange = vm::setSensitivity, valueRange = 0f..1f)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Strict", style = MaterialTheme.typography.labelSmall)
            Text("Loose", style = MaterialTheme.typography.labelSmall)
        }
        ValidationFeedback(isValidating = isValidating, result = validationResult)
        Spacer(Modifier.weight(1f))
        if (!isValidating) {
            OutlinedButton(
                onClick = {
                    isValidating = true
                    scope.launch {
                        delay(TEST_DURATION_MS)
                        isValidating = false
                        if (vm.validationResult.value == null) {
                            vm.onValidationComplete(ValidationOutcome.TIMED_OUT)
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Test it (10s)") }
        }
        Button(onClick = onSave, modifier = Modifier.fillMaxWidth()) { Text("Save gesture") }
        OutlinedButton(onClick = onRecordAgain, modifier = Modifier.fillMaxWidth()) { Text("Record again") }
    }
}

@Composable
private fun ValidationFeedback(isValidating: Boolean, result: ValidationOutcome?) {
    val greenColor = Color(COLOR_MATCH_GREEN)
    val amberColor = Color(COLOR_MATCH_AMBER)
    when {
        isValidating -> Text(
            "Perform the gesture now… (10s)",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.secondary,
        )
        result == ValidationOutcome.MATCHED -> Surface(
            color = greenColor.copy(alpha = COLOR_FEEDBACK_ALPHA),
            shape = MaterialTheme.shapes.small,
        ) {
            Text(
                "Match! Gesture recognised.",
                modifier = Modifier.padding(12.dp),
                color = greenColor,
                fontWeight = FontWeight.SemiBold,
            )
        }
        result == ValidationOutcome.PARTIAL -> Surface(
            color = amberColor.copy(alpha = COLOR_FEEDBACK_ALPHA),
            shape = MaterialTheme.shapes.small,
        ) {
            Text(
                "Partial match — try moving more like you did during recording.",
                modifier = Modifier.padding(12.dp),
                color = amberColor,
            )
        }
        result == ValidationOutcome.TIMED_OUT -> Surface(
            color = MaterialTheme.colorScheme.errorContainer,
            shape = MaterialTheme.shapes.small,
        ) {
            Text(
                "No match — try again or adjust sensitivity.",
                modifier = Modifier.padding(12.dp),
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
        }
    }
}

private fun DrawScope.drawEnvelopeBand(envelope: GestureEnvelope, lineColor: Color, fillColor: Color) {
    val n = envelope.sliceCount
    if (n < 2) return
    val max = (envelope.magnitudeMean.maxOrNull() ?: 1f) +
        (envelope.magnitudeStd.maxOrNull() ?: 0f) * BAND_STD_MULTIPLIER

    fun yOf(v: Float) = size.height - (v / max.coerceAtLeast(BAND_COERCE_MIN)) * size.height * BAND_HEIGHT_RATIO
    fun xOf(i: Int) = i.toFloat() / (n - 1) * size.width

    val bandPath = Path()
    for (i in 0 until n) {
        val x = xOf(i)
        val y = yOf(envelope.magnitudeMean[i] + envelope.magnitudeStd[i])
        if (i == 0) bandPath.moveTo(x, y) else bandPath.lineTo(x, y)
    }
    for (i in n - 1 downTo 0) {
        bandPath.lineTo(xOf(i), yOf(envelope.magnitudeMean[i] - envelope.magnitudeStd[i]))
    }
    bandPath.close()
    drawPath(bandPath, fillColor)

    val meanPath = Path()
    for (i in 0 until n) {
        val x = xOf(i)
        val y = yOf(envelope.magnitudeMean[i])
        if (i == 0) meanPath.moveTo(x, y) else meanPath.lineTo(x, y)
    }
    drawPath(meanPath, lineColor, style = Stroke(width = 3f))
}

@Composable
private fun InsufficientDataScreen(onTryAgain: () -> Unit, onCancel: () -> Unit) {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("Not enough data", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(
                "Too few clear repetitions were captured. " +
                    "Try moving more decisively — a clear, deliberate gesture works best.",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium,
            )
            Button(onClick = onTryAgain, modifier = Modifier.fillMaxWidth()) { Text("Try again") }
            OutlinedButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) { Text("Cancel") }
        }
    }
}
