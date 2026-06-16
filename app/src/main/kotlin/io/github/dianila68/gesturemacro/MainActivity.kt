package io.github.dianila68.gesturemacro

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import io.github.dianila68.gesturemacro.service.GestureCaptureService
import io.github.dianila68.gesturemacro.service.Heartbeat
import io.github.dianila68.gesturemacro.service.MacroAccessibilityService
import io.github.dianila68.gesturemacro.ui.GestureRecordingScreen
import io.github.dianila68.gesturemacro.ui.MacroManagerSection
import io.github.dianila68.gesturemacro.ui.RecordedGesturesScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                RootScreen()
            }
        }
    }
}

/** Top-level screens reachable from the home screen. State-based nav (no NavHost dependency). */
private enum class Screen { HOME, RECORDED_GESTURES, RECORDING }

@Composable
private fun RootScreen() {
    var screen by remember { mutableStateOf(Screen.HOME) }
    when (screen) {
        Screen.HOME -> EngineScreen(
            onManageRecordedGestures = { screen = Screen.RECORDED_GESTURES },
        )
        Screen.RECORDED_GESTURES -> RecordedGesturesScreen(
            onRecord = { screen = Screen.RECORDING },
        )
        Screen.RECORDING -> GestureRecordingScreen(
            onSaved = { screen = Screen.RECORDED_GESTURES },
            onCancel = { screen = Screen.RECORDED_GESTURES },
        )
    }
}

@Composable
fun EngineScreen(onManageRecordedGestures: () -> Unit = {}) {
    val context = LocalContext.current
    val running by GestureCaptureService.running.collectAsState()
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) GestureCaptureService.start(context)
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = if (running) "Engine: running" else "Engine: stopped",
                style = MaterialTheme.typography.headlineSmall,
            )
            val lastGesture by GestureCaptureService.lastGesture.collectAsState()
            lastGesture?.let {
                Text(
                    text = "Last gesture: ${it.pattern}",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            val heartbeat = remember { Heartbeat(context) }
            if (!running && heartbeat.diedUnexpectedly()) {
                Text(
                    text = "Engine was killed unexpectedly; it restarts automatically (START_STICKY)",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Button(onClick = {
                if (running) {
                    GestureCaptureService.stop(context)
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    GestureCaptureService.start(context)
                }
            }) {
                Text(text = if (running) "Stop engine" else "Start engine")
            }
            BatteryExemptionCard(context)
            AccessibilityCard(context)
            OutlinedButton(onClick = onManageRecordedGestures, modifier = Modifier.fillMaxWidth()) {
                Text(text = "Recorded gestures")
            }
            MacroManagerSection()
        }
    }
}

@SuppressLint("BatteryLife")
@Composable
private fun BatteryExemptionCard(context: Context) {
    val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    val exempt = remember {
        mutableStateOf(powerManager.isIgnoringBatteryOptimizations(context.packageName))
    }
    if (exempt.value) return
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = "Reliable background capture", style = MaterialTheme.typography.titleSmall)
            Text(
                text = "Without a battery-optimization exemption, Android may pause gesture " +
                    "detection when the screen is off. Nothing else changes: sensor data " +
                    "never leaves the device.",
                style = MaterialTheme.typography.bodySmall,
            )
            OutlinedButton(onClick = {
                context.startActivity(
                    Intent(
                        Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                        Uri.parse("package:${context.packageName}"),
                    ),
                )
                exempt.value = powerManager.isIgnoringBatteryOptimizations(context.packageName)
            }) {
                Text(text = "Allow background operation")
            }
        }
    }
}

@Composable
private fun AccessibilityCard(context: Context) {
    val connected by MacroAccessibilityService.instance.collectAsState()
    if (connected != null) {
        Text(
            text = "Accessibility actions: ready",
            style = MaterialTheme.typography.bodySmall,
        )
        return
    }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = "Control other apps (optional)", style = MaterialTheme.typography.titleSmall)
            Text(
                text = "Macros that press Back, open notifications, or drive other apps need " +
                    "the GestureMacro accessibility service. It reads no screen content and " +
                    "only runs the actions you configure. Imported macros using this stay " +
                    "disabled until you enable them.",
                style = MaterialTheme.typography.bodySmall,
            )
            OutlinedButton(onClick = {
                context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }) {
                Text(text = "Open accessibility settings")
            }
        }
    }
}
