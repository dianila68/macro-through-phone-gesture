package io.github.dianila68.gesturemacro.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.dianila68.gesturemacro.android.sensors.DevicePairingService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ticket-058: ViewModel for the BLE device pairing screen.
 */
class DevicePairingViewModel(application: Application) : AndroidViewModel(application) {

    private val service = DevicePairingService(application)

    private val _pairedDevices = MutableStateFlow<List<DevicePairingService.BleDeviceInfo>>(emptyList())
    val pairedDevices: StateFlow<List<DevicePairingService.BleDeviceInfo>> = _pairedDevices.asStateFlow()

    val isBluetoothAvailable: Boolean get() = service.isBluetoothAvailable
    val isBluetoothEnabled: Boolean get() = service.isBluetoothEnabled

    init { loadPaired() }

    private fun loadPaired() {
        _pairedDevices.value = service.getPairedDevices()
    }

    fun forgetDevice(deviceId: String) {
        viewModelScope.launch {
            service.forgetDevice(deviceId)
            loadPaired()
        }
    }
}
