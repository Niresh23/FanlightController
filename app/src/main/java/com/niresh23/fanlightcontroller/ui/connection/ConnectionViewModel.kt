package com.niresh23.fanlightcontroller.ui.connection

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.niresh23.fanlightcontroller.ble.DeviceEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ConnectionViewModel(private val app: Application) : AndroidViewModel(app) {

    private val _viewStateFlow = MutableStateFlow(ConnectionViewState())
    val viewState = _viewStateFlow.asStateFlow()

    private val bleScannerAdapter = BleScannerAdapter(app, viewModelScope)

    init {
        viewModelScope.launch {
            bleScannerAdapter.scanEventFlow.collectLatest { scanAction ->
                when(scanAction) {
                    is BleScannerAdapter.ScanEvent.ScanResult -> {
                        _viewStateFlow.value = _viewStateFlow.value.copy(scanning = false)
                        val result = scanAction.scanResults
                        val mutableList = _viewStateFlow.value.deviceList.toMutableList()

                        result.forEach { (address, device) ->
                            _viewStateFlow.value.deviceList.firstOrNull { it.address == address } ?: mutableList.add(DeviceViewState("Exo Lighstick ver. 3", address))
                        }
                        _viewStateFlow.value = _viewStateFlow.value.copy(deviceList = mutableList)
                    }

                    is BleScannerAdapter.ScanEvent.Error -> {
                        _viewStateFlow.value = _viewStateFlow.value.copy(scanning = false, error = scanAction.message)
                    }

                    is BleScannerAdapter.ScanEvent.Scanning -> {
                        _viewStateFlow.value = _viewStateFlow.value.copy(scanning = true)
                    }

                    BleScannerAdapter.ScanEvent.StopScanning -> {
                        _viewStateFlow.value = _viewStateFlow.value.copy(scanning = false, error = null)
                    }
                }
            }
        }
    }

    fun onAction(action: ConnectionAction) {
        viewModelScope.launch {
            when(action) {
                ConnectionAction.Scan -> {
                    if (ActivityCompat.checkSelfPermission(
                            app,
                            Manifest.permission.BLUETOOTH_SCAN
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        bleScannerAdapter.startScan()
                    }
                }

                ConnectionAction.StopScan -> {
                    bleScannerAdapter.stopScan()
                }
            }
        }
    }

    fun onEvent(event: DeviceEvent) {
        when(event) {
            is DeviceEvent.Connected -> {
                val mutableList = _viewStateFlow.value.deviceList.toMutableList()
                val index = mutableList.indexOfFirst { it.address == event.address }
                mutableList[index] = mutableList[index].copy(status = DeviceConnectionStatus.CONNECTED)
                _viewStateFlow.value = _viewStateFlow.value.copy(deviceList = mutableList)
            }

            is DeviceEvent.Connecting -> {
                val mutableList = _viewStateFlow.value.deviceList.toMutableList()
                val index = mutableList.indexOfFirst { it.address == event.address }
                mutableList[index] = mutableList[index].copy(status = DeviceConnectionStatus.CONNECTING)
                _viewStateFlow.value = _viewStateFlow.value.copy(deviceList = mutableList)
            }

            is DeviceEvent.Disconnected -> {
                val mutableList = _viewStateFlow.value.deviceList.toMutableList()
                val index = mutableList.indexOfFirst { it.address == event.address }
                mutableList[index] = mutableList[index].copy(status = DeviceConnectionStatus.DISCONNECTED)
                _viewStateFlow.value = _viewStateFlow.value.copy(deviceList = mutableList)
            }

            is DeviceEvent.Disconnecting -> {
                val mutableList = _viewStateFlow.value.deviceList.toMutableList()
                val index = mutableList.indexOfFirst { it.address == event.address }
                mutableList[index] = mutableList[index].copy(status = DeviceConnectionStatus.DISCONNECTING)
                _viewStateFlow.value = _viewStateFlow.value.copy(deviceList = mutableList)
            }

            is DeviceEvent.BatteryLevel -> {
                val mutableList = _viewStateFlow.value.deviceList.toMutableList()
                val index = mutableList.indexOfFirst { it.address == event.address }
                mutableList[index] = mutableList[index].copy(batteryLevel = event.level)
                _viewStateFlow.value = _viewStateFlow.value.copy(deviceList = mutableList)
            }
        }
    }

    override fun onCleared() {
        if(ActivityCompat.checkSelfPermission(app, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
            bleScannerAdapter.stopScan()
        }
        super.onCleared()
    }
}