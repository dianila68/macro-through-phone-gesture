package io.github.dianila68.gesturemacro.android.sensors

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.SharedPreferences
import io.github.dianila68.gesturemacro.core.sensors.ExternalDeviceRegistry
import io.github.dianila68.gesturemacro.core.sensors.parseHandshake
import io.github.dianila68.gesturemacro.core.sensors.toExternalChannels
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/**
 * ticket-057: Discovers and pairs BLE sensor devices implementing gesturemacro/1 protocol.
 *
 * Service UUID: 4e475552-4f4d-4143-524f-000000000001
 * Characteristic 0002: READ + NOTIFY — capability handshake JSON
 * Characteristic 0003: NOTIFY — streaming samples
 * Characteristic 0004: format flag (0x00 = JSON, 0x01 = MessagePack)
 *
 * Full GATT implementation tracked in ticket-057; this class provides the
 * device management surface (scan/connect/disconnect/persist) as a skeleton.
 *
 * Requires BLUETOOTH_SCAN + BLUETOOTH_CONNECT (API 31+) or BLUETOOTH (pre-31).
 */
class DevicePairingService(private val context: Context) {

    @Serializable
    data class BleDeviceInfo(
        val deviceId: String,
        val displayName: String,
        val rssi: Int = 0,
        val connected: Boolean = false,
    )

    private val prefs: SharedPreferences =
        context.getSharedPreferences("ble_paired_devices", Context.MODE_PRIVATE)

    private val _connectedDevices = MutableStateFlow<List<BleDeviceInfo>>(emptyList())
    val connectedDevices: Flow<List<BleDeviceInfo>> = _connectedDevices.asStateFlow()

    private val bluetoothAdapter: BluetoothAdapter? =
        (context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter

    val isBluetoothAvailable: Boolean get() = bluetoothAdapter != null
    val isBluetoothEnabled: Boolean get() = bluetoothAdapter?.isEnabled == true

    /**
     * Starts a BLE scan filtered to the GestureMacro service UUID.
     * Returns a Flow of discovered devices.
     * Full impl: use BluetoothLeScanner + ScanFilter on SERVICE_UUID.
     */
    fun scanForDevices(): Flow<BleDeviceInfo> = kotlinx.coroutines.flow.flow {
        // Skeleton: real impl starts BluetoothLeScanner here
    }

    /**
     * Connects to a device, reads the capability handshake, and registers
     * channels in ExternalDeviceRegistry.
     */
    suspend fun connect(deviceInfo: BleDeviceInfo): Boolean {
        // Skeleton: real impl opens GATT, reads char 0002, parses handshake
        return false
    }

    fun disconnect(deviceId: String) {
        // Skeleton: close GATT connection
        _connectedDevices.value = _connectedDevices.value.filter { it.deviceId != deviceId }
        ExternalDeviceRegistry.removeDevice(deviceId)
    }

    /** Persist a paired device so it auto-reconnects on next service start. */
    fun persistDevice(device: BleDeviceInfo) {
        val current = getPairedDevices().toMutableList()
        current.removeAll { it.deviceId == device.deviceId }
        current.add(device)
        prefs.edit().putString(KEY_DEVICES, Json.encodeToString(
            ListSerializer(BleDeviceInfo.serializer()), current
        )).apply()
    }

    fun forgetDevice(deviceId: String) {
        val current = getPairedDevices().filter { it.deviceId != deviceId }
        prefs.edit().putString(KEY_DEVICES, Json.encodeToString(
            ListSerializer(BleDeviceInfo.serializer()), current
        )).apply()
        disconnect(deviceId)
    }

    fun getPairedDevices(): List<BleDeviceInfo> {
        val raw = prefs.getString(KEY_DEVICES, null) ?: return emptyList()
        return runCatching {
            Json.decodeFromString(ListSerializer(BleDeviceInfo.serializer()), raw)
        }.getOrElse { emptyList() }
    }

    /**
     * Process a received capability handshake JSON (called from GATT callback).
     * Registers channels in ExternalDeviceRegistry.
     */
    fun onHandshakeReceived(deviceId: String, handshakeJson: String) {
        runCatching {
            val handshake = parseHandshake(handshakeJson)
            ExternalDeviceRegistry.registerDevice(deviceId, handshake.toExternalChannels())
        }
    }

    companion object {
        const val SERVICE_UUID = "4e475552-4f4d-4143-524f-000000000001"
        const val CHAR_HANDSHAKE_UUID = "4e475552-4f4d-4143-524f-000000000002"
        const val CHAR_SAMPLES_UUID = "4e475552-4f4d-4143-524f-000000000003"
        const val CHAR_FORMAT_UUID = "4e475552-4f4d-4143-524f-000000000004"
        private const val KEY_DEVICES = "devices_json"
    }
}
