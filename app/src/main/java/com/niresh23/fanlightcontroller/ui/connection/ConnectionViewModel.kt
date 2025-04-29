package com.niresh23.fanlightcontroller.ui.connection

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.niresh23.fanlightcontroller.ble.DeviceEvent
import com.niresh23.fanlightcontroller.ble.IBleServiceExecutor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ConnectionViewModel(
    private val serviceExecutor: IBleServiceExecutor,
    private val app: Context
) : ViewModel() {

    private val _viewStateFlow = MutableStateFlow(ConnectionViewState())
    val viewState = _viewStateFlow.asStateFlow()

    private val bleScannerAdapter = BleScannerAdapter(app, viewModelScope)

    init {
        viewModelScope.launch {
            bleScannerAdapter.scanEventFlow.collectLatest { scanAction ->
                when(scanAction) {
                    is BleScannerAdapter.ScanEvent.ScanResult -> {
                        serviceExecutor.addDevices(scanAction.scanResults.keys)
                        _viewStateFlow.value = _viewStateFlow.value.copy(scanning = false)
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

    fun onCreate() {
        viewModelScope.launch {
            serviceExecutor.devicesViewState.collect {
                println("NRES -- devicesFromService in viewModel = $it")
                _viewStateFlow.value = _viewStateFlow.value.copy(deviceList = it)
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