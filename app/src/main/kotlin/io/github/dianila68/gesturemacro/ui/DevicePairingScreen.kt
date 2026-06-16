package io.github.dianila68.gesturemacro.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * ticket-058: Screen for managing paired BLE sensor devices.
 */
@Composable
fun DevicePairingScreen(vm: DevicePairingViewModel = viewModel()) {
    val paired by vm.pairedDevices.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            "Sensor devices",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(16.dp),
        )

        if (!vm.isBluetoothAvailable) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Bluetooth is not available on this device.")
            }
            return@Column
        }

        if (!vm.isBluetoothEnabled) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Please enable Bluetooth to scan for sensor devices.")
            }
            return@Column
        }

        if (paired.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "No paired devices.\nTap Scan to find nearby GestureMacro sensor devices.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        } else {
            LazyColumn {
                items(paired, key = { it.deviceId }) { device ->
                    PairedDeviceRow(
                        device = device,
                        onForget = { vm.forgetDevice(device.deviceId) },
                    )
                    HorizontalDivider()
                }
            }
        }

        // Scan button (full BLE scan flow requires runtime permissions — wired in ticket-057 impl)
        Spacer(Modifier.weight(1f))
        Button(
            onClick = { /* TODO: launch BLE scan flow — requires permission request first */ },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Text("Scan for devices")
        }
    }
}

@Composable
private fun PairedDeviceRow(
    device: DevicePairingService.BleDeviceInfo,
    onForget: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(device.displayName, style = MaterialTheme.typography.bodyLarge)
            Text(
                device.deviceId,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = onForget) {
            Icon(Icons.Default.Delete, contentDescription = "Forget device")
        }
    }
}
