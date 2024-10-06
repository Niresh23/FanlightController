package com.niresh23.fanlightcontroller.viewstate

import android.bluetooth.BluetoothDevice

sealed class DeviceScanViewState {
    data object Idle : DeviceScanViewState()
    data object ActiveScan: DeviceScanViewState()
    data class ScanResults(val scanResults: Map<String, BluetoothDevice>): DeviceScanViewState()
    data class Error(val message: String): DeviceScanViewState()
}