package com.niresh23.fanlightcontroller.ui.connection

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.ParcelUuid
import androidx.core.app.ActivityCompat
import com.niresh23.fanlightcontroller.ble.BleDeviceData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.lang.Exception
import java.util.UUID

class BleScannerAdapter(
    private val context: Context,
    private val coroutineScope: CoroutineScope
) : IBleScanner {

    companion object {
        private const val SCAN_PERIOD = 20000L
    }

    private val scanResults = mutableMapOf<String, BluetoothDevice>()

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager

    private val adapter: BluetoothAdapter? = bluetoothManager.adapter
    private val _scanEventFlow = MutableSharedFlow<ScanEvent>()
    override val scanEventFlow = _scanEventFlow.asSharedFlow()

    private var scanner: BluetoothLeScanner? = null
    private var scanCallback: DeviceScanCallback? = null
    private lateinit var scanFilters: List<ScanFilter>
    private lateinit var scanSettings: ScanSettings

    override fun stopScan() {
        if(ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
            try {
                scanner?.stopScan(scanCallback)
            } catch (exception: Exception) {
                coroutineScope.launch {
                    exception.message?.let {
                        _scanEventFlow.emit(ScanEvent.Error(it))
                    }
                }
            }
        }

        scanCallback = null
        coroutineScope.launch {
            _scanEventFlow.emit(ScanEvent.StopScanning)
        }
    }

    override fun release() {
        coroutineScope.cancel()
    }

    override fun startScan(vararg uuids: String) {
        scanFilters = buildScanFilters(*uuids)
        scanSettings = buildScanSettings()
        if (scanCallback == null) {
            scanner = adapter?.bluetoothLeScanner
            scanCallback = DeviceScanCallback(_scanEventFlow, coroutineScope)
            if (scanner == null) {
                coroutineScope.launch {
                    _scanEventFlow.emit(ScanEvent.Error("Turn on Bluetooth connection"))
                }
            } else {
                coroutineScope.launch {
                    _scanEventFlow.emit(ScanEvent.Scanning)
                    delay(SCAN_PERIOD)
                    stopScan()
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (ActivityCompat.checkSelfPermission(
                            context,
                            Manifest.permission.BLUETOOTH_SCAN
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        scanner?.startScan(null, scanSettings, scanCallback)
                    }
                } else {
                    scanner?.startScan(null, scanSettings, scanCallback)
                }
            }
        }
    }

    private fun buildScanFilters(vararg uuids: String): List<ScanFilter> {
        return uuids.map {
            val builder = ScanFilter.Builder()
            builder.setServiceUuid(ParcelUuid(UUID.fromString(it)))

            builder.build()
        }
    }


    private fun buildScanSettings(): ScanSettings {
        return ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
            .build()
    }

    private inner class DeviceScanCallback(
        private val flow: FlowCollector<ScanEvent>,
        private val coroutineScope: CoroutineScope
    ) : ScanCallback() {
        override fun onBatchScanResults(results: List<ScanResult>) {
            super.onBatchScanResults(results)
            val newDevices = mutableListOf<BleDeviceData>()
            for (item in results) {
                item.device?.let { device ->
                    if (!scanResults.containsKey(device.address)) {
                        scanResults[device.address] = device
                        newDevices.add(
                            BleDeviceData(
                                device.address,
                                device.alias ?: device.name ?: "Unknown",
                                item.isConnectable
                            )
                        )
                    }
                }
            }
            if (newDevices.isNotEmpty()) {
                coroutineScope.launch {
                    flow.emit(ScanEvent.ScanResult(newDevices))
                }
            }
        }

        override fun onScanResult(
            callbackType: Int,
            result: ScanResult
        ) {
            super.onScanResult(callbackType, result)
            result.device?.let { device ->
                if (!scanResults.containsKey(device.address)) {
                    scanResults[device.address] = device
                    coroutineScope.launch {
                        flow.emit(
                            ScanEvent.ScanResult(
                                listOf(
                                    BleDeviceData(
                                        device.address,
                                        device.alias ?: device.name ?: "Unknown",
                                        result.isConnectable
                                    )
                                )
                            )
                        )
                    }
                }
            }
        }

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            val errorMessage = "Scan failed with error: $errorCode"
            coroutineScope.launch {
                flow.emit(ScanEvent.Error(errorMessage))
            }
        }
    }

    sealed interface ScanEvent {
        data object Scanning: ScanEvent
        data object StopScanning: ScanEvent
        data class ScanResult(val scanResults: List<BleDeviceData>): ScanEvent
        data class Error(val message: String): ScanEvent
    }
}