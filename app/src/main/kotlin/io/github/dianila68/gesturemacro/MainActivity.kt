package io.github.dianila68.gesturemacro

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import io.github.dianila68.gesturemacro.service.GestureCaptureService
import io.github.dianila68.gesturemacro.service.Heartbeat

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                EngineScreen()
            }
        }
    }
}

@Composable
fun EngineScreen() {
    val context = LocalContext.current
    val running by GestureCaptureService.running.collectAsState()
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) GestureCaptureService.start(context)
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = if (running) "Engine: running" else "Engine: stopped",
                style = MaterialTheme.typography.headlineSmall,
            )
            val lastBeat = Heartbeat(context).lastBeat()
            if (!running && lastBeat > 0L && Heartbeat(context).diedUnexpectedly()) {
                Text(
                    text = "Engine was killed unexpectedly (last heartbeat: $lastBeat)",
                    style = MaterialTheme.typography.bodyMedium,
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
        }
    }
}
