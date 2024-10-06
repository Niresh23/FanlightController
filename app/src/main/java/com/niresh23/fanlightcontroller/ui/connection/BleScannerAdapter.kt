package com.niresh23.fanlightcontroller.ui.connection

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.Handler
import android.os.ParcelUuid
import androidx.annotation.RequiresPermission
import com.niresh23.fanlightcontroller.utils.Constants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class BleScannerAdapter(context: Context, private val coroutineScope: CoroutineScope) {

    companion object {
        private const val SCAN_PERIOD = 20000L
    }

    private val scanResults = mutableMapOf<String, BluetoothDevice>()

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager

    private val adapter: BluetoothAdapter? = bluetoothManager.adapter
    private val _scanActionFlow = MutableSharedFlow<ScanAction>()
    val scanActionFlow = _scanActionFlow.asSharedFlow()

    private var scanner: BluetoothLeScanner? = null
    private var scanCallback: DeviceScanCallback? = null
    private lateinit var scanFilters: List<ScanFilter>
    private lateinit var scanSettings: ScanSettings

    @RequiresPermission(value = "android.permission.BLUETOOTH_SCAN")
    fun stopScan() {
        scanner?.stopScan(scanCallback)
        scanCallback = null
        coroutineScope.launch {
            _scanActionFlow.emit(ScanAction.ScanResult(scanResults))
        }
    }

    @RequiresPermission(value = "android.permission.BLUETOOTH_SCAN")
    fun startScan() {
        scanFilters = buildScanFilters()
        scanSettings = buildScanSettings()
        coroutineScope.launch {
            _scanActionFlow.emit(ScanAction.Scanning)
        }
        if (scanCallback == null) {
            scanner = adapter?.bluetoothLeScanner
            coroutineScope.launch {
                delay(SCAN_PERIOD)
                stopScan()
            }

            scanCallback = DeviceScanCallback()
            scanner?.startScan(scanFilters, scanSettings, scanCallback)
        }
    }

    private fun buildScanFilters(): List<ScanFilter> {
        val builder = ScanFilter.Builder()
        builder.setServiceUuid(ParcelUuid(Constants.SERVICE_UUID))
        val filter = builder.build()
        return listOf(filter)
    }


    private fun buildScanSettings(): ScanSettings {
        return ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
            .build()
    }

    private inner class DeviceScanCallback : ScanCallback() {
        override fun onBatchScanResults(results: List<ScanResult>) {
            super.onBatchScanResults(results)
            for (item in results) {
                item.device?.let { device ->
                    scanResults[device.address] = device
                }
            }
            coroutineScope.launch {
                _scanActionFlow.emit(ScanAction.ScanResult(scanResults))
            }
        }

        override fun onScanResult(
            callbackType: Int,
            result: ScanResult
        ) {
            super.onScanResult(callbackType, result)
            result.device?.let { device ->
                scanResults[device.address] = device
            }
            coroutineScope.launch {
                _scanActionFlow.emit(ScanAction.ScanResult(scanResults))
            }
        }

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            val errorMessage = "Scan failed with error: $errorCode"
            coroutineScope.launch {
                _scanActionFlow.emit(ScanAction.Error(errorMessage))
            }
        }
    }

    sealed interface ScanAction {
        data object Scanning: ScanAction
        data class ScanResult(val scanResults: Map<String, BluetoothDevice>): ScanAction
        data class Error(val message: String): ScanAction
    }
}